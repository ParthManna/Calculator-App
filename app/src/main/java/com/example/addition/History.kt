package com.example.addition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.addition.databinding.ActivityHistoryBinding
import com.example.todo_p.TodoAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class History : AppCompatActivity() {

    private val todoList = arrayListOf<TodoModel>() // Define the list of tasks
    private val adapter: TodoAdapter by lazy { TodoAdapter(todoList) } // Define the adapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge layout if necessary
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.todoRv.layoutManager = LinearLayoutManager(this)

        // Initialize RecyclerView and adapter
        binding.todoRv.adapter = adapter

        // Set up swipe functionality
        initSwipe()

        // Observe tasks from the database
        observeTasks()
    }

    private fun initSwipe() {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                val todo = todoList[position]

                lifecycleScope.launch(Dispatchers.IO) {
                    when (direction) {
                        ItemTouchHelper.LEFT -> { // Swipe left to delete
                            db.todoDao().deleteTask(todo.id)
                            withContext(Dispatchers.Main) {
                                todoList.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                showUndoSnackbar(todo, position, ItemTouchHelper.LEFT)
                            }
                        }

                        ItemTouchHelper.RIGHT -> { // Swipe right to complete
                            db.todoDao().finishTask(todo.id)
                            withContext(Dispatchers.Main) {
                                todoList.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                showUndoSnackbar(todo.copy(isFinished = -1), position, ItemTouchHelper.RIGHT)
                            }
                        }
                    }
                }
            }

            private fun showUndoSnackbar(todo: TodoModel, originalPosition: Int, direction: Int) {
                val actionText = if (direction == ItemTouchHelper.LEFT) "deleted" else "completed"

                Snackbar.make(binding.root, "Task $actionText", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Adjust task state for undo
                                val restoredTask = todo.copy(
                                    isFinished = if (direction == ItemTouchHelper.RIGHT) -1 else todo.isFinished
                                )

                                restoredTask.id = 0

                                // Re-insert task into the database
                                db.todoDao().insetTask(restoredTask)

                                withContext(Dispatchers.Main) {
                                    // Add the task back to the list at the original position
                                    todoList.add(originalPosition, restoredTask)
                                    adapter.notifyItemInserted(originalPosition)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Snackbar.make(binding.root, "Failed to undo action", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }.show()
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    val paint = Paint()
                    val icon: Bitmap

                    if (dX > 0) { // Swipe right (mark as complete)
                        icon = BitmapFactory.decodeResource(resources, R.drawable.checkmark_32)
                        paint.color = Color.parseColor("#00ff95")
                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.left.toFloat() + dX, itemView.bottom.toFloat(), paint
                        )
                        canvas.drawBitmap(
                            icon,
                            itemView.left.toFloat() + 32,
                            itemView.top.toFloat() + (itemView.height - icon.height) / 2f,
                            paint
                        )
                    } else { // Swipe left (delete)
                        icon = BitmapFactory.decodeResource(resources, R.drawable.delete_32)
                        paint.color = Color.parseColor("#FF0000")
                        canvas.drawRect(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                        canvas.drawBitmap(
                            icon,
                            itemView.right.toFloat() - icon.width - 32,
                            itemView.top.toFloat() + (itemView.height - icon.height) / 2f,
                            paint
                        )
                    }

                    viewHolder.itemView.translationX = dX
                } else {
                    viewHolder.itemView.translationX = 0f
                    super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.todoRv)
    }

    private fun observeTasks() {
        Log.d("HistoryActivity", "observeTasks() called")
        db.todoDao().getTask().observe(this) { tasks ->
            todoList.clear()
            if (!tasks.isNullOrEmpty()) {
                todoList.addAll(0, tasks)
                if (binding.emptyNotification.visibility == View.VISIBLE) {
                    binding.emptyNotification.visibility = View.GONE
                    binding.todoRv.visibility = View.VISIBLE
                }
            } else {
                if (binding.todoRv.visibility == View.VISIBLE) {
                    binding.emptyNotification.visibility = View.VISIBLE
                    binding.todoRv.visibility = View.GONE
                }
            }
            adapter.notifyDataSetChanged()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clearall -> {
                clearAllTasks()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearAllTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Clear all tasks from the database
                db.todoDao().clearAllTasks()

                withContext(Dispatchers.Main) {
                    // Clear the list and notify the adapter
                    todoList.clear()
                    adapter.notifyDataSetChanged()

                    // Show confirmation message
                    Snackbar.make(binding.root, "All tasks cleared", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Failed to clear tasks", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
