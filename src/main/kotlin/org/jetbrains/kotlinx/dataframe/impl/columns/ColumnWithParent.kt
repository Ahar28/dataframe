package org.jetbrains.kotlinx.dataframe.impl.columns

import org.jetbrains.kotlinx.dataframe.MapColumnReference
import org.jetbrains.kotlinx.dataframe.columns.BaseColumn
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.columns.ColumnResolutionContext
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.impl.emptyPath
import org.jetbrains.kotlinx.dataframe.pathOf

internal interface ColumnWithParent<out C> : ColumnReference<C> {

    val parent: MapColumnReference?

    val source: BaseColumn<C>

    override fun path() = parent?.path()?.plus(name()) ?: pathOf(name())

    override fun resolveSingle(context: ColumnResolutionContext): ColumnWithPath<C>? {
        val parentDef = parent
        val (targetDf, pathPrefix) = when (parentDef) {
            null -> context.df to emptyPath()
            else -> {
                val parentCol = parentDef.resolveSingle(context) ?: return null
                val group = parentCol.data.asGroup()
                group.df to parentCol.path
            }
        }

        val data = targetDf.getColumn<C>(name(), context.unresolvedColumnsPolicy)
        return data?.addPath(pathPrefix + name(), context.df)
    }
}
