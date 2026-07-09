package com.devpro.pizzatime.feature.admin.promo

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentManagePromoCodesBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.admin.navigation.bindAdminTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import java.util.Locale

class ManagePromoCodesFragment : Fragment() {

    private var _binding: FragmentManagePromoCodesBinding? = null
    private val binding: FragmentManagePromoCodesBinding
        get() = checkNotNull(_binding) {
            "FragmentManagePromoCodesBinding is only valid between onCreateView and onDestroyView."
        }

    private var allPromos: List<AdminPromoUiModel> = emptyList()

    private val promoAdapter by lazy {
        AdminPromoAdapter(
            onEditClick = { promo -> showEditPromoDialog(promo) },
            onDeleteClick = { promo -> confirmDeletePromo(promo) },
            onShareClick = { promo -> sharePromo(promo) },
            onReactivateClick = { promo ->
                AdminPromoFirestoreRepository.setActive(promo.id, true) { result ->
                    if (!isAdded) return@setActive
                    if (result.isSuccess) loadFirestorePromos()
                }
            },
        )
    }

    private var selectedFilter = PromoFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagePromoCodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupTopBar()
        setupActions()
        setupFilters()
        setupBottomNav()
        renderPromos()
        loadFirestorePromos()
    }

    private fun loadFirestorePromos() {
        AdminPromoFirestoreRepository.loadPromos { result ->
            if (!isAdded) return@loadPromos
            result
                .onSuccess { promos ->
                    allPromos = promos
                    Log.d(TAG, "admin loaded count=${promos.size}")
                }
                .onFailure { error ->
                    allPromos = emptyList()
                    Log.e(TAG, "Admin promo load failed", error)
                    showToast(R.string.promo_load_failed)
                }
            renderPromos()
        }
    }

    private fun showCreatePromoDialog() {
        val codeInput = createDialogInput(
            hint = getString(R.string.promo_create_code_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )
        val titleInput = createDialogInput(
            hint = getString(R.string.promo_edit_title_hint),
            text = "",
        )
        val descriptionInput = createDialogInput(
            hint = getString(R.string.promo_edit_description_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )
        val discountTypeInput = createDialogInput(
            hint = getString(R.string.promo_edit_discount_type_hint),
            text = "PERCENT",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )
        val discountValueInput = createDialogInput(
            hint = getString(R.string.promo_edit_discount_value_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )
        val minOrderInput = createDialogInput(
            hint = getString(R.string.promo_edit_min_order_hint),
            text = "0.00",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.promo_edit_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(codeInput)
            addView(titleInput)
            addView(descriptionInput)
            addView(discountTypeInput)
            addView(discountValueInput)
            addView(minOrderInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.promo_create_dialog_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.promo_create_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        savePromoCreate(
                            rawCode = codeInput.text.toString(),
                            title = titleInput.text.toString().trim(),
                            description = descriptionInput.text.toString().trim(),
                            discountType = discountTypeInput.text.toString().trim().uppercase(Locale.US),
                            discountValueText = discountValueInput.text.toString().trim(),
                            minOrderAmountText = minOrderInput.text.toString().trim(),
                            onSaved = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun showEditPromoDialog(promo: AdminPromoUiModel) {
        val titleInput = createDialogInput(
            hint = getString(R.string.promo_edit_title_hint),
            text = promo.title,
        )
        val descriptionInput = createDialogInput(
            hint = getString(R.string.promo_edit_description_hint),
            text = promo.description,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )
        val discountTypeInput = createDialogInput(
            hint = getString(R.string.promo_edit_discount_type_hint),
            text = promo.discountType.uppercase(Locale.US),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )
        val discountValueInput = createDialogInput(
            hint = getString(R.string.promo_edit_discount_value_hint),
            text = String.format(Locale.US, "%.2f", promo.discountValue),
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )
        val minOrderInput = createDialogInput(
            hint = getString(R.string.promo_edit_min_order_hint),
            text = String.format(Locale.US, "%.2f", promo.minOrderAmount),
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )
        val activeInput = CheckBox(requireContext()).apply {
            text = getString(R.string.promo_edit_active)
            isChecked = promo.status == AdminPromoStatus.ACTIVE
        }

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.promo_edit_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(titleInput)
            addView(descriptionInput)
            addView(discountTypeInput)
            addView(discountValueInput)
            addView(minOrderInput)
            addView(activeInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.promo_edit_dialog_title, promo.code))
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.promo_edit_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        savePromoEdit(
                            promoId = promo.id,
                            title = titleInput.text.toString().trim(),
                            description = descriptionInput.text.toString().trim(),
                            discountType = discountTypeInput.text.toString().trim().uppercase(Locale.US),
                            discountValueText = discountValueInput.text.toString().trim(),
                            minOrderAmountText = minOrderInput.text.toString().trim(),
                            active = activeInput.isChecked,
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

    private fun savePromoEdit(
        promoId: String,
        title: String,
        description: String,
        discountType: String,
        discountValueText: String,
        minOrderAmountText: String,
        active: Boolean,
        onSaved: () -> Unit,
    ) {
        val discountValue = discountValueText.toDoubleOrNull()
        val minOrderAmount = minOrderAmountText.toDoubleOrNull()

        when {
            title.isBlank() -> {
                showToast(R.string.promo_edit_title_required)
                return
            }

            discountType != "PERCENT" && discountType != "FIXED" -> {
                showToast(R.string.promo_edit_discount_type_invalid)
                return
            }

            discountValue == null || discountValue <= 0.0 -> {
                showToast(R.string.promo_edit_discount_value_required)
                return
            }

            minOrderAmount == null || minOrderAmount < 0.0 -> {
                showToast(R.string.promo_edit_min_order_invalid)
                return
            }
        }

        AdminPromoFirestoreRepository.updatePromo(
            promoId = promoId,
            title = title,
            description = description,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            active = active,
        ) { result ->
            if (!isAdded) return@updatePromo
            result
                .onSuccess {
                    showToast(R.string.promo_edit_saved)
                    onSaved()
                    loadFirestorePromos()
                }
                .onFailure {
                    showToast(R.string.promo_edit_failed)
                }
        }
    }

    private fun savePromoCreate(
        rawCode: String,
        title: String,
        description: String,
        discountType: String,
        discountValueText: String,
        minOrderAmountText: String,
        onSaved: () -> Unit,
    ) {
        val code = rawCode.trim().uppercase(Locale.US)
        val discountValue = discountValueText.toDoubleOrNull()
        val minOrderAmount = minOrderAmountText.toDoubleOrNull()

        when {
            code.isBlank() -> {
                showToast(R.string.promo_create_code_required)
                return
            }

            title.isBlank() -> {
                showToast(R.string.promo_edit_title_required)
                return
            }

            discountType != "PERCENT" && discountType != "FIXED" -> {
                showToast(R.string.promo_edit_discount_type_invalid)
                return
            }

            discountValue == null || discountValue <= 0.0 -> {
                showToast(R.string.promo_edit_discount_value_required)
                return
            }

            minOrderAmount == null || minOrderAmount < 0.0 -> {
                showToast(R.string.promo_edit_min_order_invalid)
                return
            }
        }

        AdminPromoFirestoreRepository.createPromo(
            code = code,
            title = title,
            description = description,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
        ) { result ->
            if (!isAdded) return@createPromo
            result
                .onSuccess {
                    showToast(R.string.promo_create_saved)
                    onSaved()
                    loadFirestorePromos()
                }
                .onFailure {
                    showToast(R.string.promo_create_failed)
                }
        }
    }

    private fun setupRecyclerView() = with(binding.rvPromos) {
        layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean = false
        }
        adapter = promoAdapter
        setHasFixedSize(false)
        itemAnimator = null
        isNestedScrollingEnabled = false
    }

    private fun setupActions() = with(binding) {
        btnAddPromo.setOnClickListener {
            showCreatePromoDialog()
        }
    }

    private fun setupFilters() = with(binding) {
        tvChipActive.setOnClickListener {
            selectedFilter = PromoFilter.ACTIVE
            renderPromos()
        }

        tvChipInactive.setOnClickListener {
            selectedFilter = PromoFilter.PAST
            renderPromos()
        }

        tvChipScheduled.setOnClickListener {
            selectedFilter = PromoFilter.ALL
            renderPromos()
        }
    }

    private fun setupBottomNav() {
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.PROMOS,
            onDashboardClick = { openAdminDashboard() },
            onManageMenuClick = { openManageMenu() },
            onManagePromoCodesClick = { openShipperDeliveryDashboard() },
            onManageStaffClick = { openCustomerAccount() },
        )
    }

    private fun setupTopBar() {
        bindAdminTopBar(
            root = binding.staffTopBar.root,
            title = getString(R.string.manage_codes),
        )
    }

    private fun renderPromos() {
        val promos = when (selectedFilter) {
            PromoFilter.ACTIVE -> allPromos.filter {
                it.status == AdminPromoStatus.ACTIVE
            }

            PromoFilter.PAST -> allPromos.filter {
                it.status == AdminPromoStatus.INACTIVE || it.status == AdminPromoStatus.EXPIRED
            }

            PromoFilter.ALL -> allPromos
        }

        Log.d(TAG, "admin filtered tab=$selectedFilter count=${promos.size}")
        promoAdapter.submitList(promos.toList()) {
            binding.rvPromos.post {
                binding.rvPromos.requestLayout()
            }
        }
        binding.rvPromos.isVisible = promos.isNotEmpty()
        binding.tvEmptyPromos.isVisible = promos.isEmpty()
        renderFilterState()
        renderStats()
    }

    private fun renderFilterState() = with(binding) {
        tvChipActive.text = getString(
            R.string.promo_filter_active_count,
            allPromos.count { promo -> promo.status == AdminPromoStatus.ACTIVE },
        )
        tvChipInactive.text = getString(
            R.string.promo_filter_past_count,
            allPromos.count { promo ->
                promo.status == AdminPromoStatus.INACTIVE || promo.status == AdminPromoStatus.EXPIRED
            },
        )
        tvChipScheduled.text = getString(R.string.promo_filter_all_count, allPromos.size)
        tvChipActive.bindChip(selectedFilter == PromoFilter.ACTIVE)
        tvChipInactive.bindChip(selectedFilter == PromoFilter.PAST)
        tvChipScheduled.bindChip(selectedFilter == PromoFilter.ALL)
    }

    private fun renderStats() = with(binding) {
        val totalUsage = allPromos.sumOf { promo -> promo.usageCount }
        val totalMaxUses = allPromos.sumOf { promo -> promo.maxUses ?: 0 }
        redemptionRate.text = if (totalMaxUses > 0) {
            String.format(Locale.US, "%.0f%%", totalUsage * 100.0 / totalMaxUses)
        } else {
            getString(R.string.promo_stat_not_available)
        }

        // Older promo docs may not store reach. maxUses is the real cap fallback, then usage count.
        val reach = allPromos.sumOf { promo ->
            promo.totalReach ?: promo.maxUses ?: promo.usageCount
        }
        totalReach.text = formatReach(reach)
    }

    private fun formatReach(value: Int): String {
        if (value < 1000) return value.toString()
        val thousands = value / 1000.0
        val formatted = String.format(Locale.US, "%.1f", thousands)
            .removeSuffix(".0")
        return "${formatted}k"
    }

    private fun TextView.bindChip(isSelected: Boolean) {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_promo_chip_selected
            } else {
                R.drawable.bg_promo_chip_unselected
            },
        )

        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_secondary
                },
            ),
        )
    }

    private fun confirmDeletePromo(promo: AdminPromoUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.promo_delete_title)
            .setMessage(R.string.promo_delete_message)
            .setNegativeButton(R.string.promo_delete_cancel, null)
            .setPositiveButton(R.string.promo_delete_confirm) { _, _ ->
                AdminPromoFirestoreRepository.deletePromo(promo.id) { result ->
                    if (!isAdded) return@deletePromo
                    result
                        .onSuccess {
                            showToast(R.string.promo_deleted)
                            loadFirestorePromos()
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Could not delete promo id=${promo.id}", error)
                            showToast(R.string.promo_delete_failed)
                        }
                }
            }
            .show()
    }

    private fun sharePromo(promo: AdminPromoUiModel) {
        val shareParts = buildList {
            add(getString(R.string.promo_share_intro, promo.code))
            promo.discountText?.takeIf { it.isNotBlank() }?.let { add(it) }
            promo.minSpendText?.takeIf { it.isNotBlank() }?.let {
                add(getString(R.string.promo_share_min_spend, it))
            }
            promo.expiryText?.takeIf { it.isNotBlank() }?.let {
                add(getString(R.string.promo_share_expires, it))
            }
        }
        val shareText = shareParts.joinToString(separator = "\n")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(shareIntent, getString(R.string.share))
        try {
            startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.promo_share_unavailable)
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class PromoFilter {
        ACTIVE,
        PAST,
        ALL,
    }

    companion object {
        private const val TAG = "ManagePromoCodes"
    }
}
