package ml.komarov.markscanner.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ml.komarov.markscanner.R
import ml.komarov.markscanner.db.History
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class HistoryRecyclerAdapter(
    private val values: List<History>,
    private val onClickListener: (History) -> Unit
) :
    RecyclerView.Adapter<HistoryRecyclerAdapter.HistoryRecyclerViewHolder>() {

    override fun getItemCount() = values.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryRecyclerViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.card_history, parent, false)
        return HistoryRecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HistoryRecyclerViewHolder, position: Int) {
        val history = values[position]
        val codeData = history.data

        if (codeData != null) {
            val json = JSONObject(codeData)

            val date = Date(json.getLong("checkDate"))
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
            val dateReadable = format.format(date)

            val productName = json.getString("productName")

            holder.textViewId?.text = history.id.toString()
            holder.textViewCode?.text = history.code
            holder.textViewName?.text = productName
            holder.textViewDate?.text = dateReadable

            holder.cardHistory?.setOnClickListener { onClickListener.invoke(history) }
        }
    }

    class HistoryRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardHistory: CardView? = null
        var textViewId: TextView? = null
        var textViewCode: TextView? = null
        var textViewName: TextView? = null
        var textViewDate: TextView? = null

        init {
            cardHistory = itemView.findViewById(R.id.cardHistory)
            textViewId = itemView.findViewById(R.id.tvId)
            textViewCode = itemView.findViewById(R.id.tvCode)
            textViewName = itemView.findViewById(R.id.tvProductName)
            textViewDate = itemView.findViewById(R.id.tvDate)
        }
    }

    interface OnHistoryClickListener {
        fun onHistoryClick(position: Int)
    }
}