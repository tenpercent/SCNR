package com.scnr

import com.scnr.CharChecker.isControl
import com.scnr.CharChecker.isInvalid
import com.scnr.CharChecker.isPunctuation
import com.scnr.CharChecker.isWhitespace

// converted from https://github.com/tensorflow/examples/blob/master/lite/examples/bert_qa/android/app/src/main/java/org/tensorflow/lite/examples/bertqa/tokenization

internal object CharChecker {
    /** To judge whether it's an empty or unknown character.  */
    fun isInvalid(ch: Char): Boolean {
        return ch.toInt() == 0 || ch.toInt() == 0xfffd
    }

    /** To judge whether it's a control character(exclude whitespace).  */
    fun isControl(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return false
        }
        val type = Character.getType(ch)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    /** To judge whether it can be regarded as a whitespace.  */
    fun isWhitespace(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return true
        }
        val type = Character.getType(ch)
        return type == Character.SPACE_SEPARATOR.toInt() || type == Character.LINE_SEPARATOR.toInt() || type == Character.PARAGRAPH_SEPARATOR.toInt()
    }

    /** To judge whether it's a punctuation.  */
    fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() || type == Character.DASH_PUNCTUATION.toInt() || type == Character.START_PUNCTUATION.toInt() || type == Character.END_PUNCTUATION.toInt() || type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() || type == Character.FINAL_QUOTE_PUNCTUATION.toInt() || type == Character.OTHER_PUNCTUATION.toInt()
    }
}

class BasicTokenizer(private val doLowerCase: Boolean) {
    fun tokenize(text: String?): List<String> {
        val cleanedText = cleanText(text)
        val origTokens =
            whitespaceTokenize(cleanedText)
        val stringBuilder = StringBuilder()
        val tks = if (doLowerCase) {
            origTokens.map (String::toLowerCase)
        } else origTokens

        for (token in tks) {
            val list =
                runSplitOnPunc(token)
            for (subToken in list) {
                stringBuilder.append(subToken).append(" ")
            }
        }
        return whitespaceTokenize(stringBuilder.toString())
    }

    companion object {
        /* Performs invalid character removal and whitespace cleanup on text. */
        fun cleanText(text: String?): String {
            if (text == null) {
                throw NullPointerException("The input String is null.")
            }
            val stringBuilder = StringBuilder("")
            for (ch in text) {
                // Skip the characters that cannot be used.
                if (isInvalid(ch) || isControl(ch)) {
                    continue
                }
                if (isWhitespace(ch)) {
                    stringBuilder.append(" ")
                } else {
                    stringBuilder.append(ch)
                }
            }
            return stringBuilder.toString()
        }

        /* Runs basic whitespace cleaning and splitting on a piece of text. */
        fun whitespaceTokenize(text: String): List<String> {
            return text.split(" ")
        }

        /* Splits punctuation on a piece of text. */
        fun runSplitOnPunc(text: String): List<String> {
            val tokens: MutableList<String> = ArrayList()
            var startNewWord = true
            for (ch in text) {
                if (isPunctuation(ch)) {
                    tokens.add(ch.toString())
                    startNewWord = true
                } else {
                    if (startNewWord) {
                        tokens.add("")
                        startNewWord = false
                    }
                    tokens[tokens.size - 1] = tokens.last() + ch
                }
            }
            return tokens
        }
    }
}

class WordpieceTokenizer(private val dic: Map<String, Int>) {

    /**
     * Tokenizes a piece of text into its word pieces. This uses a greedy longest-match-first
     * algorithm to perform tokenization using the given vocabulary. For example: input = "unaffable",
     * output = ["un", "##aff", "##able"].
     *
     * @param text: A single token or whitespace separated tokens. This should have already been
     * passed through `BasicTokenizer.
     * @return A list of wordpiece tokens.
     */
    fun tokenize(text: String): List<String> {
        val outputTokens: MutableList<String> = ArrayList()
        for (token in BasicTokenizer.whitespaceTokenize(text)) {
            if (token.length > MAX_INPUTCHARS_PER_WORD) {
                outputTokens.add(UNKNOWN_TOKEN)
                continue
            }
            var isBad =
                false // Mark if a word cannot be tokenized into known subwords.
            var start = 0
            val subTokens: MutableList<String> = ArrayList()
            while (start < token.length) {
                var curSubStr = ""
                var end = token.length // Longer substring matches first.
                while (start < end) {
                    val subStr =
                        if (start == 0) token.substring(start, end) else "##" + token.substring(
                            start,
                            end
                        )
                    if (dic.containsKey(subStr)) {
                        curSubStr = subStr
                        break
                    }
                    end--
                }

                // The word doesn't contain any known subwords.
                if ("" == curSubStr) {
                    isBad = true
                    break
                }

                // curSubStr is the longeset subword that can be found.
                subTokens.add(curSubStr)

                // Proceed to tokenize the resident string.
                start = end
            }
            if (isBad) {
                outputTokens.add(UNKNOWN_TOKEN)
            } else {
                outputTokens.addAll(subTokens)
            }
        }
        return outputTokens
    }

    companion object {
        private const val UNKNOWN_TOKEN = "[UNK]" // For unknown words.
        private const val MAX_INPUTCHARS_PER_WORD = 200
    }
}

class FullTokenizer(
    private val dic: Map<String, Int>,
    doLowerCase: Boolean
) {
    private val basicTokenizer: BasicTokenizer = BasicTokenizer(doLowerCase)
    private val wordpieceTokenizer: WordpieceTokenizer = WordpieceTokenizer(dic)

    fun tokenize(text: String): List<String> = basicTokenizer.tokenize(text).map { wordpieceTokenizer.tokenize(it) }.flatten()

    fun convertTokensToIds(tokens: List<String>): List<Int> = tokens.map { dic[it] ?: error("failed to retrieve $it") }
}