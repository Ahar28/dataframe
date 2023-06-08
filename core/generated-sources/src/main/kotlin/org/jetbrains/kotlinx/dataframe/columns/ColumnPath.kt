package org.jetbrains.kotlinx.dataframe.columns

import org.jetbrains.kotlinx.dataframe.AnyRow
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnAccessorImpl
import org.jetbrains.kotlinx.dataframe.impl.owner

/**
 * Path to a [column][DataColumn] in [DataFrame].
 *
 * Stores a list of [column names][DataColumn.name] that are used to retrieve columns through a chain of [column groups][ColumnGroup].
 */
public data class ColumnPath(val path: List<String>) : List<String> by path, ColumnAccessor<Any?> {

    public constructor(name: String) : this(listOf(name))

    public fun drop(size: Int): ColumnPath = ColumnPath(path.drop(size))

    public fun parent(): ColumnPath? = if (path.isEmpty()) null else dropLast(1)

    /**
     * Returns a shortened [ColumnPath] without the last [size] elements.
     *
     * NOTE: If called from the [ColumnsSelectionDsl], you might be looking for [ColumnsSelectionDsl.dropLastChildren]
     * instead.
     */
    public fun dropLast(size: Int = 1): ColumnPath = ColumnPath(path.dropLast(size))

    /**
     * Returns a shortened [ColumnPath] without the first [size] elements.
     *
     * NOTE: If called from the [ColumnsSelectionDsl], you might be looking for [ColumnsSelectionDsl.drop]
     * instead.
     */
    public fun dropFirst(size: Int = 1): ColumnPath = ColumnPath(path.drop(size))

    public operator fun plus(name: String): ColumnPath = ColumnPath(path + name)

    public operator fun plus(otherPath: ColumnPath): ColumnPath = ColumnPath(path + otherPath.path)

    public operator fun plus(otherPath: Iterable<String>): ColumnPath = ColumnPath(path + otherPath)

    /**
     * Returns a shortened [ColumnPath] containing just the first [first] elements.
     *
     * NOTE: If called from the [ColumnsSelectionDsl], you might be looking for [ColumnsSelectionDsl.takeChildren]
     * instead.
     */
    public fun take(first: Int): ColumnPath = ColumnPath(path.take(first))

    public fun replaceLast(name: String): ColumnPath = ColumnPath(if (path.size < 2) listOf(name) else path.dropLast(1) + name)

    /**
     * Returns a shortened [ColumnPath] containing just the last [last] elements.
     *
     * NOTE: If called from the [ColumnsSelectionDsl], you might be looking for [ColumnsSelectionDsl.takeLast]
     * instead.
     */
    public fun takeLast(last: Int): ColumnPath = ColumnPath(path.takeLast(last))

    override fun path(): ColumnPath = this

    override fun name(): String = path.last()

    val columnName: String get() = name()

    val parentName: String? get() = if (path.size > 1) path[path.size - 2] else null

    override fun rename(newName: String): ColumnPath = ColumnPath(path.dropLast(1) + newName)

    override fun getValue(row: AnyRow): Any? = row.owner[this][row.index()]

    override fun getValueOrNull(row: AnyRow): Any? = row.owner.getColumnOrNull(this)?.get(row.index())

    override fun toString(): String = path.toString()

    public fun joinToString(separator: String = "/"): String = path.joinToString(separator)

    override fun <C> get(column: ColumnReference<C>): ColumnAccessor<C> = ColumnAccessorImpl(this + column.path())
}
