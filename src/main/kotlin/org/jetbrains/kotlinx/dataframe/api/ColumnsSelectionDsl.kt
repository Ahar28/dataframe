package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.BooleanCol
import org.jetbrains.kotlinx.dataframe.ColumnFilter
import org.jetbrains.kotlinx.dataframe.ColumnsContainer
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DoubleCol
import org.jetbrains.kotlinx.dataframe.IntCol
import org.jetbrains.kotlinx.dataframe.NumberCol
import org.jetbrains.kotlinx.dataframe.Predicate
import org.jetbrains.kotlinx.dataframe.RowSelector
import org.jetbrains.kotlinx.dataframe.StringCol
import org.jetbrains.kotlinx.dataframe.columns.ColumnAccessor
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.columns.ColumnSet
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.columns.SingleColumn
import org.jetbrains.kotlinx.dataframe.columns.renamedReference
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnAccessorImpl
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnsList
import org.jetbrains.kotlinx.dataframe.impl.columns.allColumnsExcept
import org.jetbrains.kotlinx.dataframe.impl.columns.asColumnGroup
import org.jetbrains.kotlinx.dataframe.impl.columns.createColumnSet
import org.jetbrains.kotlinx.dataframe.impl.columns.newColumnWithActualType
import org.jetbrains.kotlinx.dataframe.impl.columns.single
import org.jetbrains.kotlinx.dataframe.impl.columns.toColumns
import org.jetbrains.kotlinx.dataframe.impl.columns.top
import org.jetbrains.kotlinx.dataframe.impl.columns.transform
import org.jetbrains.kotlinx.dataframe.impl.columns.transformSingle
import org.jetbrains.kotlinx.dataframe.impl.columns.tree.dfs
import org.jetbrains.kotlinx.dataframe.impl.getType
import kotlin.reflect.KProperty
import kotlin.reflect.KType

public interface ColumnsSelectionDsl<out T> : ColumnsContainer<T>, SingleColumn<DataRow<T>> {

    public fun ColumnSet<*>.first(numCols: Int): ColumnSet<Any?> = take(numCols)

    public fun <C> ColumnSet<C>.first(condition: ColumnFilter<C>): SingleColumn<C> =
        transform { listOf(it.first(condition)) }.single()

    public fun <C> ColumnSet<C>.single(condition: ColumnFilter<C>): SingleColumn<C> =
        transform { listOf(it.single(condition)) }.single()

    public fun ColumnSet<*>.last(numCols: Int): ColumnSet<Any?> = takeLast(numCols)

    public fun ColumnsContainer<*>.group(name: String): ColumnGroup<*> = this.get(name) as ColumnGroup<*>

    public fun <C> ColumnSet<*>.cols(firstCol: ColumnReference<C>, vararg otherCols: ColumnReference<C>): ColumnSet<C> =
        (listOf(firstCol) + otherCols).let { refs ->
            transform { it.flatMap { col -> refs.mapNotNull { col.getChild(it) } } }
        }

    public fun ColumnSet<*>.cols(firstCol: String, vararg otherCols: String): ColumnSet<Any?> =
        (listOf(firstCol) + otherCols).let { names ->
            transform { it.flatMap { col -> names.mapNotNull { col.getChild(it) } } }
        }

    public fun ColumnSet<*>.cols(vararg indices: Int): ColumnSet<Any?> =
        transform { it.flatMap { it.children().let { children -> indices.map { children[it] } } } }

    public fun ColumnSet<*>.cols(range: IntRange): ColumnSet<Any?> =
        transform { it.flatMap { it.children().subList(range.start, range.endInclusive + 1) } }

    public fun ColumnSet<*>.cols(predicate: (AnyCol) -> Boolean = { true }): ColumnSet<Any?> = colsInternal(predicate)

    public fun <C> ColumnSet<C>.dfs(predicate: (ColumnWithPath<*>) -> Boolean): ColumnSet<Any?> = dfsInternal(predicate)

    public fun SingleColumn<*>.all(): ColumnSet<*> = transformSingle { it.children() }

    public fun none(): ColumnSet<*> = ColumnsList<Any?>(emptyList())

    public fun ColumnSet<*>.dfs(): ColumnSet<Any?> = dfs { !it.isColumnGroup() }

    // excluding current
    public fun SingleColumn<*>.allAfter(colPath: ColumnPath): ColumnSet<Any?> {
        var take = false
        return children {
            if (take) true
            else {
                take = colPath == it.path
                false
            }
        }
    }

