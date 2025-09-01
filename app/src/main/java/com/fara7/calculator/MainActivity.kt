package com.fara7.calculator

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.widget.ImageButton
import java.text.DecimalFormat
import java.util.*
import android.animation.ValueAnimator
import android.util.TypedValue

class MainActivity : AppCompatActivity() {

    private lateinit var expressionDisplay: TextView
    private lateinit var resultDisplay: TextView

    private var currentInputNumber = ""
    private var currentExpression = ""
    private var numbersInExpression = mutableListOf<Double>()
    private var operatorsInExpression = mutableListOf<String>()
    private var isStartingNewOperation = true
    private var hasDecimalPoint = false
    private var lastCalculationResult = 0.0
    private var completedExpressionToShow = ""

    private var calculationHistory = mutableListOf<String>()
    private var resultHistory = mutableListOf<String>()
    private var currentHistoryPosition = -1

    private val MAX_DIGITS_PER_NUMBER = 7
    private val MAX_DIGITS_IN_RESULT = 10
    private val MAX_CHAINED_OPERATIONS = 7

    private val NORMAL_EXPRESSION_TEXT_SIZE = 20f
    private val SMALL_EXPRESSION_TEXT_SIZE = 16f
    private val NORMAL_RESULT_TEXT_SIZE = 36f
    private val SMALL_RESULT_TEXT_SIZE = 28f

    /**
     * onCreate - sets up the whole calculator when it starts
     * Basically initializes everything and gets the UI ready to rock
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUIComponents()
        setupAllButtonClickHandlers()
        refreshDisplay()
    }

    /**
     * setupUIComponents - finds and connects all the UI elements
     * Just grabbing references to the text views so we can update them later
     */
    private fun setupUIComponents() {
        expressionDisplay = findViewById(R.id.Text)
        resultDisplay = findViewById(R.id.resultTextView)
    }

    /**
     * setupAllButtonClickHandlers - connects all button clicks to their functions
     * This is where we tell each button what to do when someone taps it
     * Pretty straightforward - numbers call onNumberClick, operators call onOperatorClick, etc.
     */
    private fun setupAllButtonClickHandlers() {
        findViewById<AppCompatButton>(R.id.zero).setOnClickListener { handleNumberInput("0") }
        findViewById<AppCompatButton>(R.id.one).setOnClickListener { handleNumberInput("1") }
        findViewById<AppCompatButton>(R.id.two).setOnClickListener { handleNumberInput("2") }
        findViewById<AppCompatButton>(R.id.three).setOnClickListener { handleNumberInput("3") }
        findViewById<AppCompatButton>(R.id.four).setOnClickListener { handleNumberInput("4") }
        findViewById<AppCompatButton>(R.id.five).setOnClickListener { handleNumberInput("5") }
        findViewById<AppCompatButton>(R.id.six).setOnClickListener { handleNumberInput("6") }
        findViewById<AppCompatButton>(R.id.seven).setOnClickListener { handleNumberInput("7") }
        findViewById<AppCompatButton>(R.id.eight).setOnClickListener { handleNumberInput("8") }
        findViewById<AppCompatButton>(R.id.nine).setOnClickListener { handleNumberInput("9") }

        findViewById<AppCompatButton>(R.id.add).setOnClickListener { handleOperatorInput("+") }
        findViewById<AppCompatButton>(R.id.sub).setOnClickListener { handleOperatorInput("-") }
        findViewById<AppCompatButton>(R.id.mul).setOnClickListener { handleOperatorInput("×") }
        findViewById<AppCompatButton>(R.id.div).setOnClickListener { handleOperatorInput("/") }
        findViewById<AppCompatButton>(R.id.divide).setOnClickListener { handleOperatorInput("%") }

        findViewById<AppCompatButton>(R.id.equals).setOnClickListener { handleEqualsPress() }
        findViewById<AppCompatButton>(R.id.Ac).setOnClickListener { handleAllClearPress() }
        findViewById<ImageButton>(R.id.back).setOnClickListener { handleBackspacePress() }
        findViewById<AppCompatButton>(R.id.dot).setOnClickListener { handleDecimalPointPress() }
        findViewById<AppCompatButton>(R.id.plusMinus).setOnClickListener { handlePlusMinusPress() }
    }

