package com.example.todo_p

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.addition.R
import com.example.addition.TodoModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class TodoAdapter (val list : List<TodoModel>) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_todo, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return list[position].id // Assuming each TodoModel has a unique `id` field.
    }


    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(list[position])
    }

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(todoModel: TodoModel) {
            with(itemView){
//                val colors = resources.getIntArray(R.array.random_color)
//                val randomColor = colors[Random().nextInt(colors.size)]
//                findViewById<View>(R.id.viewColorTag).setBackgroundColor(randomColor)
                findViewById<TextView>(R.id.txtShowTitle).text = todoModel.input
                findViewById<TextView>(R.id.txtShowTask).text = todoModel.result
//                findViewById<TextView>(R.id.txtShowCategory).text = todoModel.category
            }

        }

    }

}