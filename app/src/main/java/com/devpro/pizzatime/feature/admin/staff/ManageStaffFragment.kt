package com.devpro.pizzatime.feature.admin.staff

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.FirebaseFeatureFlags
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentManageStaffBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ManageStaffFragment : Fragment() {

    private var _binding: FragmentManageStaffBinding? = null
    private val binding: FragmentManageStaffBinding
        get() = checkNotNull(_binding) {
            "FragmentManageStaffBinding is only valid between onCreateView and onDestroyView."
        }

    private var allStaff: List<AdminStaffUiModel> = FakeAdminStaffData.staff

    private val staffAdapter by lazy {
        AdminStaffAdapter(
            onEditClick = { showUnavailableAction(getString(R.string.edit_staff_format, it.name)) },
            onToggleStatusClick = ::confirmToggleStaffStatus,
        )
    }

    private var selectedFilter = StaffFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManageStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupActions()
        setupFilters()
        setupBottomNav()
        renderStaff()
        loadFirestoreStaff()
    }

    private fun loadFirestoreStaff() {
        AdminStaffFirestoreRepository.loadStaff { result ->
            if (_binding == null || !isAdded) return@loadStaff
            allStaff = result.getOrElse { error ->
                Log.e(TAG, "Failed to load admin staff", error)
                showUiMessage(R.string.feedback_action_failed, UiMessageType.ERROR)
                FakeAdminStaffData.staff
            }
            renderStaff()
        }
    }

    private fun setupRecyclerView() = with(binding.rvStaff) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = staffAdapter
        itemAnimator = null
    }

    private fun setupActions() = with(binding) {
        btnAddStaff.setOnClickListener {
            if (!FirebaseFeatureFlags.CLOUD_FUNCTIONS_ENABLED) {
                showUiMessage(R.string.admin_staff_create_disabled, UiMessageType.WARNING)
                return@setOnClickListener
            }

            showCreateStaffDialog()
        }
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun showCreateStaffDialog() {
        val nameInput = createDialogInput(
            hint = getString(R.string.admin_staff_create_name_hint),
        )
        val emailInput = createDialogInput(
            hint = getString(R.string.admin_staff_create_email_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        )
        val phoneInput = createDialogInput(
            hint = getString(R.string.admin_staff_create_phone_hint),
            inputType = InputType.TYPE_CLASS_PHONE,
        )
        val passwordInput = createDialogInput(
            hint = getString(R.string.admin_staff_create_password_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        val roleSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                STAFF_CREATION_ROLES,
            )
        }

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.customer_account_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(nameInput)
            addView(emailInput)
            addView(phoneInput)
            addView(passwordInput)
            addView(roleSpinner)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.admin_staff_create_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.admin_staff_create_action, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        createStaffAccount(
                            nameInput = nameInput,
                            emailInput = emailInput,
                            phoneInput = phoneInput,
                            passwordInput = passwordInput,
                            role = roleSpinner.selectedItem.toString(),
                            onCreated = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun createDialogInput(
        hint: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
    ): EditText {
        return EditText(requireContext()).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
        }
    }

    private fun createStaffAccount(
        nameInput: EditText,
        emailInput: EditText,
        phoneInput: EditText,
        passwordInput: EditText,
        role: String,
        onCreated: () -> Unit,
    ) {
        if (!FirebaseFeatureFlags.CLOUD_FUNCTIONS_ENABLED) {
            showUiMessage(R.string.admin_staff_create_disabled, UiMessageType.WARNING)
            return
        }
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString()
        nameInput.error = if (name.isBlank()) getString(R.string.admin_staff_create_name_required) else null
        emailInput.error = if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            null
        } else {
            getString(R.string.admin_staff_create_email_invalid)
        }
        phoneInput.error = if (phone.isBlank()) getString(R.string.admin_staff_create_phone_required) else null
        passwordInput.error = if (password.length >= MIN_STAFF_PASSWORD_LENGTH) {
            null
        } else {
            getString(R.string.admin_staff_create_password_invalid)
        }
        if (listOf(nameInput, emailInput, phoneInput, passwordInput).any { it.error != null }) return

        AdminStaffFunctionsRepository.createStaffAccount(
            name = name,
            email = email,
            phone = phone,
            password = password,
            role = role,
        ) { result ->
            if (_binding == null || !isAdded) return@createStaffAccount
            result
                .onSuccess {
                    onCreated()
                    showUiMessage(R.string.admin_staff_create_success, UiMessageType.SUCCESS)
                    loadFirestoreStaff()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create admin staff account role=$role", error)
                    showUiMessage(R.string.admin_staff_create_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun confirmToggleStaffStatus(item: AdminStaffUiModel) {
        val newActive = item.status == AdminStaffStatus.INACTIVE
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.admin_staff_status_confirmation_title)
            .setMessage(getString(R.string.admin_staff_status_confirmation_message, item.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.admin_staff_status_confirmation_action) { _, _ ->
                updateStaffStatus(item, newActive)
            }
            .show()
    }

    private fun updateStaffStatus(item: AdminStaffUiModel, newActive: Boolean) {
        AdminStaffFirestoreRepository.toggleActive(item.id, newActive) { result ->
            if (_binding == null || !isAdded) return@toggleActive
            result
                .onSuccess {
                    showUiMessage(R.string.admin_staff_status_updated, UiMessageType.SUCCESS)
                    loadFirestoreStaff()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update staffId=${item.id} active=$newActive", error)
                    showUiMessage(R.string.admin_staff_toggle_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun setupFilters() = with(binding) {
        tvChipAllStaff.setOnClickListener {
            selectedFilter = StaffFilter.ALL
            renderStaff()
        }

        tvChipKitchen.setOnClickListener {
            selectedFilter = StaffFilter.KITCHEN
            renderStaff()
        }

        tvChipShipper.setOnClickListener {
            selectedFilter = StaffFilter.SHIPPER
            renderStaff()
        }

        tvChipAdmin.setOnClickListener {
            selectedFilter = StaffFilter.ADMIN
            renderStaff()
        }
    }

    private fun setupBottomNav() {
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.STAFF,
            onDashboardClick = { openAdminDashboard() },
            onManageMenuClick = { openManageMenu() },
            onManagePromoCodesClick = { openShipperDeliveryDashboard() },
            onManageStaffClick = { openCustomerAccount() },
        )
    }

    private fun renderStaff() {
        val staff = allStaff.filter { item ->
            when (selectedFilter) {
                StaffFilter.ALL -> true
                StaffFilter.KITCHEN -> item.role == AdminStaffRole.KITCHEN
                StaffFilter.SHIPPER -> item.role == AdminStaffRole.SHIPPER
                StaffFilter.ADMIN -> item.role == AdminStaffRole.ADMIN
            }
        }

        staffAdapter.submitList(staff)
        binding.rvStaff.isVisible = staff.isNotEmpty()
        binding.tvEmptyStaff.isVisible = staff.isEmpty()
        renderFilterState()
    }

    private fun renderFilterState() = with(binding) {
        tvChipAllStaff.bindChip(selectedFilter == StaffFilter.ALL)
        tvChipKitchen.bindChip(selectedFilter == StaffFilter.KITCHEN)
        tvChipShipper.bindChip(selectedFilter == StaffFilter.SHIPPER)
        tvChipAdmin.bindChip(selectedFilter == StaffFilter.ADMIN)
    }

    private fun TextView.bindChip(isSelected: Boolean) {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_admin_staff_chip_selected
            } else {
                R.drawable.bg_admin_staff_chip_unselected
            },
        )

        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) {
                    R.color.pt_gold_light
                } else {
                    R.color.pt_text_secondary
                },
            ),
        )
    }

    private fun showUnavailableAction(action: String) {
        showUiMessage(
            textRes = R.string.staff_action_coming_soon,
            type = UiMessageType.INFO,
            args = listOf(action),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class StaffFilter {
        ALL,
        KITCHEN,
        SHIPPER,
        ADMIN,
    }

    private companion object {
        const val TAG = "ManageStaff"
        const val MIN_STAFF_PASSWORD_LENGTH = 6
        val STAFF_CREATION_ROLES = listOf("STAFF", "KITCHEN", "SHIPPER")
    }
}
