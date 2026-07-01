package com.devpro.pizzatime.feature.customer.support

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemSupportFaqBinding

class SupportFaqAdapter(
    private val onFaqClick: (SupportFaqUiModel) -> Unit,
) : RecyclerView.Adapter<SupportFaqAdapter.SupportFaqViewHolder>() {

    private val items = mutableListOf<SupportFaqUiModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<SupportFaqUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SupportFaqViewHolder {
        val binding = ItemSupportFaqBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SupportFaqViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SupportFaqViewHolder,
        position: Int,
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SupportFaqViewHolder(
        private val binding: ItemSupportFaqBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupportFaqUiModel) = with(binding) {
            tvQuestion.text = root.context.getString(item.questionRes)
            tvAnswer.text = root.context.getString(item.answerRes)
            tvAnswer.isVisible = item.isExpanded
            tvArrow.text = root.context.getString(
                if (item.isExpanded) {
                    R.string.support_faq_arrow_expanded
                } else {
                    R.string.support_faq_arrow_collapsed
                },
            )

            root.setOnClickListener {
                onFaqClick(item)
            }
        }
    }
}