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
import android.graphics.Color
import android.os.Build
import android.view.WindowManager

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
     * Called when the activity is first created
     * Sets up UI components and makes status bar transparent
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        makeStatusBarTransparent()

        setContentView(R.layout.activity_main)

        setupUIComponents()
        setupAllButtonClickHandlers()
        refreshDisplay()
    }

    /**
     * Makes the status bar transparent for modern Android versions
     */
    private fun makeStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    /**
     * Initialize UI components by finding them in the layout
     */
    private fun setupUIComponents() {
        expressionDisplay = findViewById(R.id.Text)
        resultDisplay = findViewById(R.id.resultTextView)
    }

    /**
     * Set up click handlers for all calculator buttons
     */
    private fun setupAllButtonClickHandlers() {
        findViewById<AppCompatButton>(R.id.zero).setOnClickListener {
            handleNumberInput("0")
        }
        findViewById<AppCompatButton>(R.id.one).setOnClickListener {
            handleNumberInput("1")
        }
        findViewById<AppCompatButton>(R.id.two).setOnClickListener {
            handleNumberInput("2")
        }
        findViewById<AppCompatButton>(R.id.three).setOnClickListener {
            handleNumberInput("3")
        }
        findViewById<AppCompatButton>(R.id.four).setOnClickListener {
            handleNumberInput("4")
        }
        findViewById<AppCompatButton>(R.id.five).setOnClickListener {
            handleNumberInput("5")
        }
        findViewById<AppCompatButton>(R.id.six).setOnClickListener {
            handleNumberInput("6")
        }
        findViewById<AppCompatButton>(R.id.seven).setOnClickListener {
            handleNumberInput("7")
        }
        findViewById<AppCompatButton>(R.id.eight).setOnClickListener {
            handleNumberInput("8")
        }
        findViewById<AppCompatButton>(R.id.nine).setOnClickListener {
            handleNumberInput("9")
        }

        findViewById<AppCompatButton>(R.id.add).setOnClickListener {
            handleOperatorInput("+")
        }
        findViewById<AppCompatButton>(R.id.sub).setOnClickListener {
            handleOperatorInput("-")
        }
        findViewById<AppCompatButton>(R.id.mul).setOnClickListener {
            handleOperatorInput("×")
        }
        findViewById<AppCompatButton>(R.id.div).setOnClickListener {
            handleOperatorInput("/")
        }

        findViewById<AppCompatButton>(R.id.divide).setOnClickListener {
            handlePercentagePress()
        }

        findViewById<AppCompatButton>(R.id.equals).setOnClickListener {
            handleEqualsPress()
        }
        findViewById<AppCompatButton>(R.id.Ac).setOnClickListener {
            handleAllClearPress()
        }
        findViewById<ImageButton>(R.id.back).setOnClickListener {
            handleBackspacePress()
        }
        findViewById<AppCompatButton>(R.id.dot).setOnClickListener {
            handleDecimalPointPress()
        }
        findViewById<AppCompatButton>(R.id.plusMinus).setOnClickListener {
            handlePlusMinusPress()
        }
    }

    /**
     * Handle number input (0-9)
     * @param digit The digit to add to current input
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
     * Handle operator input (+, -, ×, /)
     * @param operator The operator to add to expression
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

        if (currentInputNumber.isNotEmpty() ||
            (currentExpression.isNotEmpty() && operatorsInExpression.isNotEmpty())
        ) {

            if (currentInputNumber.isNotEmpty()) {
                numbersInExpression.add(currentInputNumber.toDouble())

                if (currentExpression.isEmpty()) {
                    currentExpression = currentInputNumber
                } else {
                    currentExpression += " $currentInputNumber"
                }
            } else if (operatorsInExpression.isNotEmpty()) {
                operatorsInExpression[operatorsInExpression.size - 1] = operator
                currentExpression = currentExpression
                    .dropLastWhile { it != ' ' }
                    .dropLast(1) + " $operator"
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
     * Handle percentage calculation
     * Converts current number to percentage (divides by 100)
     */
    private fun handlePercentagePress() {
        if (isErrorState()) {
            handleAllClearPress()
        }

        if (completedExpressionToShow.isNotEmpty() && isStartingNewOperation) {
            completedExpressionToShow = ""
            resetCalculationState()
        }

        if (currentInputNumber.isNotEmpty() && currentInputNumber != "0") {
            try {
                val currentValue = currentInputNumber.toDouble()
                val percentageValue = currentValue / 100.0
                currentInputNumber = formatNumberForDisplay(percentageValue)
                hasDecimalPoint = currentInputNumber.contains(".")
                refreshDisplay()
            } catch (e: NumberFormatException) {
                currentInputNumber = "Error"
                refreshDisplay()
            }
        }
    }

    /**
     * Handle equals button press - perform calculation
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
                val resultDigitCount = formattedResult
                    .replace(".", "")
                    .replace("-", "")
                    .replace("E", "")
                    .length

                if (resultDigitCount > MAX_DIGITS_IN_RESULT ||
                    calculationResult.isInfinite() ||
                    calculationResult.isNaN()
                ) {

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
     * Perform mathematical calculation with proper operator precedence
     * @param numbers List of numbers in the expression
     * @param operators List of operators in the expression
     * @return The calculated result
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
     * Reset calculation state to initial values
     */
    private fun resetCalculationState() {
        currentExpression = ""
        numbersInExpression.clear()
        operatorsInExpression.clear()
        isStartingNewOperation = true
        hasDecimalPoint = false
    }

    /**
     * Handle All Clear (AC) button press
     */
    private fun handleAllClearPress() {
        currentInputNumber = ""
        completedExpressionToShow = ""
        resetCalculationState()
        lastCalculationResult = 0.0
        refreshDisplay()
    }

    /**
     * Handle backspace button press
     * Supports navigation through calculation history
     */
    private fun handleBackspacePress() {
        if (calculationHistory.isNotEmpty() &&
            currentHistoryPosition >= 0 &&
            (currentInputNumber.isEmpty() ||
                    currentInputNumber == "0" ||
                    isStartingNewOperation)
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

        if (currentInputNumber.isNotEmpty() &&
            !isStartingNewOperation &&
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
     * Handle decimal point button press
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

            val digitCount = potentialInput
                .replace(".", "")
                .replace("-", "")
                .length

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
     * Handle plus/minus toggle button press
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
     * Refresh the display with current calculation state
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
     * Adjust text sizes based on content length
     * @param expressionText The expression text to check
     * @param resultText The result text to check
     */
    private fun adjustTextSizesForLength(expressionText: String, resultText: String) {
        val shouldShrinkExpression = expressionText.length > 30 ||
                expressionText.split(" ").size > 6
        val shouldShrinkResult = resultText.length > 8

        val targetExpressionSize = if (shouldShrinkExpression) {
            SMALL_EXPRESSION_TEXT_SIZE
        } else {
            NORMAL_EXPRESSION_TEXT_SIZE
        }

        val targetResultSize = if (shouldShrinkResult) {
            SMALL_RESULT_TEXT_SIZE
        } else {
            NORMAL_RESULT_TEXT_SIZE
        }

        val currentExpressionSize = expressionDisplay.textSize /
                resources.displayMetrics.scaledDensity
        if (currentExpressionSize != targetExpressionSize) {
            animateTextSizeChange(
                expressionDisplay,
                currentExpressionSize,
                targetExpressionSize
            )
        }

        val currentResultSize = resultDisplay.textSize /
                resources.displayMetrics.scaledDensity
        if (currentResultSize != targetResultSize) {
            animateTextSizeChange(
                resultDisplay,
                currentResultSize,
                targetResultSize
            )
        }
    }

    /**
     * Animate text size change for smooth transitions
     * @param textView The TextView to animate
     * @param fromSize Starting text size
     * @param toSize Target text size
     */
    private fun animateTextSizeChange(
        textView: TextView,
        fromSize: Float,
        toSize: Float
    ) {
        val animator = ValueAnimator.ofFloat(fromSize, toSize)
        animator.duration = 200

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, animatedValue)
        }

        animator.start()
    }

    /**
     * Format number for display with appropriate precision
     * @param value The number to format
     * @return Formatted string representation
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
                if (formatted
                        .replace(".", "")
                        .replace("-", "")
                        .length <= MAX_DIGITS_IN_RESULT
                ) {
                    formatted
                } else {
                    String.format("%.2E", value)
                }
            }
        }
    }

    /**
     * Check if calculator is in error state
     * @return true if in error state, false otherwise
     */
    private fun isErrorState(): Boolean {
        return currentInputNumber == "Error" ||
                currentInputNumber == "Cannot divide by zero" ||
                currentInputNumber == "Number too large"
    }
}