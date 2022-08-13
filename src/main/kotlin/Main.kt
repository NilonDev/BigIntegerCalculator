import java.math.BigInteger

fun main() {
    Calculator.calculation()
}

object Calculator {

    private val listVariable = mutableMapOf<String, BigInteger>()

    public fun calculation() {
        var expression = ""

        while (expression != Command.exit) {
            expression = readln()

            try {
                when {
                    expression.isEmpty() -> continue
                    expression.contains("=") -> createVariable(expression)
                    expression.first() == '/' -> println(Command.command(expression))
                    else -> println(CalculatingRPN(expression))
                }
            } catch (exception: InvalidExpression) {
                println(exception.message)
            } catch (exception: UnknownCommand) {
                println(exception.message)
            } catch (exception: ErrorVariable) {
                println(exception.message)
            }
        }
    }

    private fun CalculatingRPN(postfix: String): BigInteger {
        val validPostfix = convertToPostfix(postfix)

        //println("postfix(CalculatingRPN): $validPostfix")

        val stack: ArrayDeque<BigInteger> = ArrayDeque()
        val digit = Regex("([-+]?[0-9]+)")

        val list = validPostfix.split(" ")

        when {
            list.size == 1 ->  return list.first().toBigInteger()
            postfix.matches("[-+]?[0-9]+[-+]+".toRegex()) -> throw InvalidExpression("Invalid expression")
        }

        for (elem in list) {
            if (elem.matches(digit)) {
                stack.addLast(elem.toBigInteger())
            } else {
                val second = stack.removeLast()
                val first = stack.removeLast()

                when(elem) {
                    "-" -> stack.addLast(first - second)
                    "+" -> stack.addLast(first + second)
                    "/" -> stack.addLast(first / second)
                    "*" -> stack.addLast(first * second)
                    "^" -> stack.addLast(first.pow(second.toInt()))
                }
            }
        }
        return if (stack.isNotEmpty()) stack.last() else BigInteger.ZERO
    }

    private fun convertToPostfix(infix: String): String {
        val validInfix = convertExpression(infix)
        var postfix = ""
        val stack: ArrayDeque<Sign> = ArrayDeque()

        val digit = Regex("([-+]?[0-9]+)")
        val superRegex = Regex("((?<=[-+*/(])-\\d+|^(-\\d+)|[-+/*()])|[0-9]+")

        val list = superRegex.findAll(validInfix).toList().map { it.value }

        //println("convertToPostfix: $list")

        for (elem in list) {
            if (elem.matches(digit)) {
                postfix += "$elem "
            }
            else {
                val sign = Sign(elem.first())

                when {
                    stack.isEmpty() -> stack.addLast(sign)
                    stack.last().sign == '(' -> stack.addLast(sign)

                    sign.sign == '(' -> stack.addLast(sign)

                    sign.sign == ')' -> {
                        try {
                            while (stack.last().sign != '(') {
                                postfix += "${stack.last().sign} "
                                stack.removeLast()
                            }
                            stack.removeLast()
                        } catch (exception: java.util.NoSuchElementException) {
                            throw InvalidExpression("Invalid expression")
                        }
                    }

                    sign.priority > stack.last().priority -> stack.addLast(sign)

                    sign.priority <= stack.last().priority -> {
                        while (stack.isNotEmpty() && sign.priority <= stack.last().priority && stack.last().sign != '(') {
                            postfix += "${stack.last().sign} "
                            stack.removeLast()
                        }
                        stack.addLast(sign)
                    }
                }
            }
        }

        stack.forEach {if (it.sign == '(' || it.sign == ')') throw InvalidExpression("Invalid expression")}

        while (stack.isNotEmpty()) {
            postfix += "${stack.last().sign} "
            stack.removeLast()
        }

        postfix = postfix.trim()
        return postfix
    }

