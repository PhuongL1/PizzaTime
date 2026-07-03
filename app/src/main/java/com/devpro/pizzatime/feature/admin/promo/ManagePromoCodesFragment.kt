package com.devpro.pizzatime.feature.admin.promo

import android.app.AlertDialog
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
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav
import java.util.Locale

class ManagePromoCodesFragment : Fragment() {

    private var _binding: FragmentManagePromoCodesBinding? = null
    private val binding: FragmentManagePromoCodesBinding
        get() = checkNotNull(_binding) {
            "FragmentManagePromoCodesBinding is only valid between onCreateView and onDestroyView."
        }

    private var allPromos: List<AdminPromoUiModel> = FakeAdminPromoData.promos

    private val promoAdapter by lazy {
        AdminPromoAdapter(
            onEditClick = { promo -> showEditPromoDialog(promo) },
            onDeleteClick = { promo ->
                AdminPromoFirestoreRepository.setActive(promo.id, false) { result ->
                    if (!isAdded) return@setActive
                    if (result.isSuccess) loadFirestorePromos()
                }
            },
            onShareClick = { showActionToast(getString(R.string.share), it) },
            onReactivateClick = { promo ->
                AdminPromoFirestoreRepository.setActive(promo.id, true) { result ->
                    if (!isAdded) return@setActive
                    if (result.isSuccess) loadFirestorePromos()
                }
            },
        )
    }

    private var selectedFilter = PromoFilter.ACTIVE

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
        setupActions()
        setupFilters()
        setupBottomNav()
        renderPromos()
        loadFirestorePromos()
    }

    private fun loadFirestorePromos() {
        AdminPromoFirestoreRepository.loadPromos { result ->
            if (!isAdded) return@loadPromos
            allPromos = result.getOrElse { FakeAdminPromoData.promos }
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
        layoutManager = LinearLayoutManager(requireContext())
        adapter = promoAdapter
        itemAnimator = null
    }

    private fun setupActions() = with(binding) {
        btnAddPromo.setOnClickListener {
            showCreatePromoDialog()
        }

        btnMenu.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.promo_action_coming_soon, getString(R.string.menu)),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupFilters() = with(binding) {
        tvChipActive.setOnClickListener {
            selectedFilter = PromoFilter.ACTIVE
            renderPromos()
        }

        tvChipInactive.setOnClickListener {
            selectedFilter = PromoFilter.INACTIVE
            renderPromos()
        }

        tvChipScheduled.setOnClickListener {
            selectedFilter = PromoFilter.SCHEDULED
            renderPromos()
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            StaffBottomNavTab.PROFILE,
            { openAdminDashboard() },
            { openKitchenBoard() },
            { openShipperDeliveryDashboard() },
            {},
        )
    }

    private fun renderPromos() {
        val promos = when (selectedFilter) {
            PromoFilter.ACTIVE -> allPromos.filter {
                it.status == AdminPromoStatus.ACTIVE
            }

            PromoFilter.INACTIVE -> allPromos.filter {
                it.status == AdminPromoStatus.INACTIVE || it.status == AdminPromoStatus.EXPIRED
            }

            PromoFilter.SCHEDULED -> allPromos.filter {
                it.status == AdminPromoStatus.SCHEDULED
            }
        }

        promoAdapter.submitList(promos)
        binding.rvPromos.isVisible = promos.isNotEmpty()
        binding.tvEmptyPromos.isVisible = promos.isEmpty()
        renderFilterState()
    }

    private fun renderFilterState() = with(binding) {
        tvChipActive.bindChip(selectedFilter == PromoFilter.ACTIVE)
        tvChipInactive.bindChip(selectedFilter == PromoFilter.INACTIVE)
        tvChipScheduled.bindChip(selectedFilter == PromoFilter.SCHEDULED)
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

    private fun showActionToast(action: String, promo: AdminPromoUiModel) {
        Toast.makeText(
            requireContext(),
            getString(R.string.promo_action_coming_soon, "$action ${promo.code}"),
            Toast.LENGTH_SHORT,
        ).show()
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
        INACTIVE,
        SCHEDULED,
    }
}
