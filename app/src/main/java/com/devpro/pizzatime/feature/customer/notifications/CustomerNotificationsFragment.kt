package com.devpro.pizzatime.feature.customer.notifications

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerNotificationsBinding
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerMemberQr
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory

class CustomerNotificationsFragment : Fragment(R.layout.fragment_customer_notifications) {

    private var _binding: FragmentCustomerNotificationsBinding? = null
    private val binding: FragmentCustomerNotificationsBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerNotificationsBinding is only valid between onViewCreated and onDestroyView."
        }

    private val adapter = CustomerNotificationAdapter(
        onNotificationClick = ::openNotification,
    )

    private var notifications = FakeCustomerNotificationsData.getNotifications()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCustomerNotificationsBinding.bind(view)

        setupRecyclerView()
        setupActions()
        setupBottomNav()
        renderNotifications()
    }

    private fun setupRecyclerView() = with(binding.rvNotifications) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@CustomerNotificationsFragment.adapter
    }

    private fun setupActions() = with(binding) {
        btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun setupBottomNav() = with(binding.bottomNav) {
        bindBottomNavItem(navMenu)
        bindBottomNavItem(navOrders)
        bindBottomNavItem(navLoyalty)
        bindBottomNavItem(navProfile)

        navMenu.setOnClickListener {
            openCustomerHome(addToBackStack = false)
        }

        navOrders.setOnClickListener {
            openCustomerOrderHistory()
        }

        navLoyalty.setOnClickListener {
            openCustomerMemberQr()
        }

        navProfile.setOnClickListener {
            openCustomerAccount()
        }
    }

    private fun bindBottomNavItem(item: TextView) {
        item.setBackgroundResource(0)
        item.setTextColor(requireContext().getColor(R.color.pt_text_primary))
    }

    private fun renderNotifications() {
        adapter.submitList(notifications)
    }

    private fun markAllAsRead() {
        notifications = notifications.map { notification ->
            notification.copy(isUnread = false)
        }

        renderNotifications()

        Toast.makeText(
            requireContext(),
            getString(R.string.customer_notifications_marked_read_toast),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun openNotification(notification: CustomerNotificationUiModel) {
        Toast.makeText(
            requireContext(),
            getString(R.string.customer_notifications_open_toast, notification.title),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}