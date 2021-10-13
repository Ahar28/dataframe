package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.union
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.columns.Columns
import org.jetbrains.kotlinx.dataframe.columns.FrameColumn
import org.jetbrains.kotlinx.dataframe.columns.SingleColumn
import org.jetbrains.kotlinx.dataframe.columns.name
import org.jetbrains.kotlinx.dataframe.columns.size
import org.jetbrains.kotlinx.dataframe.columns.values
import org.jetbrains.kotlinx.dataframe.impl.DataFrameSize
import org.jetbrains.kotlinx.dataframe.impl.TreeNode
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnWithParent
import org.jetbrains.kotlinx.dataframe.impl.columns.addPath
import org.jetbrains.kotlinx.dataframe.impl.columns.asGroup
import org.jetbrains.kotlinx.dataframe.impl.columns.changePath
import org.jetbrains.kotlinx.dataframe.impl.getOrPutEmpty
import org.jetbrains.kotlinx.dataframe.impl.projectTo
import org.jetbrains.kotlinx.dataframe.impl.toColumnPath
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

public fun rowNumber(columnName: String = "id"): AddRowNumberStub = AddRowNumberStub(columnName)

public data class AddRowNumberStub(val columnName: String)

// size

public val AnyFrame.size: DataFrameSize get() = DataFrameSize(ncol(), nrow())

public fun commonParent(classes: Iterable<KClass<*>>): KClass<*>? = commonParents(classes).withMostSuperclasses()

public fun commonParent(vararg classes: KClass<*>): KClass<*>? = commonParent(classes.toList())

public fun Iterable<KClass<*>>.withMostSuperclasses(): KClass<*>? = maxByOrNull { it.allSuperclasses.size }

public fun Iterable<KClass<*>>.createType(nullable: Boolean, upperBound: KType? = null): KType =
    if (upperBound == null) (withMostSuperclasses() ?: Any::class).createStarProjectedType(nullable)
    else {
        val upperClass = upperBound.classifier as KClass<*>
        val baseClass = filter { it.isSubclassOf(upperClass) }.withMostSuperclasses() ?: withMostSuperclasses()
        if (baseClass == null) Any::class.createStarProjectedType(nullable)
        else upperBound.projectTo(baseClass).withNullability(nullable)
    }

public fun commonParents(vararg classes: KClass<*>): List<KClass<*>> = commonParents(classes.toList())

public fun commonParents(classes: Iterable<KClass<*>>): List<KClass<*>> =
    when {
        !classes.any() -> emptyList()
        else -> {
            classes.distinct().let {
                when {
                    it.size == 1 && it[0].visibility == KVisibility.PUBLIC -> { // if there is only one class - return it
                        listOf(it[0])
                    }
                    else -> it.fold(null as (Set<KClass<*>>?)) { set, clazz ->
                        // collect a set of all common superclasses from original classes
                        val superclasses =
                            (clazz.allSuperclasses + clazz).filter { it.visibility == KVisibility.PUBLIC }
                        set?.intersect(superclasses) ?: superclasses.toSet()
                    }!!.let {
                        it - it.flatMap { it.superclasses } // leave only 'leaf' classes, that are not super to some other class in a set
                    }.toList()
                }
            }
        }
    }

