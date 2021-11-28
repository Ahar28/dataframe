package org.jetbrains.kotlinx.dataframe.jupyter

import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.jetbrains.kotlinx.jupyter.testkit.ReplProvider
import org.jetbrains.kotlinx.jupyter.testkit.notebook.JupyterCell

abstract class DataFrameJupyterTest : JupyterReplTestCase(
    ReplProvider.forLibrariesTesting(listOf("dataframe"))
)

fun interface CodeReplacer {
    fun replace(code: String): String

    companion object {
        val DEFAULT = CodeReplacer { it }

        fun byMap(replacements: Map<String, String>) = CodeReplacer { code ->
            replacements.entries.fold(code) { acc, (key, replacement) ->
                acc.replace(key, replacement)
            }
        }

        fun byMap(vararg replacements: Pair<String, String>): CodeReplacer = byMap(mapOf(*replacements))
    }
}

fun interface CellClause {
    fun isAccepted(cell: JupyterCell): Boolean

    companion object {
        val IS_CODE = CellClause { it.cell_type == "code" }
    }
}

infix fun CellClause.and(other: CellClause): CellClause {
    return CellClause { cell ->
        // Prevent lazy evaluation
        val acceptedThis = this.isAccepted(cell)
        val acceptedOther = other.isAccepted(cell)
        acceptedThis && acceptedOther
    }
}

fun CellClause.Companion.stopAfter(breakClause: CellClause) = object : CellClause {
    var clauseTriggered: Boolean = false

    override fun isAccepted(cell: JupyterCell): Boolean {
        clauseTriggered = clauseTriggered || breakClause.isAccepted(cell)
        return !clauseTriggered
    }
}