    /**
     * handleNumberInput - processes when someone taps a number button
     * @param digit the number that was pressed (as a string)
     */
    private fun handleNumberInput(digit: String) {
        if (isErrorState()) {
            handleAllClearPress()
        }

        if (completedExpressionToShow.isNotEmpty() && isStartingNewOperation) {
            completedExpressionToShow = ""
            resetCalculationState()
        }

        val potentialNewInput = if (isStartingNewOperation) {
            digit
        } else {
            if (currentInputNumber == "0" && digit != "0") {
                digit
            } else if (currentInputNumber != "0") {
                currentInputNumber + digit
            } else {
                currentInputNumber
            }
        }

        val digitCount = potentialNewInput.replace(".", "").replace("-", "").length

        if (digitCount > MAX_DIGITS_PER_NUMBER) {
            return
        }

        if (isStartingNewOperation) {
            currentInputNumber = digit
            isStartingNewOperation = false
        } else {
            if (currentInputNumber == "0" && digit != "0") {
                currentInputNumber = digit
            } else if (currentInputNumber != "0") {
                currentInputNumber += digit
            }
        }
        refreshDisplay()
    }

    /**
     * handleOperatorInput - processes when someone taps an operator button (+, -, etc.)
     * @param operator the math symbol that was pressed
     */
    private fun handleOperatorInput(operator: String) {
        if (isErrorState()) {
            handleAllClearPress()
        }

        if (completedExpressionToShow.isNotEmpty()) {
            completedExpressionToShow = ""
            currentExpression = currentInputNumber
            numbersInExpression.clear()
            numbersInExpression.add(currentInputNumber.toDouble())
            operatorsInExpression.clear()
            operatorsInExpression.add(operator)
            currentExpression += " $operator"
            currentInputNumber = ""
            isStartingNewOperation = true
            hasDecimalPoint = false
            refreshDisplay()
            return
        }

        if (operatorsInExpression.size >= MAX_CHAINED_OPERATIONS) {
            return
        }

        if (currentInputNumber.isNotEmpty() || (currentExpression.isNotEmpty() && operatorsInExpression.isNotEmpty())) {
            if (currentInputNumber.isNotEmpty()) {
                numbersInExpression.add(currentInputNumber.toDouble())

                if (currentExpression.isEmpty()) {
                    currentExpression = currentInputNumber
                } else {
                    currentExpression += " $currentInputNumber"
                }
            } else if (operatorsInExpression.isNotEmpty()) {
                operatorsInExpression[operatorsInExpression.size - 1] = operator
                currentExpression =
                    currentExpression.dropLastWhile { it != ' ' }.dropLast(1) + " $operator"
                refreshDisplay()
                return
            }

            if (operatorsInExpression.size < MAX_CHAINED_OPERATIONS) {
                operatorsInExpression.add(operator)
                currentExpression += " $operator"
            }

            currentInputNumber = ""
            isStartingNewOperation = true
            hasDecimalPoint = false
        }

        refreshDisplay()
    }

    /**
     * handleEqualsPress - processes when someone hits the equals button
     */
    private fun handleEqualsPress() {
        if (currentInputNumber.isNotEmpty() && operatorsInExpression.isNotEmpty()) {
            numbersInExpression.add(currentInputNumber.toDouble())
            val fullExpression = "$currentExpression $currentInputNumber"

            try {
                val calculationResult = performCalculation(
                    ArrayList(numbersInExpression),
                    ArrayList(operatorsInExpression)
                )

                val formattedResult = formatNumberForDisplay(calculationResult)
                val resultDigitCount =
                    formattedResult.replace(".", "").replace("-", "").replace("E", "").length

                if (resultDigitCount > MAX_DIGITS_IN_RESULT || calculationResult.isInfinite() || calculationResult.isNaN()) {
                    completedExpressionToShow = fullExpression
                    currentInputNumber = "Number too large"
                    resetCalculationState()
                    refreshDisplay()
                    return
                }

                calculationHistory.add(fullExpression)
                resultHistory.add(formattedResult)
                currentHistoryPosition = calculationHistory.size - 1

                completedExpressionToShow = fullExpression

                currentInputNumber = formattedResult
                lastCalculationResult = calculationResult

                resetCalculationState()

            } catch (e: ArithmeticException) {
                completedExpressionToShow = fullExpression
                currentInputNumber = "Cannot divide by zero"
                resetCalculationState()
            } catch (e: Exception) {
                completedExpressionToShow = fullExpression
                currentInputNumber = "Error"
                resetCalculationState()
            }
        }

        refreshDisplay()
    }