internal fun baseType(types: Set<KType>): KType {
    val nullable = types.any { it.isMarkedNullable }
    return when (types.size) {
        0 -> getType<Unit>()
        1 -> types.single()
        else -> {
            val classes = types.map { it.jvmErasure }.distinct()
            when {
                classes.size == 1 -> {
                    val typeProjections = classes[0].typeParameters.mapIndexed { index, parameter ->
                        val arguments = types.map { it.arguments[index].type }.toSet()
                        if (arguments.contains(null)) KTypeProjection.STAR
                        else {
                            val type = baseType(arguments as Set<KType>)
                            KTypeProjection(parameter.variance, type)
                        }
                    }
                    classes[0].createType(typeProjections, nullable)
                }
                classes.any { it == List::class } && classes.all { it == List::class || !it.isSubclassOf(Collection::class) } -> {
                    val listTypes =
                        types.map { if (it.classifier == List::class) it.arguments[0].type else it }.toMutableSet()
                    if (listTypes.contains(null)) List::class.createStarProjectedType(nullable)
                    else {
                        val type = baseType(listTypes as Set<KType>)
                        List::class.createType(listOf(KTypeProjection.invariant(type)), nullable)
                    }
                }
                else -> {
                    val commonClass = commonParent(classes) ?: Any::class
                    commonClass.createStarProjectedType(nullable)
                }
            }
        }
    }
}

internal fun indexColumn(columnName: String, size: Int): AnyCol = column(columnName, (0 until size).toList())

public fun <T> DataFrame<T>.addRowNumber(column: ColumnReference<Int>): DataFrame<T> = addRowNumber(column.name())
public fun <T> DataFrame<T>.addRowNumber(columnName: String = "id"): DataFrame<T> =
    dataFrameOf(columns() + indexColumn(columnName, nrow())).typed()

public fun AnyCol.addRowNumber(columnName: String = "id"): AnyFrame =
    dataFrameOf(listOf(indexColumn(columnName, size), this))

// Update

public inline fun <reified C> headPlusArray(head: C, cols: Array<out C>): Array<C> =
    (listOf(head) + cols.toList()).toTypedArray()

public inline fun <reified C> headPlusIterable(head: C, cols: Iterable<C>): Iterable<C> =
    (listOf(head) + cols.asIterable())

// column grouping

internal fun TreeNode<ColumnPosition>.allRemovedColumns() = dfs { it.data.wasRemoved && it.data.column != null }

internal fun TreeNode<ColumnPosition>.allWithColumns() = dfs { it.data.column != null }

internal fun Iterable<ColumnWithPath<*>>.dfs(): List<ColumnWithPath<*>> {
    val result = mutableListOf<ColumnWithPath<*>>()
    fun dfs(cols: Iterable<ColumnWithPath<*>>) {
        cols.forEach {
            result.add(it)
            val path = it.path
            val df = it.df
            if (it.data.isGroup()) {
                dfs(it.data.asGroup().columns().map { it.addPath(path + it.name(), df) })
            }
        }
    }
    dfs(this)
    return result
}

internal fun List<ColumnWithPath<*>>.collectTree() = collectTree(null) { it }

internal fun <D> List<ColumnWithPath<*>>.collectTree(emptyData: D, createData: (AnyCol) -> D): TreeNode<D> {
    val root = TreeNode.createRoot(emptyData)

    fun collectColumns(col: AnyCol, parentNode: TreeNode<D>) {
        val newNode = parentNode.getOrPut(col.name()) { createData(col) }
        if (col.isGroup()) {
            col.asGroup().columns().forEach {
                collectColumns(it, newNode)
            }
        }
    }
    forEach {
        if (it.path.isEmpty()) {
            it.data.asGroup().df.columns().forEach {
                collectColumns(it, root)
            }
        } else {
            val node = root.getOrPutEmpty(it.path.dropLast(1), emptyData)
            collectColumns(it.data, node)
        }
    }
    return root
}

public interface ReferenceData {
    public val originalIndex: Int
    public val wasRemoved: Boolean
}

internal data class ColumnPosition(
    override val originalIndex: Int,
    override var wasRemoved: Boolean,
    var column: AnyCol?
) : ReferenceData

public fun Column.getParent(): MapColumnReference? = when (this) {
    is ColumnWithParent<*> -> parent
    else -> null
}

public fun Column.getPath(): ColumnPath {
    val list = mutableListOf<String>()
    var c = this.asNullable()
    while (c != null) {
        list.add(c.name())
        c = c.getParent()
    }
    list.reverse()
    return list.toColumnPath()
}

