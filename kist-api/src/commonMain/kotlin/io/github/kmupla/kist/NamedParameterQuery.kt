package io.github.kmupla.kist

/**
 * Holds the result of parsing a named-parameter SQL query.
 *
 * @property sql The rewritten SQL with all `:name` placeholders replaced by `?`.
 * @property paramNames The ordered list of parameter names as they appear in the original SQL.
 *   A name may appear more than once if the same parameter is referenced multiple times.
 */
data class ParsedNamedQuery(
    val sql: String,
    val paramNames: List<String>,
)

/**
 * Parses a SQL query that may contain named parameters in the form `:name`.
 *
 * Rules:
 * - A named token is `:` followed by one or more word characters (`[a-zA-Z0-9_]`).
 * - Tokens inside single-quoted string literals are ignored (e.g. `':notAParam'`).
 * - Mixing positional `?` and named `:param` placeholders in the same query is not allowed.
 *
 * If the query contains no `:name` tokens the original SQL is returned unchanged with an
 * empty [ParsedNamedQuery.paramNames] list, indicating positional mode.
 */
object NamedParameterQuery {

    private val NAMED_PARAM_REGEX = Regex(""":([a-zA-Z][a-zA-Z0-9_]*)""")

    /**
     * Returns `true` when [sql] contains at least one `:name` style placeholder outside
     * of a string literal.
     */
    fun isNamed(sql: String): Boolean = NAMED_PARAM_REGEX.containsMatchIn(stripStringLiterals(sql))

    /**
     * Parses [sql] and returns a [ParsedNamedQuery].
     *
     * @throws IllegalArgumentException if the query mixes `?` and `:name` placeholders.
     */
    fun parse(sql: String): ParsedNamedQuery {
        val stripped = stripStringLiterals(sql)

        val hasPositional = stripped.contains('?')
        val hasNamed = NAMED_PARAM_REGEX.containsMatchIn(stripped)

        require(!(hasPositional && hasNamed)) {
            "Query mixes positional '?' and named ':param' placeholders, which is not allowed: $sql"
        }

        if (!hasNamed) {
            return ParsedNamedQuery(sql = sql, paramNames = emptyList())
        }

        // Replace :name tokens only outside of string literals, preserving literal content.
        val paramNames = mutableListOf<String>()
        val rewritten = replaceNamedParams(sql, paramNames)

        return ParsedNamedQuery(sql = rewritten, paramNames = paramNames)
    }

    /**
     * Given a [ParsedNamedQuery] and a [namedParams] map, returns the values in the order
     * they must be bound (i.e. the order in which the names appear in the query).
     *
     * @throws IllegalArgumentException if a name referenced in the query is absent from [namedParams].
     */
    fun orderedValues(parsed: ParsedNamedQuery, namedParams: Map<String, Any?>): List<Any?> {
        return parsed.paramNames.map { name ->
            require(namedParams.containsKey(name)) {
                "Named parameter ':$name' referenced in query is not present in the provided parameter map. " +
                    "Available keys: ${namedParams.keys}"
            }
            namedParams[name]
        }
    }

    /**
     * Scans [sql] character-by-character, replacing `:name` tokens that appear *outside*
     * single-quoted string literals with `?`, and appending each captured name to [outNames].
     * Literal content is copied verbatim so the rewritten SQL remains valid.
     */
    private fun replaceNamedParams(sql: String, outNames: MutableList<String>): String {
        val sb = StringBuilder(sql.length)
        var inLiteral = false
        var i = 0
        while (i < sql.length) {
            val ch = sql[i]
            when {
                ch == '\'' && !inLiteral -> {
                    inLiteral = true
                    sb.append(ch)
                    i++
                }
                ch == '\'' && inLiteral -> {
                    sb.append(ch)
                    i++
                    // handle escaped quote ''
                    if (i < sql.length && sql[i] == '\'') {
                        sb.append(sql[i])
                        i++
                    } else {
                        inLiteral = false
                    }
                }
                !inLiteral && ch == ':' -> {
                    // Try to read an identifier after the colon
                    val start = i + 1
                    var end = start
                    if (end < sql.length && sql[end].isLetter()) {
                        end++
                        while (end < sql.length && (sql[end].isLetterOrDigit() || sql[end] == '_')) {
                            end++
                        }
                        outNames.add(sql.substring(start, end))
                        sb.append('?')
                        i = end
                    } else {
                        sb.append(ch)
                        i++
                    }
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /**
     * Replaces single-quoted string literals with same-length whitespace so that `:tokens`
     * inside literal values are not mistaken for named parameters.
     */
    private fun stripStringLiterals(sql: String): String {
        val sb = StringBuilder(sql.length)
        var inLiteral = false
        var i = 0
        while (i < sql.length) {
            val ch = sql[i]
            when {
                ch == '\'' && !inLiteral -> {
                    inLiteral = true
                    sb.append(' ')
                }
                ch == '\'' && inLiteral -> {
                    // handle escaped quote ''
                    if (i + 1 < sql.length && sql[i + 1] == '\'') {
                        sb.append("  ")
                        i += 2
                        continue
                    }
                    inLiteral = false
                    sb.append(' ')
                }
                inLiteral -> sb.append(' ')
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }
}
