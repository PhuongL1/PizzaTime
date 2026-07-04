package com.devpro.pizzatime.feature.admin.staff

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.FirebaseFeatureFlags
import com.devpro.pizzatime.databinding.FragmentManageStaffBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav

class ManageStaffFragment : Fragment() {

    private var _binding: FragmentManageStaffBinding? = null
    private val binding: FragmentManageStaffBinding
        get() = checkNotNull(_binding) {
            "FragmentManageStaffBinding is only valid between onCreateView and onDestroyView."
        }

    private var allStaff: List<AdminStaffUiModel> = FakeAdminStaffData.staff

    private val staffAdapter by lazy {
        AdminStaffAdapter(
            onEditClick = { showComingSoon(getString(R.string.edit_staff_format, it.name)) },
            onToggleStatusClick = { item ->
                val newActive = item.status == AdminStaffStatus.INACTIVE
                AdminStaffFirestoreRepository.toggleActive(item.id, newActive) { result ->
                    if (!isAdded) return@toggleActive
                    result
                        .onSuccess { loadFirestoreStaff() }
                        .onFailure {
                            Toast.makeText(
                                requireContext(),
                                R.string.admin_staff_toggle_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                }
            },
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
            if (!isAdded) return@loadStaff
            allStaff = result.getOrElse { FakeAdminStaffData.staff }
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
                showToast(R.string.admin_staff_create_disabled)
                return@setOnClickListener
            }

            showCreateStaffDialog()
        }

        tvMenu.setOnClickListener {
            showComingSoon(getString(R.string.menu))
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

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.admin_staff_create_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.admin_staff_create_action, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        createStaffAccount(
                            name = nameInput.text.toString().trim(),
                            email = emailInput.text.toString().trim(),
                            phone = phoneInput.text.toString().trim(),
                            password = passwordInput.text.toString(),
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
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        onCreated: () -> Unit,
    ) {
        if (!FirebaseFeatureFlags.CLOUD_FUNCTIONS_ENABLED) {
            showToast(R.string.admin_staff_create_disabled)
            return
        }

        when {
            name.isBlank() -> {
                showToast(R.string.admin_staff_create_name_required)
                return
            }

            !email.contains("@") -> {
                showToast(R.string.admin_staff_create_email_invalid)
                return
            }

            password.length < 6 -> {
                showToast(R.string.admin_staff_create_password_invalid)
                return
            }
        }

        AdminStaffFunctionsRepository.createStaffAccount(
            name = name,
            email = email,
            phone = phone,
            password = password,
            role = role,
        ) { result ->
            if (!isAdded) return@createStaffAccount
            result
                .onSuccess {
                    showToast(R.string.admin_staff_create_success)
                    loadFirestoreStaff()
                    onCreated()
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.admin_staff_create_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
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

    private fun showComingSoon(action: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_action_coming_soon, action),
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

    private enum class StaffFilter {
        ALL,
        KITCHEN,
        SHIPPER,
        ADMIN,
    }

    private companion object {
        val STAFF_CREATION_ROLES = listOf("STAFF", "KITCHEN", "SHIPPER")
    }
}