internal fun <T> DataFrame<T>.collectTree(selector: ColumnsSelector<T, *>): TreeNode<AnyCol?> {
    val colPaths = getColumnPaths(selector)

    val root = TreeNode.createRoot(null as AnyCol?)

    colPaths.forEach {
        var column: AnyCol? = null
        var node: TreeNode<AnyCol?> = root
        it.forEach {
            when (column) {
                null -> column = this[it]
                else -> column = column!!.asFrame()[it]
            }
            node = node.getOrPut(it) { null }
        }
        node.data = column
    }

    return root
}

internal fun <T> DataFrame<T>.splitByIndices(
    startIndices: Sequence<Int>,
    emptyToNull: Boolean
): Sequence<DataFrame<T>?> {
    return (startIndices + nrow).zipWithNext { start, endExclusive ->
        if (emptyToNull && start == endExclusive) null
        else get(start until endExclusive)
    }
}

internal fun <T> List<T>.splitByIndices(startIndices: Sequence<Int>): Sequence<Many<T>> {
    return (startIndices + size).zipWithNext { start, endExclusive ->
        subList(start, endExclusive).toMany()
    }
}

internal fun KClass<*>.createTypeWithArgument(argument: KType? = null, nullable: Boolean = false): KType {
    require(typeParameters.size == 1)
    return if (argument != null) createType(listOf(KTypeProjection.invariant(argument)), nullable)
    else createStarProjectedType(nullable)
}

internal inline fun <reified T> createTypeWithArgument(typeArgument: KType? = null) =
    T::class.createTypeWithArgument(typeArgument)

public fun <T> FrameColumn<T>.union(): DataFrame<Any?> = if (size > 0) values.union() else emptyDataFrame(0)

internal fun <T> T.asNullable() = this as T?

internal fun <T> List<T>.last(count: Int) = subList(size - count, size)

/**
 * Shorten column paths as much as possible to keep them unique
 */
internal fun <C> List<ColumnWithPath<C>>.shortenPaths(): List<ColumnWithPath<C>> {
    // try to use just column name as column path
    val map = groupBy { it.path.takeLast(1) }.toMutableMap()

    fun add(path: ColumnPath, column: ColumnWithPath<C>) {
        val list: MutableList<ColumnWithPath<C>> =
            (map.getOrPut(path) { mutableListOf() } as? MutableList<ColumnWithPath<C>>)
                ?: let {
                    val values = map.remove(path)!!
                    map.put(path, values.toMutableList()) as MutableList<ColumnWithPath<C>>
                }
        list.add(column)
    }

    // resolve name collisions by using more parts of column path
    var conflicts = map.filter { it.value.size > 1 }
    while (conflicts.size > 0) {
        conflicts.forEach {
            val key = it.key
            val keyLength = key.size
            map.remove(key)
            it.value.forEach {
                val path = it.path
                val newPath = if (path.size < keyLength) path.takeLast(keyLength + 1) else path
                add(newPath, it)
            }
        }
        conflicts = map.filter { it.value.size > 1 }
    }

    val pathRemapping = map.map { it.value.single().path to it.key }.toMap()

    return map { it.changePath(pathRemapping[it.path]!!) }
}

public fun AnyFrame.toMap(): Map<String, List<Any?>> = columns().associateBy({ it.name }, { it.toList() })

internal fun <C> Columns<C>.resolve(df: DataFrame<*>, unresolvedColumnsPolicy: UnresolvedColumnsPolicy) =
    resolve(ColumnResolutionContext(df, unresolvedColumnsPolicy))

internal fun <C> SingleColumn<C>.resolveSingle(df: DataFrame<*>, unresolvedColumnsPolicy: UnresolvedColumnsPolicy) =
    resolveSingle(ColumnResolutionContext(df, unresolvedColumnsPolicy))