    public fun SingleColumn<*>.allAfter(colName: String): ColumnSet<Any?> = allAfter(pathOf(colName))
    public fun SingleColumn<*>.allAfter(column: Column): ColumnSet<Any?> = allAfter(column.path())

    // including current
    public fun SingleColumn<*>.allSince(colPath: ColumnPath): ColumnSet<Any?> {
        var take = false
        return children {
            if (take) true
            else {
                take = colPath == it.path
                take
            }
        }
    }

    public fun SingleColumn<*>.allSince(colName: String): ColumnSet<Any?> = allSince(pathOf(colName))
    public fun SingleColumn<*>.allSince(column: Column): ColumnSet<Any?> = allSince(column.path())

    // excluding current
    public fun SingleColumn<*>.allBefore(colPath: ColumnPath): ColumnSet<Any?> {
        var take = true
        return children {
            if (!take) false
            else {
                take = colPath != it.path
                take
            }
        }
    }

    public fun SingleColumn<*>.allBefore(colName: String): ColumnSet<Any?> = allBefore(pathOf(colName))
    public fun SingleColumn<*>.allBefore(column: Column): ColumnSet<Any?> = allBefore(column.path())

    // including current
    public fun SingleColumn<*>.allUntil(colPath: ColumnPath): ColumnSet<Any?> {
        var take = true
        return children {
            if (!take) false
            else {
                take = colPath != it.path
                true
            }
        }
    }

    public fun SingleColumn<*>.allUntil(colName: String): ColumnSet<Any?> = allUntil(pathOf(colName))
    public fun SingleColumn<*>.allUntil(column: Column): ColumnSet<Any?> = allUntil(column.path())

    public fun SingleColumn<*>.groups(filter: (ColumnGroup<*>) -> Boolean = { true }): ColumnSet<AnyRow> =
        children { it.isColumnGroup() && filter(it.asColumnGroup()) } as ColumnSet<AnyRow>

    public fun <C> ColumnSet<C>.children(predicate: (ColumnWithPath<Any?>) -> Boolean = { true }): ColumnSet<Any?> =
        transform { it.flatMap { it.children().filter { predicate(it) } } }

    public fun ColumnGroupReference.children(): ColumnSet<Any?> = transform { it.single().children() }

    public operator fun <C> List<DataColumn<C>>.get(range: IntRange): ColumnSet<C> =
        ColumnsList(subList(range.first, range.last + 1))

    public operator fun String.invoke(): ColumnAccessor<Any?> = toColumnAccessor()
    public operator fun String.get(column: String): ColumnPath = pathOf(this, column)
    public fun <C> String.cast(): ColumnAccessor<C> = ColumnAccessorImpl(this)

    public fun <C> col(property: KProperty<C>): ColumnAccessor<C> = property.toColumnAccessor()

    public fun ColumnSet<*>.col(index: Int): ColumnSet<Any?> = transform { it.mapNotNull { it.getChild(index) } }

    public operator fun ColumnSet<*>.get(colName: String): ColumnSet<Any?> = transform { it.mapNotNull { it.getChild(colName) } }
    public operator fun <C> ColumnSet<*>.get(column: ColumnReference<C>): ColumnSet<C> = cols(column)

    public fun <C> ColumnSet<C>.drop(n: Int): ColumnSet<C> = transform { it.drop(n) }
    public fun <C> ColumnSet<C>.take(n: Int): ColumnSet<C> = transform { it.take(n) }
    public fun <C> ColumnSet<C>.dropLast(n: Int): ColumnSet<C> = transform { it.dropLast(n) }
    public fun <C> ColumnSet<C>.takeLast(n: Int): ColumnSet<C> = transform { it.takeLast(n) }
    public fun <C> ColumnSet<C>.top(): ColumnSet<C> = transform { it.top() }
    public fun <C> ColumnSet<C>.takeWhile(predicate: Predicate<ColumnWithPath<C>>): ColumnSet<C> =
        transform { it.takeWhile(predicate) }

    public fun <C> ColumnSet<C>.takeLastWhile(predicate: Predicate<ColumnWithPath<C>>): ColumnSet<C> =
        transform { it.takeLastWhile(predicate) }

    public fun <C> ColumnSet<C>.filter(predicate: Predicate<ColumnWithPath<C>>): ColumnSet<C> =
        transform { it.filter(predicate) }

