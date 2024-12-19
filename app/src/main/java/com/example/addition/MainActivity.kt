package com.example.addition

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.addition.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityMainBinding

    // Store the input as a string to build the equation
    private var input: String = ""
    private var operatorPressed: Boolean = false

    private lateinit var appDatabase: AppDatabase
    private lateinit var todoDao: TodoDao

    companion object {
        const val PLUS = "+"
        const val MINUS = "-"
        const val MULTIPLY = "*"
        const val DIVIDE = "/"
        const val MODULO = "%"
        const val SQUARE_ROOT = "√"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Room database instance
        appDatabase = AppDatabase.getDatabase(this)
        todoDao = appDatabase.todoDao()

        setSupportActionBar(binding.toolbar)
        setupButtonListeners()

        // Observer for tasks
        val tasksObserver = Observer<List<TodoModel>> { tasks ->
            // You can update the UI here if you want to display the list of tasks
        }
        // Get the LiveData from the Dao and observe it
        todoDao.getTask().observe(this, tasksObserver)
    }

    private fun setupButtonListeners() {
        setupNumberButtons()
        setupOperatorButtons()
        setupSpecialButtons()
    }

    private fun setupNumberButtons() {
        binding.btn0.setOnClickListener { appendInput("0") }
        binding.btn1.setOnClickListener { appendInput("1") }
        binding.btn2.setOnClickListener { appendInput("2") }
        binding.btn3.setOnClickListener { appendInput("3") }
        binding.btn4.setOnClickListener { appendInput("4") }
        binding.btn5.setOnClickListener { appendInput("5") }
        binding.btn6.setOnClickListener { appendInput("6") }
        binding.btn7.setOnClickListener { appendInput("7") }
        binding.btn8.setOnClickListener { appendInput("8") }
        binding.btn9.setOnClickListener { appendInput("9") }
        binding.btnDot.setOnClickListener { appendInput(".") }
    }

    private fun setupOperatorButtons() {
        binding.btnPlus.setOnClickListener { appendOperator(PLUS) }
        binding.btnMinus.setOnClickListener { appendOperator(MINUS) }
        binding.btnMultiply.setOnClickListener { appendOperator(MULTIPLY) }
        binding.btnDivide.setOnClickListener { appendOperator(DIVIDE) }
        binding.btnPercent.setOnClickListener { appendOperator(MODULO) }
        binding.btnRoot.setOnClickListener { appendOperator(SQUARE_ROOT) }
    }

    private fun setupSpecialButtons() {
        binding.btnEquals.setOnClickListener { calculateResult() }
        binding.btnClear.setOnClickListener { clearInput() }
        binding.btnDelete.setOnClickListener { deleteLastChar() }
    }

    private fun appendInput(value: String) {
        if (value == ".") {
            // Prevent multiple dots in a single number
            if (input.takeLastWhile { it.isDigit() || it == '.' }.contains(".")) return
            if (input.isEmpty() || input.endsWith(" ")) input += "0"
        }

        // Handle negative numbers
        if (value == "-") {
            // Allow '-' at the start or after an operator
            if (input.isEmpty() || input.endsWith(" ")) {
                input += value
            } else {
                return // Ignore invalid '-' placement
            }
        } else {
            input += value
        }

        binding.display.text = input.toEditable()
    }



    private fun appendOperator(operator: String) {
        if (operator == SQUARE_ROOT) {
            input = "$SQUARE_ROOT($input)"
            binding.display.text = input.toEditable()
            return
        }

        if (input.isNotEmpty() && !operatorPressed && !input.endsWith(" ")) {
            input += " $operator "
            operatorPressed = true
        } else if (input.isEmpty() && operator == MINUS) {
            // Allow '-' as the first operator for a negative number
            input += operator
        }
        binding.display.text = input.toEditable()
    }


    private fun calculateResult() {
        try {
            val inputDatabase = input
            val tokens = input.split(" ").filter { it.isNotBlank() }
            val values = mutableListOf<BigDecimal>()
            val operators = mutableListOf<String>()

            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                when {
                    token.toBigDecimalOrNull() != null -> {
                        values.add(BigDecimal(token))
                    }
                    token == MINUS && (i == 0 || tokens[i - 1] in listOf(PLUS, MINUS, MULTIPLY, DIVIDE, MODULO)) -> {
                        // Handle negative numbers
                        values.add(BigDecimal(tokens[++i]).negate())
                    }
                    token in listOf(PLUS, MINUS, MULTIPLY, DIVIDE, MODULO) -> {
                        operators.add(token)
                    }
                    token.startsWith(SQUARE_ROOT) -> {
                        val number = token.substringAfter("(").substringBefore(")").toDoubleOrNull()
                        if (number != null) {
                            values.add(BigDecimal(Math.sqrt(number)))
                        } else {
                            throw IllegalArgumentException("Invalid input for square root")
                        }
                    }
                }
                i++
            }

            var result = values[0]
            for (j in operators.indices) {
                val operator = operators[j]
                val operand2 = values[j + 1]
                result = when (operator) {
                    PLUS -> result.add(operand2)
                    MINUS -> result.subtract(operand2)
                    MULTIPLY -> result.multiply(operand2)
                    DIVIDE -> if (operand2 != BigDecimal.ZERO) result.divide(operand2, 10, RoundingMode.HALF_UP)
                    else throw ArithmeticException("Division by zero")
                    MODULO -> result.remainder(operand2)
                    else -> throw IllegalArgumentException("Invalid Operator")
                }
            }

            val resultString = result.stripTrailingZeros().toPlainString()

            // Update the display
            binding.display.text = resultString.toEditable()
            input = resultString

            // Save to database
            val todoModel = TodoModel(input = inputDatabase, result = resultString)
            insertResult(todoModel)

        } catch (e: Exception) {
            Toast.makeText(this, "Error in calculation: ${e.message}", Toast.LENGTH_SHORT).show()
            clearInput()
        }
    }


    private fun insertResult(todoModel: TodoModel) {
        // Launch a coroutine to insert the result into the database
        lifecycleScope.launch {
            try {
                todoDao.insetTask(todoModel) // Call the suspend function within the coroutine
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error inserting task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearInput() {
        input = ""
        binding.display.text = "0".toEditable()
        operatorPressed = false
    }

    private fun deleteLastChar() {
        if (input.isNotEmpty()) {
            if (input.last() == ' ') {
                input = input.dropLast(3)
            } else {
                input = input.dropLast(1)
            }
            binding.display.text = if (input.isEmpty()) "0".toEditable() else input.toEditable()
        }
    }

    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.history -> {
                startActivity(Intent(this, History::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
