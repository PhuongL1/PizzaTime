package com.devpro.pizzatime.feature.customer.account

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.core.ui.message.UiMessage
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.UiText
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentCustomerAccountBinding
import com.devpro.pizzatime.feature.admin.menu.CloudinaryProductImageRepository
import com.devpro.pizzatime.feature.admin.store.StoreSettingsRepository
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import com.devpro.pizzatime.feature.customer.orderhistory.CustomerOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.openCustomerFavorites
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openForgotPassword
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking
import com.devpro.pizzatime.feature.staff.navigation.openStoreSettings
import com.devpro.pizzatime.feature.staff.navigation.signOutAndOpenLogin
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

class CustomerAccountFragment : Fragment() {

    private var _binding: FragmentCustomerAccountBinding? = null
    private val binding: FragmentCustomerAccountBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerAccountBinding is only valid between onCreateView and onDestroyView."
        }

    private var accountData: CustomerAccountUiModel = FakeCustomerAccountData.getCustomerAccount()
    private var currentRole: UserRole = UserRole.GUEST
    private var changePasswordRow: LinearLayout? = null
    private var isUploadingAvatar = false
    private var isLoadingOrderRoute = false

    private val pickAvatarImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
            if (imageUri != null) {
                uploadAvatar(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentRole = if (FirebaseAuth.getInstance().currentUser == null) {
            FakeSessionStore.logout()
            UserRole.GUEST
        } else {
            FakeSessionStore.currentRole
        }
        bindAccount()
        setupTopBar()
        setupBottomNav()
        setupActions()
        loadCustomerProfile()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
        }
    }

    private fun bindAccount() = with(binding) {
        bindAvatar()
        tvCustomerName.text = accountData.fullName
        tvTierName.text = if (currentRole == UserRole.CUSTOMER) {
            accountData.tierName
        } else {
            currentRole.name
        }
        tvDoughPoints.text = getString(
            R.string.customer_account_dough_points,
            formatNumber(accountData.doughPoints),
        )
        tvEmailValue.text = accountData.email.ifBlank { getString(R.string.common_not_provided) }
        tvPhoneValue.text = accountData.phone.ifBlank { getString(R.string.common_not_provided) }
    }

    private fun setupTopBar() = with(binding) {
        customerTopBar.root.visibility = if (currentRole == UserRole.CUSTOMER) View.VISIBLE else View.GONE
        if (currentRole == UserRole.CUSTOMER) {
            bindCustomerTopBar(
                root = customerTopBar.root,
                cartItemCount = CartStore.items.sumOf { it.quantity },
            )
        }
    }

    private fun setupBottomNav() = with(binding) {
        bindRoleAwareAccountBottomNav(
            root = customerBottomNav.root,
            role = currentRole,
        )
    }

    private fun setupActions() = with(binding) {
        avatarArea.setOnClickListener(null)
        editAvatarButton.setOnClickListener { showEditProfileDialog() }
        tvChangePhoto.setOnClickListener { startAvatarPicker() }

        rowOrderHistory.setOnClickListener {
            if (currentRole == UserRole.CUSTOMER) {
                openCustomerOrders()
            } else {
                showStoreInfoDialog()
            }
        }

        rowPaymentMethods.setOnClickListener {
            if (currentRole == UserRole.CUSTOMER) {
                showAccountMessage(getString(R.string.customer_account_payment_methods_toast))
            } else {
                showAccountInfoDialog()
            }
        }

        rowDeliveryAddresses.setOnClickListener {
            if (currentRole == UserRole.CUSTOMER) {
                showEditProfileDialog()
            } else {
                showChangePasswordDialog()
            }
        }

        rowFavorites.setOnClickListener {
            openCustomerFavorites()
        }

        rowSettings.setOnClickListener {
            showAccountInfoDialog()
        }

        logoutCard.setOnClickListener {
            showLogoutConfirmation()
        }

        configureMenuForRole()
        applyAuthenticatedRowVisibility()
    }

    private fun openCustomerOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank() || isLoadingOrderRoute) return

        isLoadingOrderRoute = true
        binding.rowOrderHistory.isEnabled = false
        CustomerOrderFirestoreRepository.loadOrderHistory(uid) { result ->
            if (_binding == null || !isAdded) return@loadOrderHistory
            isLoadingOrderRoute = false
            binding.rowOrderHistory.isEnabled = true

            val orders = result.getOrElse { error ->
                Log.e(TAG, "Failed to load customer order route", error)
                showAccountMessage(getString(R.string.customer_order_detail_load_failed), UiMessageType.ERROR)
                return@loadOrderHistory
            }

            when (orders.size) {
                0 -> openCustomerOrderHistory()
                1 -> openOrderTracking(orders.first().orderId)
                else -> openCustomerOrderHistory()
            }
        }
    }

    private fun bindAvatar() = with(binding) {
        tvAvatarInitials.text = buildAvatarInitials(accountData.fullName)
        val avatarUrl = accountData.avatarUrl.trim()
        if (avatarUrl.isBlank()) {
            Glide.with(ivAvatar).clear(ivAvatar)
            ivAvatar.setImageDrawable(null)
            ivAvatar.isVisible = false
            tvAvatarInitials.isVisible = true
            return@with
        }

        ivAvatar.isVisible = true
        tvAvatarInitials.isVisible = false
        Glide.with(ivAvatar)
            .load(avatarUrl)
            .placeholder(accountData.avatarRes)
            .error(accountData.avatarRes)
            .fallback(accountData.avatarRes)
            .centerCrop()
            .into(ivAvatar)
    }

    private fun startAvatarPicker() {
        if (isUploadingAvatar) return
        if (FirebaseAuth.getInstance().currentUser == null) {
            showAccountMessage(getString(R.string.customer_favorites_login_required), UiMessageType.WARNING)
            return
        }
        pickAvatarImage.launch("image/*")
    }

    private fun loadCustomerProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            showAccountMessage(getString(R.string.customer_account_login_required_toast), UiMessageType.WARNING)
            openLoginScreen(addToBackStack = false)
            return
        }

        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null || !isAdded) return@loadProfile
            result
                .onSuccess { profile ->
                    currentRole = profile.role
                    if (currentRole != UserRole.GUEST) {
                        FakeSessionStore.login(currentRole)
                    }
                    accountData = profile.copy(
                        email = profile.email.ifBlank { user.email.orEmpty() },
                    )
                    bindAccount()
                    setupTopBar()
                    setupBottomNav()
                    configureMenuForRole()
                    applyAuthenticatedRowVisibility()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load customer profile", error)
                    showAccountMessage(getString(R.string.customer_account_profile_load_failed), UiMessageType.ERROR)
                }
        }
    }

    private fun uploadAvatar(imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            showAccountMessage(getString(R.string.customer_favorites_login_required), UiMessageType.WARNING)
            return
        }

        setAvatarUploading(true)
        CloudinaryProductImageRepository.uploadAvatarImage(
            context = requireContext(),
            imageUri = imageUri,
        ) { uploadResult ->
            if (_binding == null) return@uploadAvatarImage
            uploadResult
                .onSuccess { avatarUrl ->
                    CustomerProfileFirestoreRepository.updateAvatarUrl(uid, avatarUrl) { saveResult ->
                        if (_binding == null) return@updateAvatarUrl
                        setAvatarUploading(false)
                        saveResult
                            .onSuccess {
                                accountData = accountData.copy(avatarUrl = avatarUrl)
                                bindAccount()
                                showAccountMessage(getString(R.string.customer_account_photo_updated), UiMessageType.SUCCESS)
                            }
                            .onFailure { error ->
                                Log.e(TAG, "Failed to save customer avatar", error)
                                showAccountMessage(
                                    getString(R.string.customer_account_photo_update_failed),
                                    UiMessageType.ERROR,
                                )
                            }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to upload customer avatar", error)
                    setAvatarUploading(false)
                    showAccountMessage(getString(R.string.customer_account_photo_update_failed), UiMessageType.ERROR)
                }
        }
    }

    private fun setAvatarUploading(uploading: Boolean) = with(binding) {
        isUploadingAvatar = uploading
        avatarUploadProgress.isVisible = uploading
        avatarArea.isEnabled = !uploading
        editAvatarButton.isEnabled = !uploading
        tvChangePhoto.isEnabled = !uploading
        editAvatarButton.alpha = if (uploading) DISABLED_ALPHA else ENABLED_ALPHA
        tvChangePhoto.alpha = if (uploading) DISABLED_ALPHA else ENABLED_ALPHA
    }

    private fun showEditProfileDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            showAccountMessage(getString(R.string.customer_account_login_required_toast), UiMessageType.WARNING)
            openLoginScreen(addToBackStack = false)
            return
        }

        val includeDeliveryAddress = currentRole == UserRole.CUSTOMER
        val nameInput = createDialogInput(
            hint = getString(R.string.customer_account_edit_name_hint),
            text = accountData.fullName,
        )
        val phoneInput = createDialogInput(
            hint = getString(R.string.customer_account_edit_phone_hint),
            text = accountData.phone,
            inputType = InputType.TYPE_CLASS_PHONE,
        )
        val addressInput = createDialogInput(
            hint = getString(R.string.customer_account_edit_address_hint),
            text = accountData.deliveryAddress,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.customer_account_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(nameInput)
            addView(phoneInput)
            if (includeDeliveryAddress) {
                addView(addressInput)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.customer_account_edit_profile_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.customer_account_edit_profile_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = nameInput.text.toString().trim()
                        if (name.isBlank()) {
                            nameInput.error = getString(R.string.customer_account_name_required_toast)
                            return@setOnClickListener
                        }
                        nameInput.error = null
                        saveProfile(
                            uid = uid,
                            name = name,
                            phone = phoneInput.text.toString().trim(),
                            deliveryAddress = if (includeDeliveryAddress) {
                                addressInput.text.toString().trim()
                            } else {
                                accountData.deliveryAddress
                            },
                            onSaved = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun createDialogInput(
        hint: String,
        text: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
    ): EditText {
        return EditText(requireContext()).apply {
            this.hint = hint
            this.inputType = inputType
            setText(text)
            setSelectAllOnFocus(false)
        }
    }

    private fun saveProfile(
        uid: String,
        name: String,
        phone: String,
        deliveryAddress: String,
        onSaved: () -> Unit,
    ) {
        CustomerProfileFirestoreRepository.updateProfile(
            uid = uid,
            name = name,
            phone = phone,
            deliveryAddress = deliveryAddress,
        ) { result ->
            if (_binding == null || !isAdded) return@updateProfile
            result
                .onSuccess {
                    accountData = accountData.copy(
                        fullName = name,
                        phone = phone,
                        deliveryAddress = deliveryAddress,
                    )
                    bindAccount()
                    showAccountMessage(getString(R.string.customer_account_profile_saved_toast), UiMessageType.SUCCESS)
                    onSaved()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to save customer profile", error)
                    showAccountMessage(getString(R.string.customer_account_profile_save_failed), UiMessageType.ERROR)
                }
        }
    }

    private fun configureMenuForRole() = with(binding) {
        val isCustomer = currentRole == UserRole.CUSTOMER
        setMenuRowTitle(
            rowOrderHistory,
            if (isCustomer) R.string.customer_account_order_history else R.string.account_store_info,
        )
        setMenuRowTitle(
            rowPaymentMethods,
            if (isCustomer) R.string.customer_account_payment_methods else R.string.account_account_info,
        )
        setMenuRowTitle(
            rowDeliveryAddresses,
            if (isCustomer) R.string.customer_account_delivery_addresses else R.string.account_change_password,
        )
        setMenuRowTitle(rowSettings, R.string.account_account_info)

        rowFavorites.visibility = if (isCustomer) View.VISIBLE else View.GONE
        rowSettings.visibility = if (isCustomer) View.VISIBLE else View.GONE
        pointsRow.visibility = if (isCustomer) View.VISIBLE else View.GONE

        if (isCustomer) {
            ensureCustomerChangePasswordRow()
        } else {
            changePasswordRow?.visibility = View.GONE
        }
        applyAuthenticatedRowVisibility()
    }

    private fun applyAuthenticatedRowVisibility() = with(binding) {
        val isAuthenticated = FirebaseAuth.getInstance().currentUser != null
        val authenticatedRows = listOf(rowOrderHistory, rowFavorites, logoutCard)
        authenticatedRows.forEach { row ->
            if (isAuthenticated) {
                row.isClickable = true
                row.isFocusable = true
                row.isEnabled = true
            } else {
                row.visibility = View.GONE
                row.isClickable = false
                row.isFocusable = false
                row.isEnabled = false
                row.setOnClickListener(null)
            }
        }
    }

    private fun ensureCustomerChangePasswordRow() {
        if (changePasswordRow == null) {
            changePasswordRow = createMenuRow(getString(R.string.account_change_password)).also { row ->
                row.setOnClickListener { showChangePasswordDialog() }
                binding.menuContainer.addView(row, binding.menuContainer.childCount)
            }
        }
        changePasswordRow?.visibility = View.VISIBLE
    }

    private fun createMenuRow(title: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                MENU_ROW_HEIGHT_DP.dp(),
            ).apply {
                bottomMargin = MENU_ROW_SPACING_DP.dp()
            }
            setBackgroundResource(R.drawable.bg_customer_account_menu_card)
            isClickable = true
            isFocusable = true
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(MENU_ROW_HORIZONTAL_PADDING_DP.dp(), 0, MENU_ROW_HORIZONTAL_PADDING_DP.dp(), 0)
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(MENU_ICON_SIZE_DP.dp(), MENU_ICON_SIZE_DP.dp())
                setBackgroundResource(R.drawable.bg_customer_account_icon_circle)
            })
            addView(TextView(context).apply {
                text = title
                setTextColor(requireContext().getColor(R.color.pt_text_primary_dark_bg))
                textSize = MENU_TEXT_SIZE_SP
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart = MENU_TEXT_MARGIN_START_DP.dp()
                }
            })
        }
    }

    private fun setMenuRowTitle(row: LinearLayout, titleRes: Int) {
        row.findDirectChildTextView()?.setText(titleRes)
    }

    private fun LinearLayout.findDirectChildTextView(): TextView? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is TextView) {
                return child
            }
        }
        return null
    }

    private fun showStoreInfoDialog() {
        StoreSettingsRepository.loadStoreSettings { result ->
            if (_binding == null || !isAdded) return@loadStoreSettings
            val message = result.fold(
                onSuccess = { settings -> buildStoreInfoMessage(settings) },
                onFailure = { getString(R.string.account_store_info_not_configured) },
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_store_info)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, null)
                .apply {
                    if (currentRole == UserRole.ADMIN) {
                        setPositiveButton(R.string.account_edit_store_settings) { _, _ ->
                            openStoreSettings()
                        }
                    }
                }
                .show()
        }
    }

    private fun buildStoreInfoMessage(settings: StoreSettingsUiModel): String {
        if (settings.pickupAddress.isBlank() && settings.storePhone.isBlank()) {
            return getString(R.string.account_store_info_not_configured)
        }

        return listOf(
            getString(
                R.string.account_store_name_format,
                settings.storeName.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(
                R.string.account_pickup_address_format,
                settings.pickupAddress.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(
                R.string.account_opening_hours_format,
                settings.openingHours.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(R.string.account_base_delivery_fee_format, formatCurrency(settings.baseDeliveryFee)),
            getString(R.string.account_fee_per_km_format, formatCurrency(settings.deliveryFeePerKm)),
            getString(R.string.account_free_delivery_min_format, formatCurrency(settings.freeDeliveryMinSubtotal)),
            getString(
                R.string.account_accepting_orders_format,
                if (settings.acceptingOrders) {
                    getString(R.string.account_accepting_orders_yes)
                } else {
                    getString(R.string.account_accepting_orders_no)
                },
            ),
        ).joinToString(separator = "\n")
    }

    private fun showAccountInfoDialog() {
        val roleLabel = currentRole.name.lowercase(Locale.US)
            .replaceFirstChar { char -> char.titlecase(Locale.US) }
        val message = listOf(
            getString(
                R.string.account_full_name_format,
                accountData.fullName.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(
                R.string.account_email_format,
                accountData.email.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(
                R.string.account_phone_format,
                accountData.phone.ifBlank { getString(R.string.common_not_provided) },
            ),
            getString(R.string.account_role_format, roleLabel),
            getString(
                R.string.account_active_format,
                if (accountData.active) {
                    getString(R.string.account_active_yes)
                } else {
                    getString(R.string.account_active_no)
                },
            ),
        ).joinToString(separator = "\n")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_account_info)
            .setMessage(message)
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val oldPasswordInput = createPasswordInput(R.string.account_old_password)
        val newPasswordInput = createPasswordInput(R.string.account_new_password)
        val confirmPasswordInput = createPasswordInput(R.string.account_confirm_new_password)
        val forgotPasswordText = TextView(requireContext()).apply {
            setText(R.string.account_forgot_password)
            setTextColor(requireContext().getColor(R.color.pt_copper))
            textSize = FORGOT_PASSWORD_TEXT_SIZE_SP
            setPadding(0, FORGOT_PASSWORD_TOP_PADDING_DP.dp(), 0, 0)
        }

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.customer_account_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(oldPasswordInput)
            addView(newPasswordInput)
            addView(confirmPasswordInput)
            addView(forgotPasswordText)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_change_password)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.account_change_password, null)
            .create()

        dialog.setOnShowListener {
            forgotPasswordText.setOnClickListener {
                dialog.dismiss()
                openForgotPassword()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val oldPassword = oldPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()
                val valid = validatePasswordInputs(
                    oldPasswordInput = oldPasswordInput,
                    newPasswordInput = newPasswordInput,
                    confirmPasswordInput = confirmPasswordInput,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                )
                if (!valid) return@setOnClickListener
                changePassword(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    onChanged = { dialog.dismiss() },
                )
            }
        }
        dialog.show()
    }

    private fun createPasswordInput(hintRes: Int): EditText {
        return createDialogInput(
            hint = getString(hintRes),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
    }

    private fun validatePasswordInputs(
        oldPasswordInput: EditText,
        newPasswordInput: EditText,
        confirmPasswordInput: EditText,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): Boolean {
        oldPasswordInput.error = if (oldPassword.isBlank()) {
            getString(R.string.account_password_fields_required)
        } else {
            null
        }
        newPasswordInput.error = when {
            newPassword.isBlank() -> getString(R.string.account_password_fields_required)
            newPassword.length < MIN_PASSWORD_LENGTH -> getString(R.string.account_password_too_short)
            else -> null
        }
        confirmPasswordInput.error = if (confirmPassword != newPassword) {
            getString(R.string.account_passwords_do_not_match)
        } else {
            null
        }
        return oldPasswordInput.error == null &&
            newPasswordInput.error == null &&
            confirmPasswordInput.error == null
    }

    private fun changePassword(
        oldPassword: String,
        newPassword: String,
        onChanged: () -> Unit,
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email.orEmpty()
        when {
            user == null || email.isBlank() -> {
                showAccountMessage(getString(R.string.account_login_required), UiMessageType.WARNING)
                return
            }
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception(getString(R.string.account_password_change_failed))
                }
                user.updatePassword(newPassword)
            }
            .addOnSuccessListener {
                if (_binding == null || !isAdded) return@addOnSuccessListener
                showAccountMessage(getString(R.string.account_password_changed), UiMessageType.SUCCESS)
                onChanged()
            }
            .addOnFailureListener { error ->
                if (_binding == null || !isAdded) return@addOnFailureListener
                Log.e(TAG, "Failed to change account password", error)
                showAccountMessage(getString(R.string.account_password_change_failed), UiMessageType.ERROR)
            }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value)
    }

    private fun buildAvatarInitials(name: String): String {
        val parts = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) return DEFAULT_AVATAR_INITIALS
        return parts
            .take(2)
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString(separator = "")
            .ifBlank { DEFAULT_AVATAR_INITIALS }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.customer_menu_title_logout)
            .setMessage(R.string.customer_account_logout_confirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.customer_menu_title_logout) { _, _ ->
                signOutAndOpenLogin()
            }
            .show()
    }

    private fun showAccountMessage(
        message: String,
        type: UiMessageType = UiMessageType.INFO,
    ) {
        val text = UiText.Dynamic.from(message) ?: return
        showUiMessage(UiMessage(text = text, type = type))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "CustomerAccount"
        const val DEFAULT_AVATAR_INITIALS = "PT"
        const val ENABLED_ALPHA = 1f
        const val DISABLED_ALPHA = 0.45f
        const val MIN_PASSWORD_LENGTH = 6
        const val MENU_ROW_HEIGHT_DP = 64
        const val MENU_ROW_SPACING_DP = 8
        const val MENU_ROW_HORIZONTAL_PADDING_DP = 16
        const val MENU_ICON_SIZE_DP = 42
        const val MENU_TEXT_MARGIN_START_DP = 18
        const val MENU_TEXT_SIZE_SP = 20f
        const val FORGOT_PASSWORD_TEXT_SIZE_SP = 14f
        const val FORGOT_PASSWORD_TOP_PADDING_DP = 12
    }
}
