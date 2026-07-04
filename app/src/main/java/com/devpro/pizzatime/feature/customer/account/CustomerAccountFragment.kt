package com.devpro.pizzatime.feature.customer.account

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerAccountBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.bottomnav.setupCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.topbar.setupCustomerTopBar
import com.devpro.pizzatime.feature.staff.navigation.openCustomerFavorites
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.signOutAndOpenLogin
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

class CustomerAccountFragment : Fragment() {

    private var _binding: FragmentCustomerAccountBinding? = null
    private val binding: FragmentCustomerAccountBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerAccountBinding is only valid between onCreateView and onDestroyView."
        }

    private var accountData: CustomerAccountUiModel = FakeCustomerAccountData.getCustomerAccount()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindAccount()
        setupTopBar()
        setupBottomNav()
        setupActions()
        loadCustomerProfile()
    }

    private fun bindAccount() = with(binding) {
        ivAvatar.setImageResource(accountData.avatarRes)
        tvCustomerName.text = accountData.fullName
        tvTierName.text = accountData.tierName
        tvDoughPoints.text = getString(
            R.string.customer_account_dough_points,
            formatNumber(accountData.doughPoints),
        )
        tvEmailValue.text = accountData.email
        tvPhoneValue.text = accountData.phone
    }

    private fun setupTopBar() = with(binding) {
        setupCustomerTopBar(
            topBar = customerTopBar,
            cartItemCount = 0,
            onCartClick = {
                showToast(getString(R.string.customer_account_cart_toast))
            },
        )
    }

    private fun setupBottomNav() = with(binding) {
        setupCustomerBottomNav(
            bottomNav = customerBottomNav,
            selectedTab = CustomerBottomNavTab.PROFILE,
            onCustomerMenuClick = {
                openCustomerHome()
            },
            onCustomerOrdersClick = {
                openCustomerOrderHistory()
            },
            onCustomerLoyaltyClick = {
                openCustomerPromoCodes()
            },
        )
    }

    private fun setupActions() = with(binding) {
        editAvatarButton.setOnClickListener {
            showEditProfileDialog()
        }

        rowOrderHistory.setOnClickListener {
            openCustomerOrderHistory()
        }

        rowPaymentMethods.setOnClickListener {
            showToast(getString(R.string.customer_account_payment_methods_toast))
        }

        rowDeliveryAddresses.setOnClickListener {
            showEditProfileDialog()
        }

        rowFavorites.setOnClickListener {
            openCustomerFavorites()
        }

        rowSettings.setOnClickListener {
            showToast(getString(R.string.customer_account_settings_toast))
        }

        logoutCard.setOnClickListener {
            showToast(getString(R.string.customer_account_logout_toast))
            signOutAndOpenLogin()
        }
    }

    private fun loadCustomerProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(getString(R.string.customer_account_login_required_toast))
            return
        }

        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (!isAdded) return@loadProfile
            result
                .onSuccess { profile ->
                    accountData = profile
                    bindAccount()
                }
                .onFailure {
                    showToast(getString(R.string.customer_account_profile_load_failed))
                }
        }
    }

    private fun showEditProfileDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(getString(R.string.customer_account_login_required_toast))
            return
        }

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
            addView(addressInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.customer_account_edit_profile_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.customer_account_edit_profile_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        saveProfile(
                            uid = uid,
                            name = nameInput.text.toString().trim(),
                            phone = phoneInput.text.toString().trim(),
                            deliveryAddress = addressInput.text.toString().trim(),
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
        if (name.isBlank()) {
            showToast(getString(R.string.customer_account_name_required_toast))
            return
        }

        CustomerProfileFirestoreRepository.updateProfile(
            uid = uid,
            name = name,
            phone = phone,
            deliveryAddress = deliveryAddress,
        ) { result ->
            if (!isAdded) return@updateProfile
            result
                .onSuccess {
                    accountData = accountData.copy(
                        fullName = name,
                        phone = phone,
                        deliveryAddress = deliveryAddress,
                    )
                    bindAccount()
                    showToast(getString(R.string.customer_account_profile_saved_toast))
                    onSaved()
                }
                .onFailure {
                    showToast(getString(R.string.customer_account_profile_save_failed))
                }
        }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