    /**
     * performCalculation - does the actual math on the expression
     * @param numbers list of numbers in the expression
     * @param operators list of operators between the numbers
     * @return the final calculated result
     */
    private fun performCalculation(
        numbers: MutableList<Double>,
        operators: MutableList<String>
    ): Double {
        var i = 0
        while (i < operators.size) {
            when (operators[i]) {
                "×", "/", "%" -> {
                    val result = when (operators[i]) {
                        "×" -> numbers[i] * numbers[i + 1]
                        "/" -> {
                            if (numbers[i + 1] == 0.0) {
                                throw ArithmeticException("Cannot divide by zero")
                            }
                            numbers[i] / numbers[i + 1]
                        }

                        "%" -> {
                            if (numbers[i + 1] == 0.0) {
                                throw ArithmeticException("Cannot divide by zero")
                            }
                            numbers[i] % numbers[i + 1]
                        }

                        else -> numbers[i]
                    }

                    numbers[i] = result
                    numbers.removeAt(i + 1)
                    operators.removeAt(i)
                }

                else -> i++
            }
        }

        i = 0
        while (i < operators.size) {
            val result = when (operators[i]) {
                "+" -> numbers[i] + numbers[i + 1]
                "-" -> numbers[i] - numbers[i + 1]
                else -> numbers[i]
            }

            numbers[i] = result
            numbers.removeAt(i + 1)
            operators.removeAt(i)
        }

        return numbers[0]
    }

    /**
     * resetCalculationState - clears all the calculation variables for a fresh start
     */
    private fun resetCalculationState() {
        currentExpression = ""
        numbersInExpression.clear()
        operatorsInExpression.clear()
        isStartingNewOperation = true
        hasDecimalPoint = false
    }

    /**
     * handleAllClearPress - resets everything back to initial state
     */
    private fun handleAllClearPress() {
        currentInputNumber = ""
        completedExpressionToShow = ""
        resetCalculationState()
        lastCalculationResult = 0.0
        refreshDisplay()
    }

    /**
     * handleBackspacePress - handles the back button functionality
     */
    private fun handleBackspacePress() {
        if (calculationHistory.isNotEmpty() && currentHistoryPosition >= 0 &&
            (currentInputNumber.isEmpty() || currentInputNumber == "0" || isStartingNewOperation)
        ) {

            val previousExpression = calculationHistory[currentHistoryPosition]
            val previousResult = resultHistory[currentHistoryPosition]

            completedExpressionToShow = previousExpression
            currentInputNumber = previousResult

            if (currentHistoryPosition > 0) {
                currentHistoryPosition--
            }

            isStartingNewOperation = true
            hasDecimalPoint = currentInputNumber.contains(".")
            refreshDisplay()
            return
        }

        if (currentInputNumber.isNotEmpty() && !isStartingNewOperation &&
            !isErrorState()
        ) {

            if (currentInputNumber.length == 1) {
                currentInputNumber = "0"
            } else {
                val lastChar = currentInputNumber.last()
                currentInputNumber = currentInputNumber.dropLast(1)

                if (lastChar == '.') {
                    hasDecimalPoint = false
                }
            }
            refreshDisplay()
        }
    }

    /**
     * handleDecimalPointPress - adds a decimal point to the current number
     */
    private fun handleDecimalPointPress() {
        if (isErrorState()) {
            handleAllClearPress()
        }

        if (completedExpressionToShow.isNotEmpty() && isStartingNewOperation) {
            completedExpressionToShow = ""
            resetCalculationState()
        }

        if (!hasDecimalPoint) {
            val potentialInput = if (isStartingNewOperation || currentInputNumber.isEmpty()) {
                "0."
            } else {
                "$currentInputNumber."
            }

            val digitCount = potentialInput.replace(".", "").replace("-", "").length
            if (digitCount > MAX_DIGITS_PER_NUMBER) {
                return
            }

            if (isStartingNewOperation || currentInputNumber.isEmpty()) {
                currentInputNumber = "0."
                isStartingNewOperation = false
            } else {
                currentInputNumber += "."
            }
            hasDecimalPoint = true
            refreshDisplay()
        }
    }

