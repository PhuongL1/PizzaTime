package com.devpro.pizzatime.feature.customer.support

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentSupportFaqBinding
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerMemberQr
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory

class SupportFaqFragment : Fragment(R.layout.fragment_support_faq) {

    private var _binding: FragmentSupportFaqBinding? = null
    private val binding: FragmentSupportFaqBinding
        get() = checkNotNull(_binding) {
            "FragmentSupportFaqBinding is only valid between onViewCreated and onDestroyView."
        }

    private var allFaqs = FakeSupportFaqData.getFaqs()
    private var selectedCategory = SupportTopicCategory.ALL
    private var searchQuery = ""

    private val adapter = SupportFaqAdapter(
        onFaqClick = ::toggleFaq,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSupportFaqBinding.bind(view)

        setupRecyclerView()
        setupSearch()
        setupActions()
        setupBottomNav()
        renderFaqs()
    }

    private fun setupRecyclerView() = with(binding.rvFaq) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@SupportFaqFragment.adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    searchQuery = text?.toString()?.trim() ?: ""
                    renderFaqs()
                }

                override fun afterTextChanged(editable: Editable?) = Unit
            },
        )
    }

    private fun setupActions() = with(binding) {
        cardDelivery.setOnClickListener {
            selectCategory(SupportTopicCategory.DELIVERY)
        }

        cardPayments.setOnClickListener {
            selectCategory(SupportTopicCategory.PAYMENTS)
        }

        btnContactSupport.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.support_contact_support_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupBottomNav() = with(binding.bottomNav) {
        bindBottomNavItem(
            item = navMenu,
            selected = true,
        )
        bindBottomNavItem(
            item = navOrders,
            selected = false,
        )
        bindBottomNavItem(
            item = navLoyalty,
            selected = false,
        )
        bindBottomNavItem(
            item = navProfile,
            selected = false,
        )

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

    private fun bindBottomNavItem(
        item: TextView,
        selected: Boolean,
    ) {
        item.setBackgroundResource(
            if (selected) {
                R.drawable.bg_bottom_nav_item_selected
            } else {
                0
            },
        )

        item.setTextColor(
            requireContext().getColor(
                if (selected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_primary
                },
            ),
        )
    }

    private fun selectCategory(category: SupportTopicCategory) {
        selectedCategory = if (selectedCategory == category) {
            SupportTopicCategory.ALL
        } else {
            category
        }

        renderFaqs()
    }

    private fun renderFaqs() {
        val filteredFaqs = allFaqs
            .filter { faq ->
                selectedCategory == SupportTopicCategory.ALL ||
                        faq.category == selectedCategory ||
                        faq.category == SupportTopicCategory.ALL
            }
            .filter { faq ->
                matchesSearch(faq)
            }

        adapter.submitList(filteredFaqs)
    }

    private fun matchesSearch(faq: SupportFaqUiModel): Boolean {
        if (searchQuery.isBlank()) {
            return true
        }

        val question = getString(faq.questionRes)
        val answer = getString(faq.answerRes)

        return question.contains(searchQuery, ignoreCase = true) ||
                answer.contains(searchQuery, ignoreCase = true)
    }

    private fun toggleFaq(selectedFaq: SupportFaqUiModel) {
        allFaqs = allFaqs.map { faq ->
            if (faq.id == selectedFaq.id) {
                faq.copy(isExpanded = !selectedFaq.isExpanded)
            } else {
                faq
            }
        }

        renderFaqs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}