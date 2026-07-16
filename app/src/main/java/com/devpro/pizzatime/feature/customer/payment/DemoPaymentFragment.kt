package com.devpro.pizzatime.feature.customer.payment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentDemoPaymentBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.feature.order.PaymentStatus
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class DemoPaymentFragment : Fragment() {

    private var _binding: FragmentDemoPaymentBinding? = null
    private val binding: FragmentDemoPaymentBinding
        get() = checkNotNull(_binding) {
            "FragmentDemoPaymentBinding is only valid between onCreateView and onDestroyView."
        }

    private val backendRepository = DemoPaymentBackendRepository()
    private var orderListener: ListenerRegistration? = null
    private var createPaymentJob: Job? = null
    private var qrJob: Job? = null
    private var currentSession: DemoPaymentSession? = null
    private var currentOrder: DemoPaymentOrderSnapshot? = null
    private var hasNavigatedToSuccess = false
    private var hasRequestedInitialSession = false

    private val orderId: String
        get() = arguments?.getString(ARG_ORDER_ID).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDemoPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindStaticContent()
        setupActions()
        if (orderId.isBlank()) {
            renderState(DemoPaymentScreenState.SERVICE_UNAVAILABLE)
            return
        }
        renderState(DemoPaymentScreenState.CREATING)
        startOrderListener()
    }

    override fun onResume() {
        super.onResume()
        currentUserId()?.let { userId ->
            val pendingNavigation = DemoPaymentPendingStore.successNavigationPending(
                requireContext().applicationContext,
                userId,
            )
            if (pendingNavigation != null) {
                currentOrder = currentOrder?.takeIf { snapshot -> snapshot.orderId == pendingNavigation.orderId }
                attemptNavigateToSuccess(
                    pendingNavigation.orderId,
                    pendingNavigation.paymentAttemptId,
                )
            }
        }
    }

    private fun bindStaticContent() = with(binding) {
        tvTitle.text = getString(R.string.demo_payment_title)
        tvDisclaimer.text = getString(R.string.demo_payment_disclaimer)
        tvInstruction.text = getString(R.string.demo_payment_instruction)
        tvOrderLabel.text = getString(R.string.demo_payment_order_label)
        tvAmountLabel.text = getString(R.string.demo_payment_amount_label)
        tvReferenceLabel.text = getString(R.string.demo_payment_reference_label)
        tvExpirationLabel.text = getString(R.string.demo_payment_expiration_label)
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnOpenPaymentPage.setOnClickListener {
            openPaymentPage()
        }
        btnRefreshPaymentStatus.setOnClickListener {
            requestPaymentSession(createNewAttempt = false)
        }
        btnCreateNewPayment.setOnClickListener {
            requestPaymentSession(createNewAttempt = true)
        }
        btnBackToOrders.setOnClickListener {
            openCustomerOrderDetail(orderId)
        }
    }

    private fun startOrderListener() {
        val userId = currentUserId() ?: run {
            renderState(DemoPaymentScreenState.SERVICE_UNAVAILABLE)
            return
        }
        orderListener?.remove()
        orderListener = DemoPaymentOrderRepository.listenOrder(orderId, userId) { result ->
            if (_binding == null || !isAdded) {
                return@listenOrder
            }
            result
                .onSuccess { snapshot ->
                    currentOrder = snapshot
                    renderCurrentState(isLoadingSession = createPaymentJob?.isActive == true)
                    if (snapshot.paymentStatus == PaymentStatus.PAID) {
                        handlePaidOrder(snapshot)
                    } else if (
                        !hasRequestedInitialSession &&
                        snapshot.paymentMethod == PaymentMethod.DEMO &&
                        snapshot.paymentStatus == PaymentStatus.PENDING
                    ) {
                        hasRequestedInitialSession = true
                        requestPaymentSession(createNewAttempt = false)
                    }
                }
                .onFailure {
                    DemoPaymentPendingStore.clearActiveIfMatches(
                        requireContext().applicationContext,
                        userId,
                        orderId,
                    )
                    renderState(DemoPaymentScreenState.SERVICE_UNAVAILABLE)
                }
        }
    }

    private fun requestPaymentSession(createNewAttempt: Boolean) {
        val userId = currentUserId() ?: run {
            renderState(DemoPaymentScreenState.SERVICE_UNAVAILABLE)
            return
        }
        if (createPaymentJob?.isActive == true) {
            return
        }
        if (!createNewAttempt && currentOrder?.paymentStatus == PaymentStatus.PAID) {
            return
        }
        val appContext = requireContext().applicationContext
        val pendingState = if (createNewAttempt) {
            DemoPaymentPendingStore.createNewAttemptState(appContext, userId, orderId)
        } else {
            DemoPaymentPendingStore.ensureActiveState(appContext, userId, orderId)
        }
        currentSession = currentSession?.takeUnless { createNewAttempt }
        renderCurrentState(isLoadingSession = true)
        createPaymentJob = viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                backendRepository.createPaymentSession(
                    orderId = pendingState.orderId,
                    requestId = pendingState.requestId,
                )
            }.onSuccess { session ->
                if (_binding == null || !isAdded) {
                    return@launch
                }
                currentSession = session
                DemoPaymentPendingStore.updateSession(
                    context = appContext,
                    userId = userId,
                    orderId = orderId,
                    session = session,
                )
                renderCurrentState(isLoadingSession = false)
                generateQrCode(session.qrPayload)
            }.onFailure { error ->
                if (_binding == null || !isAdded) {
                    return@launch
                }
                currentSession = null
                renderBackendFailure(error)
            }
        }
    }

    private fun renderCurrentState(isLoadingSession: Boolean) {
        val orderSnapshot = currentOrder
        bindOrderSummary(orderSnapshot)
        when {
            orderSnapshot == null && isLoadingSession -> renderState(DemoPaymentScreenState.CREATING)
            orderSnapshot == null -> renderState(DemoPaymentScreenState.SERVICE_UNAVAILABLE)
            orderSnapshot.paymentMethod != PaymentMethod.DEMO -> renderState(DemoPaymentScreenState.UNSUPPORTED)
            orderSnapshot.paymentStatus == PaymentStatus.PAID -> renderState(DemoPaymentScreenState.CONFIRMED)
            orderSnapshot.paymentStatus == PaymentStatus.FAILED -> renderState(DemoPaymentScreenState.CANCELLED)
            orderSnapshot.paymentStatus == PaymentStatus.EXPIRED -> renderState(DemoPaymentScreenState.EXPIRED)
            orderSnapshot.paymentStatus == PaymentStatus.REFUNDED -> renderState(DemoPaymentScreenState.REFUNDED)
            isLoadingSession -> renderState(DemoPaymentScreenState.CREATING)
            else -> renderState(DemoPaymentScreenState.WAITING)
        }
    }

    private fun renderBackendFailure(error: Throwable) {
        val state = when ((error as? DemoPaymentBackendException)?.type) {
            DemoPaymentBackendErrorType.SERVICE_UNAVAILABLE -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
            DemoPaymentBackendErrorType.SESSION_EXPIRED -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
            DemoPaymentBackendErrorType.ORDER_NOT_PAYABLE -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
            DemoPaymentBackendErrorType.AMOUNT_MISMATCH -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
            null -> DemoPaymentScreenState.SERVICE_UNAVAILABLE
        }
        renderState(state)
        val messageRes = when ((error as? DemoPaymentBackendException)?.type) {
            DemoPaymentBackendErrorType.SERVICE_UNAVAILABLE -> R.string.demo_payment_service_unavailable
            DemoPaymentBackendErrorType.SESSION_EXPIRED -> R.string.demo_payment_session_expired_auth
            DemoPaymentBackendErrorType.ORDER_NOT_PAYABLE -> R.string.demo_payment_order_not_payable
            DemoPaymentBackendErrorType.AMOUNT_MISMATCH -> R.string.demo_payment_amount_mismatch
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            null,
            -> R.string.demo_payment_create_failed
        }
        showUiMessage(messageRes, UiMessageType.ERROR)
    }

    private fun bindOrderSummary(orderSnapshot: DemoPaymentOrderSnapshot?) = with(binding) {
        val appContext = requireContext().applicationContext
        val userId = currentUserId()
        val pendingState = if (userId == null) {
            null
        } else {
            DemoPaymentPendingStore.activeForUser(appContext, userId)
        }
        val orderReference = orderSnapshot?.displayOrderCode?.removePrefix("#")
            ?.ifBlank { orderId }
            ?: orderId
        val amountVnd = currentSession?.amountVnd ?: pendingState?.amountVnd
        val paymentReference = currentSession?.paymentReference
            ?: pendingState?.paymentReference
            ?: orderSnapshot?.paymentReference
            ?: getString(R.string.common_not_provided)
        val expirationLabel = currentSession?.expiresAt
            ?: pendingState?.expiresAtIso
                ?.let { value -> runCatching { parseIsoUtcDate(value) }.getOrNull() }

        tvOrderValue.text = getString(R.string.demo_payment_order_value, orderReference)
        tvAmountValue.text = amountVnd?.let(::formatVnd)
            ?: getString(R.string.common_not_provided)
        tvReferenceValue.text = paymentReference
        tvExpirationValue.text = expirationLabel?.let(::formatExpiration)
            ?: getString(R.string.common_not_provided)
    }

    private fun renderState(state: DemoPaymentScreenState) = with(binding) {
        progressSession.isVisible = state == DemoPaymentScreenState.CREATING
        btnOpenPaymentPage.isVisible = state == DemoPaymentScreenState.WAITING
        btnRefreshPaymentStatus.isVisible = state in setOf(
            DemoPaymentScreenState.WAITING,
            DemoPaymentScreenState.SERVICE_UNAVAILABLE,
            DemoPaymentScreenState.CANCELLED,
            DemoPaymentScreenState.EXPIRED,
            DemoPaymentScreenState.REFUNDED,
        )
        btnCreateNewPayment.isVisible = state in setOf(
            DemoPaymentScreenState.CANCELLED,
            DemoPaymentScreenState.EXPIRED,
        )
        btnBackToOrders.isVisible = true
        btnOpenPaymentPage.isEnabled = currentSession != null && state == DemoPaymentScreenState.WAITING
        btnRefreshPaymentStatus.isEnabled = createPaymentJob?.isActive != true
        btnCreateNewPayment.isEnabled = createPaymentJob?.isActive != true

        tvStatusValue.text = getString(
            when (state) {
                DemoPaymentScreenState.CREATING -> R.string.demo_payment_status_creating
                DemoPaymentScreenState.WAITING -> R.string.demo_payment_status_waiting
                DemoPaymentScreenState.CONFIRMED -> R.string.demo_payment_status_confirmed
                DemoPaymentScreenState.CANCELLED -> R.string.demo_payment_status_cancelled
                DemoPaymentScreenState.EXPIRED -> R.string.demo_payment_status_expired
                DemoPaymentScreenState.SERVICE_UNAVAILABLE -> R.string.demo_payment_status_unavailable
                DemoPaymentScreenState.REFUNDED -> R.string.demo_payment_status_refunded
                DemoPaymentScreenState.UNSUPPORTED -> R.string.demo_payment_status_unsupported
            },
        )
        tvStatusMessage.text = getString(
            when (state) {
                DemoPaymentScreenState.CREATING -> R.string.demo_payment_message_creating
                DemoPaymentScreenState.WAITING -> R.string.demo_payment_message_waiting
                DemoPaymentScreenState.CONFIRMED -> R.string.demo_payment_message_confirmed
                DemoPaymentScreenState.CANCELLED -> R.string.demo_payment_message_cancelled
                DemoPaymentScreenState.EXPIRED -> R.string.demo_payment_message_expired
                DemoPaymentScreenState.SERVICE_UNAVAILABLE -> R.string.demo_payment_message_unavailable
                DemoPaymentScreenState.REFUNDED -> R.string.demo_payment_message_refunded
                DemoPaymentScreenState.UNSUPPORTED -> R.string.demo_payment_message_unsupported
            },
        )
        if (state != DemoPaymentScreenState.WAITING) {
            clearQrCode()
        }
    }

    private fun handlePaidOrder(orderSnapshot: DemoPaymentOrderSnapshot) {
        val userId = currentUserId() ?: return
        val paymentAttemptId = orderSnapshot.paymentAttemptId ?: return
        val appContext = requireContext().applicationContext
        val successPendingCreated = DemoPaymentPendingStore.markSuccessNavigationPending(
            context = appContext,
            userId = userId,
            orderId = orderSnapshot.orderId,
            paymentAttemptId = paymentAttemptId,
        )
        if (successPendingCreated) {
            CartStore.clear()
        }
        attemptNavigateToSuccess(orderSnapshot.orderId, paymentAttemptId)
    }

    private fun attemptNavigateToSuccess(
        orderId: String,
        paymentAttemptId: String,
    ) {
        if (hasNavigatedToSuccess || _binding == null) {
            return
        }
        val userId = currentUserId() ?: return
        if (parentFragmentManager.isStateSaved || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        hasNavigatedToSuccess = true
        DemoPaymentPendingStore.clearSuccessNavigation(
            context = requireContext().applicationContext,
            userId = userId,
            orderId = orderId,
            paymentAttemptId = paymentAttemptId,
        )
        openOrderSuccess(orderId = orderId, addToBackStack = false)
    }

    private fun openPaymentPage() {
        val paymentUri = currentSession?.paymentPageUri ?: run {
            showUiMessage(R.string.demo_payment_create_failed, UiMessageType.ERROR)
            return
        }
        val customTabsIntent = CustomTabsIntent.Builder().build()
        runCatching {
            customTabsIntent.launchUrl(requireContext(), paymentUri)
        }.recoverCatching {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, paymentUri)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (fallbackIntent.resolveActivity(requireContext().packageManager) == null) {
                throw ActivityNotFoundException("No browser available")
            }
            startActivity(fallbackIntent)
        }.onFailure {
            showUiMessage(R.string.demo_payment_no_browser, UiMessageType.ERROR)
        }
    }

    private fun generateQrCode(qrPayload: String) {
        clearQrCode()
        qrJob = viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = runCatching {
                withContext(Dispatchers.Default) {
                    buildQrBitmap(qrPayload, QR_SIZE_PX)
                }
            }.getOrNull()
            if (_binding == null || !isAdded) {
                return@launch
            }
            if (bitmap == null) {
                binding.tvQrUnavailable.isVisible = true
                binding.ivQrCode.isVisible = false
                showUiMessage(R.string.demo_payment_qr_unavailable, UiMessageType.WARNING)
                return@launch
            }
            binding.ivQrCode.setImageBitmap(bitmap)
            binding.ivQrCode.isVisible = true
            binding.tvQrUnavailable.isVisible = false
        }
    }

    private fun clearQrCode() = with(binding) {
        qrJob?.cancel()
        ivQrCode.setImageDrawable(null)
        ivQrCode.isVisible = false
        tvQrUnavailable.isVisible = false
    }

    private fun currentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.trim()
            ?.takeIf { userId -> userId.isNotBlank() }
    }

    private fun formatVnd(amountVnd: Int): String {
        return getString(
            R.string.demo_payment_amount_value,
            NumberFormat.getIntegerInstance(Locale.US).format(amountVnd),
        )
    }

    private fun formatExpiration(expiresAt: Date): String {
        val remainingMillis = expiresAt.time - System.currentTimeMillis()
        val remaining = (remainingMillis / 60_000L).coerceAtLeast(0L)
        return getString(R.string.demo_payment_expiration_value, "${remaining}m")
    }

    override fun onDestroyView() {
        createPaymentJob?.cancel()
        qrJob?.cancel()
        orderListener?.remove()
        orderListener = null
        _binding = null
        super.onDestroyView()
    }

    private enum class DemoPaymentScreenState {
        CREATING,
        WAITING,
        CONFIRMED,
        CANCELLED,
        EXPIRED,
        SERVICE_UNAVAILABLE,
        REFUNDED,
        UNSUPPORTED,
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"
        private const val QR_SIZE_PX = 720

        fun newInstance(orderId: String): DemoPaymentFragment {
            return DemoPaymentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}

private fun buildQrBitmap(
    value: String,
    sizePx: Int,
): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(
        value,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        hints,
    )
    return matrix.toBitmap()
}

private fun BitMatrix.toBitmap(): Bitmap {
    val width = width
    val height = height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (get(x, y)) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