    public fun ColumnSet<*>.numberCols(filter: (NumberCol) -> Boolean = { true }): ColumnSet<Number?> = colsOf(filter)
    public fun ColumnSet<*>.stringCols(filter: (StringCol) -> Boolean = { true }): ColumnSet<String?> = colsOf(filter)
    public fun ColumnSet<*>.intCols(filter: (IntCol) -> Boolean = { true }): ColumnSet<Int?> = colsOf(filter)
    public fun ColumnSet<*>.doubleCols(filter: (DoubleCol) -> Boolean = { true }): ColumnSet<Double?> = colsOf(filter)
    public fun ColumnSet<*>.booleanCols(filter: (BooleanCol) -> Boolean = { true }): ColumnSet<Boolean?> = colsOf(filter)

    public fun ColumnSet<*>.nameContains(text: CharSequence): ColumnSet<Any?> = cols { it.name.contains(text) }
    public fun ColumnSet<*>.nameContains(regex: Regex): ColumnSet<Any?> = cols { it.name.contains(regex) }
    public fun ColumnSet<*>.startsWith(prefix: CharSequence): ColumnSet<Any?> = cols { it.name.startsWith(prefix) }
    public fun ColumnSet<*>.endsWith(suffix: CharSequence): ColumnSet<Any?> = cols { it.name.endsWith(suffix) }

    public infix fun <C> ColumnSet<C>.and(other: ColumnSet<C>): ColumnSet<C> = ColumnsList(this, other)

    public fun <C> ColumnSet<C>.except(vararg other: ColumnSet<*>): ColumnSet<*> = except(other.toColumns())
    public fun <C> ColumnSet<C>.except(vararg other: String): ColumnSet<*> = except(other.toColumns())

    public fun <C> ColumnSet<C?>.withoutNulls(): ColumnSet<C> = transform { it.filter { !it.hasNulls } } as ColumnSet<C>

    public infix fun <C> ColumnSet<C>.except(other: ColumnSet<*>): ColumnSet<*> =
        createColumnSet { resolve(it).allColumnsExcept(other.resolve(it)) }

    public infix fun <C> ColumnSet<C>.except(selector: ColumnsSelector<T, *>): ColumnSet<C> =
        except(selector.toColumns()) as ColumnSet<C>

    // public operator fun <C> ColumnSelector<T, C>.invoke(): ColumnReference<C> = this(this@SelectReceiver, this@SelectReceiver)
    public operator fun <C> ColumnsSelector<T, C>.invoke(): ColumnSet<C> =
        this(this@ColumnsSelectionDsl, this@ColumnsSelectionDsl)

    public operator fun <C> ColumnReference<C>.invoke(): DataColumn<C> = getColumn(this)

    public operator fun <C> ColumnReference<C>.invoke(newName: String): ColumnReference<C> = renamedReference(newName)
    public infix fun <C> ColumnReference<C>.into(newName: String): ColumnReference<C> = named(newName)
    public infix fun <C> ColumnReference<C>.named(newName: String): ColumnReference<C> = renamedReference(newName)

    public fun ColumnReference<String?>.length(): ColumnReference<Int?> = map { it?.length }
    public fun ColumnReference<String?>.lowercase(): ColumnReference<String?> = map { it?.lowercase() }
    public fun ColumnReference<String?>.uppercase(): ColumnReference<String?> = map { it?.uppercase() }

    public infix fun String.and(other: String): ColumnSet<Any?> = toColumnAccessor() and other.toColumnAccessor()
    public infix fun <C> String.and(other: ColumnSet<C>): ColumnSet<Any?> = toColumnAccessor() and other
    public infix fun <C> KProperty<C>.and(other: ColumnSet<C>): ColumnSet<C> = toColumnAccessor() and other
    public infix fun <C> ColumnSet<C>.and(other: KProperty<C>): ColumnSet<C> = this and other.toColumnAccessor()
    public infix fun <C> KProperty<C>.and(other: KProperty<C>): ColumnSet<C> =
        toColumnAccessor() and other.toColumnAccessor()

    public infix fun <C> ColumnSet<C>.and(other: String): ColumnSet<Any?> = this and other.toColumnAccessor()

    public operator fun ColumnPath.invoke(): ColumnAccessor<Any?> = toColumnAccessor()

    public operator fun <C> String.invoke(newColumnExpression: RowSelector<T, C>): DataColumn<C> =
        newColumnWithActualType(this, newColumnExpression)

    public infix fun <C> String.by(newColumnExpression: RowSelector<T, C>): DataColumn<C> =
        newColumnWithActualType(this, newColumnExpression)

