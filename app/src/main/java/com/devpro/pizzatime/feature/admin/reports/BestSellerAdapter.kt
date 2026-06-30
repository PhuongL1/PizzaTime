package com.devpro.pizzatime.feature.admin.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.databinding.ItemReportBestSellerBinding

class BestSellerAdapter :
    ListAdapter<BestSellerUiModel, BestSellerAdapter.BestSellerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestSellerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemReportBestSellerBinding.inflate(inflater, parent, false)
        return BestSellerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BestSellerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BestSellerViewHolder(
        private val binding: ItemReportBestSellerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BestSellerUiModel) = with(binding) {
            ivPizza.setImageResource(item.imageRes)
            tvRank.text = item.rank
            tvPizzaName.text = item.name
            tvSold.text = item.soldText
            progressSold.progress = item.progress
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BestSellerUiModel>() {
        override fun areItemsTheSame(
            oldItem: BestSellerUiModel,
            newItem: BestSellerUiModel,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: BestSellerUiModel,
            newItem: BestSellerUiModel,
        ): Boolean = oldItem == newItem
    }
}