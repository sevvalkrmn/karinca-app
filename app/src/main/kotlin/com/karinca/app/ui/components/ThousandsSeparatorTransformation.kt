package com.karinca.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Keeps only digits and a single ',' decimal separator — letters, spaces, extra commas,
// or pasted non-numeric text are dropped before they ever reach a ViewModel's state.
// Call this from onValueChange, not the VisualTransformation below (that only changes
// how existing digits are *displayed*, it can't stop non-digit characters from being
// typed in the first place).
fun filterAmountInput(text: String): String {
    val sb = StringBuilder()
    var seenComma = false
    for (c in text) {
        when {
            c.isDigit() -> sb.append(c)
            c == ',' && !seenComma -> {
                sb.append(c)
                seenComma = true
            }
        }
    }
    return sb.toString()
}

// Groups the integer part of a typed amount with "." every 3 digits (10000 -> 10.000)
// purely for display — the underlying stored text (what onValueChange receives) is
// untouched, so parsing (replace(',', '.').toDoubleOrNull()) elsewhere never has to
// know about this. Anything from the first non-digit character onward (a "," decimal
// separator) is passed through unformatted.
object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val separatorIndex = original.indexOfFirst { !it.isDigit() }
        val integerPart = if (separatorIndex >= 0) original.substring(0, separatorIndex) else original
        val rest = if (separatorIndex >= 0) original.substring(separatorIndex) else ""

        val len = integerPart.length
        val grouped = groupThousands(integerPart)
        val transformedText = grouped + rest

        // Precompute the transformed position for every possible cursor offset within
        // the integer part (0..len) once, rather than deriving a closed-form inverse.
        val toTransformed = IntArray(len + 1) { i -> i + dotsBefore(i, len) }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= len) toTransformed[offset] else toTransformed[len] + (offset - len)

            override fun transformedToOriginal(offset: Int): Int {
                if (offset > grouped.length) return len + (offset - grouped.length)
                var result = 0
                for (i in 0..len) {
                    if (toTransformed[i] <= offset) result = i else break
                }
                return result
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}

private fun groupThousands(digits: String): String {
    if (digits.isEmpty()) return digits
    val sb = StringBuilder()
    val len = digits.length
    for (i in digits.indices) {
        if (i > 0 && (len - i) % 3 == 0) sb.append('.')
        sb.append(digits[i])
    }
    return sb.toString()
}

// How many "." separators land at or before original digit index i, once grouped.
// groupThousands() only ever inserts a dot for loop index j in [1, len - 1] (it never
// considers j == len), so j must be capped there too — otherwise this overcounts by one
// dot right at the cursor-at-end position (i == len).
private fun dotsBefore(i: Int, len: Int): Int {
    var count = 0
    for (j in 1..minOf(i, len - 1)) {
        if ((len - j) % 3 == 0) count++
    }
    return count
}