    public fun String.ints(): DataColumn<Int> = getColumn(this)
    public fun String.intOrNulls(): DataColumn<Int?> = getColumn(this)
    public fun String.strings(): DataColumn<String> = getColumn(this)
    public fun String.stringOrNulls(): DataColumn<String?> = getColumn(this)
    public fun String.booleans(): DataColumn<Boolean> = getColumn(this)
    public fun String.booleanOrNulls(): DataColumn<Boolean?> = getColumn(this)
    public fun String.doubles(): DataColumn<Double> = getColumn(this)
    public fun String.doubleOrNulls(): DataColumn<Double?> = getColumn(this)
    public fun String.comparables(): DataColumn<Comparable<Any?>> = getColumn(this)
    public fun String.comparableOrNulls(): DataColumn<Comparable<Any?>?> = getColumn(this)
    public fun String.numberOrNulls(): DataColumn<Number?> = getColumn(this)

    public fun ColumnPath.ints(): DataColumn<Int> = getColumn(this)
    public fun ColumnPath.intOrNulls(): DataColumn<Int?> = getColumn(this)
    public fun ColumnPath.strings(): DataColumn<String> = getColumn(this)
    public fun ColumnPath.stringOrNulls(): DataColumn<String?> = getColumn(this)
    public fun ColumnPath.booleans(): DataColumn<Boolean> = getColumn(this)
    public fun ColumnPath.booleanOrNulls(): DataColumn<Boolean?> = getColumn(this)
    public fun ColumnPath.doubles(): DataColumn<Double> = getColumn(this)
    public fun ColumnPath.doubleOrNulls(): DataColumn<Double?> = getColumn(this)
    public fun ColumnPath.comparables(): DataColumn<Comparable<Any?>> = getColumn(this)
    public fun ColumnPath.comparableOrNulls(): DataColumn<Comparable<Any?>?> = getColumn(this)
    public fun ColumnPath.numberOrNulls(): DataColumn<Number?> = getColumn(this)
    public fun nrow(): Int
    public fun rows(): Iterable<DataRow<T>>
    public fun rowsReversed(): Iterable<DataRow<T>>
}

public inline fun <T, reified R> ColumnsSelectionDsl<T>.expr(
    name: String = "",
    useActualType: Boolean = false,
    noinline expression: AddExpression<T, R>
): DataColumn<R> = newColumn(name, useActualType, expression)

internal fun <T, R> ColumnsSelectionDsl<T>.exprWithActualType(
    name: String = "",
    expression: AddExpression<T, R>
): DataColumn<R> = newColumnWithActualType(name, expression)

internal fun <T, C> ColumnsSelector<T, C>.filter(predicate: (ColumnWithPath<C>) -> Boolean): ColumnsSelector<T, C> =
    { this@filter(it, it).filter(predicate) }
// internal fun Columns<*>.filter(predicate: (AnyCol) -> Boolean) = transform { it.filter { predicate(it.data) } }

internal fun ColumnSet<*>.colsInternal(predicate: (AnyCol) -> Boolean) =
    transform { it.flatMap { it.children().filter { predicate(it.data) } } }

internal fun ColumnSet<*>.dfsInternal(predicate: (ColumnWithPath<*>) -> Boolean) =
    transform { it.filter { it.isColumnGroup() }.flatMap { it.children().dfs().filter(predicate) } }

public fun <C> ColumnSet<*>.dfsOf(type: KType, predicate: (ColumnWithPath<C>) -> Boolean = { true }): ColumnSet<*> =
    dfsInternal { it.data.hasElementsOfType(type) && predicate(it.typed()) }

public inline fun <reified C> ColumnSet<*>.dfsOf(noinline filter: (ColumnWithPath<C>) -> Boolean = { true }): ColumnSet<C> =
    dfsOf(
        getType<C>(),
        filter
    ) as ColumnSet<C>

public fun ColumnSet<*>.colsOf(type: KType): ColumnSet<Any?> = colsOf(type) { true }

public fun <C> ColumnSet<*>.colsOf(type: KType, filter: (DataColumn<C>) -> Boolean): ColumnSet<C> =
    colsInternal { it.hasElementsOfType(type) && filter(it.typed()) } as ColumnSet<C>

public inline fun <reified C> ColumnSet<*>.colsOf(noinline filter: (DataColumn<C>) -> Boolean = { true }): ColumnSet<C> =
    colsOf(
        getType<C>(), filter
    )