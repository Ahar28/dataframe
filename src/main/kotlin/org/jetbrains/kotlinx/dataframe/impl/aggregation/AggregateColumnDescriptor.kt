package org.jetbrains.kotlinx.dataframe.impl.aggregation

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.aggregation.AggregateColumnsSelector
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.columns.UnresolvedColumnsPolicy
import org.jetbrains.kotlinx.dataframe.columns.shortPath
import org.jetbrains.kotlinx.dataframe.impl.aggregation.receivers.AggregateReceiverInternal
import org.jetbrains.kotlinx.dataframe.impl.columns.resolve

internal class AggregateColumnDescriptor<C>(
    val column: ColumnWithPath<C>,
    val default: C? = null,
    val newPath: ColumnPath? = null
) : ColumnWithPath<C> by column

internal fun <T, C> DataFrame<T>.getAggregateColumn(selector: AggregateColumnsSelector<T, C>) =
    getAggregateColumns(selector).single()

internal fun <T, C> DataFrame<T>.getAggregateColumns(selector: AggregateColumnsSelector<T, C>): List<AggregateColumnDescriptor<C>> {
    val columns = selector.toColumns().resolve(this, UnresolvedColumnsPolicy.Create)
    return columns.map {
        when (val col = it) {
            is AggregateColumnDescriptor<*> -> col as AggregateColumnDescriptor<C>
            else -> AggregateColumnDescriptor(it, null, null)
        }
    }
}

internal fun <T, C> AggregateReceiverInternal<T>.getPath(col: AggregateColumnDescriptor<C>, isSingle: Boolean) =
    col.newPath ?: if (isSingle) pathForSingleColumn(col.data) else col.data.shortPath()