    private fun convertExpression(expression: String): String {
        val spaceReg = Regex("\\s+")
        val badVal = Regex("([a-zA-Z]+[0-9]+|[0-9]+[a-zA-Z]+)")
        val badMul = Regex("/{2,}")
        val badDiv = Regex("\\*{2,}")
        val signConv = Regex("[-+]{2,}")
        val correctVariable = Regex("[a-zA-Z]")

        // Delete space
        var newExpression = expression.replace(spaceReg, "")
        //println(newExpression)

        // convert "+++" to "+" or "+---" to "-"
        while (signConv.containsMatchIn(newExpression)) {
            val repls = signConv.find(newExpression)?.value.toString()
            when {
                '-' !in repls -> newExpression = newExpression.replaceFirst(signConv, "+")
                repls.count {it == '-'} % 2 == 0 -> newExpression = newExpression.replaceFirst(signConv, "+")
                repls.count {it == '-'} % 2 != 0 -> newExpression = newExpression.replaceFirst(signConv, "-")
            }
        }

        // convert variable to value ( a = 5.   1 + 2 + a -> 1 + 2 + 5)
        while (correctVariable.containsMatchIn(newExpression)) {
            val variable = correctVariable.find(newExpression)!!.value.toString()
            when {
                !listVariable.containsKey(variable) -> throw ErrorVariable("Unknown variable")
                else -> newExpression = newExpression.replaceFirst(correctVariable, listVariable[variable].toString())
            }
        }

        when {
            badVal.containsMatchIn(newExpression) -> throw ErrorVariable("Invalid identifier")
            badMul.containsMatchIn(newExpression) -> throw InvalidExpression("Invalid expression")
            badDiv.containsMatchIn(newExpression) -> throw InvalidExpression("Invalid expression")
        }
        //println("convertExpression: $newExpression")
        return newExpression
    }

    private fun createVariable(expression: String) {
        val spaceReg = Regex("\\s+")
        val digitReg = Regex("-?\\d+(\\.\\d+)?")
        val validKeyReg = Regex("[a-zA-Z]+")
        val validValueReg = Regex("([a-zA-Z]+|[-+]?[0-9]+)")
        val someEquals = Regex("=.*=")

        val newExpression = expression.replace(spaceReg, "")

        val (key, value) = newExpression.split("=")      // Левая и правая часть объявления переменной
        val valueIsDigit = value.matches(digitReg)                // значение - цифра

        when {
            !key.matches(validKeyReg) -> throw ErrorVariable("Invalid identifier")                      // неверное имя
            !value.matches(validValueReg) -> throw ErrorVariable("Invalid assignment")                  // неверное значение
            !valueIsDigit && !listVariable.containsKey(value) -> throw ErrorVariable("Unknown variable")      // значение переменная, котрой нет
            newExpression.matches(someEquals) ->  throw ErrorVariable("Invalid assignment")             // 2 или больше равно
        }

        if (!valueIsDigit) {
            listVariable[key] = listVariable.getOrDefault(value, BigInteger.ZERO)
        } else {
            listVariable[key] = value.toBigInteger()
        }
    }
}

class Command {
    companion object {
        const val exit = "/exit"
        const val help = "/help"
        val helpText =
            """
            Smart calculator version 0.0.1.
            Supports operations with large INTEGER numbers.
            Available operations: [ + - * / ^ ( ) ].
            The calculator supports working with variables.
            Variable names must match the pattern [a-zA-z].
            Variable declaration example:
            ">" is an input character, do not enter it when using a calculator 
            > a = 25
            > b = a
            > a
            25
            > b
            25
            
            Example of writing an expression:
            
            > a = 2
            > b = 4
            > 5 + 2 * (b - a)
            9
            
            > 2+2^2
            6
            """.trimIndent()

        fun command(expression: String): String {
            return when (expression.trim()) {
                exit -> "Bye!"
                help -> helpText
                else -> throw UnknownCommand("Unknown command")
            }
        }
    }
}

class Sign(val sign: Char) {
    val priority = when(sign) {
        '+', '-' -> 0
        '*', '/' -> 1
        '^' -> 2
        '(', ')' -> 3
        else -> -1
    }
}

class InvalidExpression(message: String) : Exception(message)
class UnknownCommand(message: String) : Exception(message)
class ErrorVariable(message: String) : Exception(message)