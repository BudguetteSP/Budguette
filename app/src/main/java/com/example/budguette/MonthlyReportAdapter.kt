package com.example.budguette

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MonthlyReportAdapter(
    private val reports: List<MonthlyReport>
) : RecyclerView.Adapter<MonthlyReportAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val monthText: TextView = view.findViewById(R.id.monthText)
        val spentText: TextView = view.findViewById(R.id.spentText)
        val statusText: TextView = view.findViewById(R.id.statusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monthly_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.monthText.text = report.month
        holder.spentText.text = "$${"%.2f".format(report.spent)}"
        holder.statusText.text = if (report.overBudget) "Over" else "Under"
        holder.statusText.setTextColor(
            if (report.overBudget) Color.parseColor("#FF6347") // red
            else Color.parseColor("#32CD32") // green
        )
    }

    override fun getItemCount(): Int = reports.size
}
