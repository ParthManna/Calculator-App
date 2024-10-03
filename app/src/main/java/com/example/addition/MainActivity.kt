package com.example.addition

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.addition.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityMainBinding

    // Store the input as a string to build the equation
    private var input: String = ""
    private var operatorPressed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the click listeners for the numeric buttons
        setupNumberButtons()

        // Set up the click listeners for the operator buttons
        setupOperatorButtons()

        // Set up the click listener for the equals button
        binding.btnEquals.setOnClickListener {
            calculateResult()
        }

        // Set up the clear button
        binding.btnClear.setOnClickListener {
            clearInput()
        }

        // Set up the delete button
        binding.btnDelete.setOnClickListener {
            deleteLastChar()
        }
    }

    // Function to handle numeric button clicks
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

    // Function to handle operator button clicks
    private fun setupOperatorButtons() {
        binding.btnPlus.setOnClickListener { appendOperator("+") }
        binding.btnMinus.setOnClickListener { appendOperator("-") }
        binding.btnMultiply.setOnClickListener { appendOperator("*") }
        binding.btnDivide.setOnClickListener { appendOperator("/") }
        binding.btnPercent.setOnClickListener { appendOperator("%") }
    }

    // Append number or dot to input
    private fun appendInput(value: String) {
        if (operatorPressed) {
            // Clear input on new operator sequence
            binding.display.text = ""
            operatorPressed = false
        }

        input += value
        binding.display.text = input
    }

    // Append operator and ensure no duplicate operators
    private fun appendOperator(operator: String) {
        if (input.isNotEmpty() && !operatorPressed) {
            input += " $operator "
            binding.display.text = input
            operatorPressed = true
        }
    }

    // Function to calculate the result when '=' is pressed
    private fun calculateResult() {
        try {
            // Basic evaluation of the input string
            val tokens = input.split(" ")

            // Create a mutable list to handle the calculation
            val values = mutableListOf<Double>()
            val operators = mutableListOf<String>()

            // Parse the tokens
            for (token in tokens) {
                when {
                    token.toDoubleOrNull() != null -> {
                        // If it's a number, add it to the values list
                        values.add(token.toDouble())
                    }
                    token in listOf("+", "-", "*", "/", "%") -> {
                        // If it's an operator, add it to the operators list
                        operators.add(token)
                    }
                }
            }

            // Perform calculations based on operators and values
            var result = values[0]

            for (i in operators.indices) {
                val operator = operators[i]
                val operand2 = values[i + 1]
                result = when (operator) {
                    "+" -> result + operand2
                    "-" -> result - operand2
                    "*" -> result * operand2
                    "/" -> if (operand2 != 0.0) result / operand2 else throw ArithmeticException("Division by zero")
                    "%" -> result % operand2
                    else -> throw IllegalArgumentException("Invalid Operator")
                }
            }

            // Set the result to the display
            binding.display.text = result.toString()
            input = result.toString() // Store result for further operations
        } catch (e: Exception) {
            Toast.makeText(this, "Error in calculation : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // Function to clear input and reset the calculator
    private fun clearInput() {
        input = ""
        binding.display.text = "0"
        operatorPressed = false
    }

    // Function to delete the last character from the input
    private fun deleteLastChar() {
        if (input.isNotEmpty()) {
            input = input.dropLast(1)
            binding.display.text = if (input.isEmpty()) "0" else input
        }
    }
}