    /**
     * handlePlusMinusPress - toggles the sign of the current number
     */
    private fun handlePlusMinusPress() {
        if (isErrorState()) {
            handleAllClearPress()
        }
        if (completedExpressionToShow.isNotEmpty() && isStartingNewOperation) {
            completedExpressionToShow = ""
            resetCalculationState()
        }

        if (currentInputNumber.isNotEmpty() && currentInputNumber != "0") {
            currentInputNumber = if (currentInputNumber.startsWith("-")) {
                currentInputNumber.substring(1)
            } else {
                "-$currentInputNumber"
            }
            refreshDisplay()
        }
    }

    /**
     * refreshDisplay - updates what the user sees on screen
     * Modified to show current expression in result area and completed expression in expression area
     */
    private fun refreshDisplay() {
        val displayResult = when {
            completedExpressionToShow.isNotEmpty() -> {
                if (currentInputNumber.isEmpty()) "0" else currentInputNumber
            }
            currentExpression.isNotEmpty() -> {
                if (currentInputNumber.isNotEmpty() && !isStartingNewOperation) {
                    "$currentExpression $currentInputNumber"
                } else {
                    currentExpression
                }
            }
            else -> if (currentInputNumber.isEmpty()) "0" else currentInputNumber
        }
        resultDisplay.text = displayResult

        val displayExpression = if (completedExpressionToShow.isNotEmpty()) {
            completedExpressionToShow
        } else {
            ""
        }
        expressionDisplay.text = displayExpression

        adjustTextSizesForLength(displayExpression, displayResult)
    }

    /**
     * adjustTextSizesForLength - makes text smaller when expressions get long
     * @param expressionText the expression text to check length of
     * @param resultText the result text to check length of
     */
    private fun adjustTextSizesForLength(expressionText: String, resultText: String) {
        val shouldShrinkExpression =
            expressionText.length > 30 || expressionText.split(" ").size > 6
        val shouldShrinkResult = resultText.length > 8

        val targetExpressionSize =
            if (shouldShrinkExpression) SMALL_EXPRESSION_TEXT_SIZE else NORMAL_EXPRESSION_TEXT_SIZE
        val targetResultSize =
            if (shouldShrinkResult) SMALL_RESULT_TEXT_SIZE else NORMAL_RESULT_TEXT_SIZE

        val currentExpressionSize =
            expressionDisplay.textSize / resources.displayMetrics.scaledDensity
        if (currentExpressionSize != targetExpressionSize) {
            animateTextSizeChange(expressionDisplay, currentExpressionSize, targetExpressionSize)
        }

        val currentResultSize = resultDisplay.textSize / resources.displayMetrics.scaledDensity
        if (currentResultSize != targetResultSize) {
            animateTextSizeChange(resultDisplay, currentResultSize, targetResultSize)
        }
    }

    /**
     * animateTextSizeChange - smoothly changes text size with animation
     * @param textView the TextView to animate
     * @param fromSize starting text size
     * @param toSize ending text size
     */
    private fun animateTextSizeChange(textView: TextView, fromSize: Float, toSize: Float) {
        val animator = ValueAnimator.ofFloat(fromSize, toSize)
        animator.duration = 200

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, animatedValue)
        }

        animator.start()
    }

    /**
     * formatNumberForDisplay - formats numbers to look nice on screen
     * @param value the number to format
     * @return formatted string representation
     */
    private fun formatNumberForDisplay(value: Double): String {
        return when {
            value.isInfinite() -> "∞"
            value.isNaN() -> "Error"
            value == value.toLong().toDouble() -> {
                val longValue = value.toLong()
                if (longValue.toString().length <= MAX_DIGITS_IN_RESULT) {
                    longValue.toString()
                } else {
                    String.format("%.2E", value)
                }
            }

            else -> {
                val formatted = DecimalFormat("#.##########").format(value)
                if (formatted.replace(".", "").replace("-", "").length <= MAX_DIGITS_IN_RESULT) {
                    formatted
                } else {
                    String.format("%.2E", value)
                }
            }
        }
    }

    /**
     * isErrorState - checks if we're currently showing an error message
     * @return true if showing error, false otherwise
     */
    private fun isErrorState(): Boolean {
        return currentInputNumber == "Error" ||
                currentInputNumber == "Cannot divide by zero" ||
                currentInputNumber == "Number too large"
    }
}