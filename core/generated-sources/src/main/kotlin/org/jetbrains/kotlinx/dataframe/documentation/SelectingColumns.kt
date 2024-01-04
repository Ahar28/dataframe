package org.jetbrains.kotlinx.dataframe.documentation

import org.jetbrains.kotlinx.dataframe.ColumnSelector
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ColumnSelectionDslLink
import org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl
import org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDslLink
import org.jetbrains.kotlinx.dataframe.api.colsOf
import org.jetbrains.kotlinx.dataframe.api.column
import org.jetbrains.kotlinx.dataframe.api.fillNulls
import org.jetbrains.kotlinx.dataframe.api.gather
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.api.update
import org.jetbrains.kotlinx.dataframe.columns.ColumnReference
import org.jetbrains.kotlinx.dataframe.columns.ColumnsResolver
import org.jetbrains.kotlinx.dataframe.columns.SingleColumn
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnAccessors
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnAccessorsLink
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnNames
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnNamesLink
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.Dsl
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.DslLink
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.DslSingleLink
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.KProperties
import org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.KPropertiesLink
import kotlin.reflect.KProperty

/** [Selecting Columns][SelectingColumns] */
internal interface SelectingColumnsLink

/**
 * ## Selecting Columns
 * Selecting columns for various operations (including but not limited to
 * [DataFrame.select], [DataFrame.update], [DataFrame.gather], and [DataFrame.fillNulls])
 * can be done in the following ways:
 * ### 1. [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.Dsl.WithExample]
 * Select or express columns using the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl].
 * (Any (combination of) [Access API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi]).
 *
 * This DSL comes in the form a [Columns Selector][org.jetbrains.kotlinx.dataframe.ColumnsSelector] lambda,
 * which operates on the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl] and
 * expects you to return a [ColumnsResolver][org.jetbrains.kotlinx.dataframe.columns.ColumnsResolver]; an entity formed by calling any (combination) of the functions
 * in the DSL that is or can be resolved into one or more columns.
 * ### Check out: [Columns Selection DSL Usage][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl.Usage]
 *
 * &nbsp;&nbsp;&nbsp;&nbsp;
 *
 * [See Column Selectors on the documentation website.](https://kotlin.github.io/dataframe/columnselectors.html)
 *
 * #### For example:
 *
 * `df.`operation` { length `[and][org.jetbrains.kotlinx.dataframe.api.AndColumnsSelectionDsl.and]` age }`
 *
 * `df.`operation` { `[cols][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl.cols]`(1..5) }`
 *
 * `df.`operation` { `[colsOf][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl.colsOf]`<`[Double][Double]`>() }`
 *
 *
 * #### NOTE: There's also a 'single column' variant used sometimes: [Column Selection DSL][org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.DslSingle.WithExample].
 * ### 2. [Column names][org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnNames.WithExample]
 * Select columns using their [column names][String]
 * ([String API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.StringApi]).
 *
 * #### For example:
 *
 * `df.`operation`("length", "age")`
 *
 * ### 3. [Column references][org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.ColumnAccessors.WithExample]
 * Select columns using [column accessors][org.jetbrains.kotlinx.dataframe.columns.ColumnReference]
 * ([Column Accessors API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.ColumnAccessorsApi]).
 *
 * #### For example:
 *
 * `val length by `[column][org.jetbrains.kotlinx.dataframe.api.column]`<`[Double][Double]`>()`
 *
 * `val age by `[column][org.jetbrains.kotlinx.dataframe.api.column]`<`[Double][Double]`>()`
 *
 * `df.`operation`(length, age)`
 *
 * ### 4. [KProperties][org.jetbrains.kotlinx.dataframe.documentation.SelectingColumns.KProperties.WithExample]
 * Select columns using [KProperties][KProperty] ([KProperties API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.KPropertiesApi]).
 *
 * #### For example:
 * ```kotlin
 * data class Person(val length: Double, val age: Double)
 * ```
 *
 * `df.`operation`(Person::length, Person::age)`
 *
 */
internal interface SelectingColumns {

    /**
     * The key for an @setArg that will define the operation name for the examples below.
     * Make sure to [alias][your examples].
     */
    interface OperationArg

    interface SetDefaultOperationArg

    /**
     * Select or express columns using the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl].
     * (Any (combination of) [Access API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi]).
     *
     * This DSL comes in the form a [Columns Selector][ColumnsSelector] lambda,
     * which operates on the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl] and
     * expects you to return a [ColumnsResolver]; an entity formed by calling any (combination) of the functions
     * in the DSL that is or can be resolved into one or more columns.
     * ### Check out: [Columns Selection DSL Usage][ColumnsSelectionDsl.Usage]
     *
     * &nbsp;&nbsp;&nbsp;&nbsp;
     *
     * [See Column Selectors on the documentation website.](https://kotlin.github.io/dataframe/columnselectors.html)
     */
    interface Dsl {

        /**
         * Select or express columns using the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl].
         * (Any (combination of) [Access API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi]).
         *
         * This DSL comes in the form a [Columns Selector][org.jetbrains.kotlinx.dataframe.ColumnsSelector] lambda,
         * which operates on the [Columns Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl] and
         * expects you to return a [ColumnsResolver][org.jetbrains.kotlinx.dataframe.columns.ColumnsResolver]; an entity formed by calling any (combination) of the functions
         * in the DSL that is or can be resolved into one or more columns.
         * ### Check out: [Columns Selection DSL Usage][org.jetbrains.kotlinx.dataframe.api.ColumnsSelectionDsl.Usage]
         *
         * &nbsp;&nbsp;&nbsp;&nbsp;
         *
         * [See Column Selectors on the documentation website.](https://kotlin.github.io/dataframe/columnselectors.html)
         *
         * #### For example:
         *
         * `df.`operation` { length `[and][ColumnsSelectionDsl.and]` age }`
         *
         * `df.`operation` { `[cols][ColumnsSelectionDsl.cols]`(1..5) }`
         *
         * `df.`operation` { `[colsOf][ColumnsSelectionDsl.colsOf]`<`[Double][Double]`>() }`
         *
         *
         */
        interface WithExample
    }

    /** [Columns Selection DSL][Dsl.WithExample] */
    interface DslLink

    /**
     * Select or express a single column using the Column Selection DSL.
     * (Any [Access API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi]).
     *
     * This DSL comes in the form of a [Column Selector][ColumnSelector] lambda,
     * which operates in the [Column Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnSelectionDsl] and
     * expects you to return a [SingleColumn].
     *
     *
     * &nbsp;&nbsp;&nbsp;&nbsp;
     *
     * [See Column Selectors on the documentation website.](https://kotlin.github.io/dataframe/columnselectors.html)
     */
    interface DslSingle {

        /**
         * Select or express a single column using the Column Selection DSL.
         * (Any [Access API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi]).
         *
         * This DSL comes in the form of a [Column Selector][org.jetbrains.kotlinx.dataframe.ColumnSelector] lambda,
         * which operates in the [Column Selection DSL][org.jetbrains.kotlinx.dataframe.api.ColumnSelectionDsl] and
         * expects you to return a [SingleColumn][org.jetbrains.kotlinx.dataframe.columns.SingleColumn].
         *
         *
         * &nbsp;&nbsp;&nbsp;&nbsp;
         *
         * [See Column Selectors on the documentation website.](https://kotlin.github.io/dataframe/columnselectors.html)
         *
         * #### For example:
         *
         * `df.`operation` { length }`
         *
         * `df.`operation` { `[col][ColumnsSelectionDsl.col]`(1) }`
         *
         * `df.`operation` { `[colsOf][ColumnsSelectionDsl.colsOf]`<`[Double][Double]`>().`[first][ColumnsSelectionDsl.first]`() }`
         *
         */
        interface WithExample
    }

    /** [Column Selection DSL][DslSingle.WithExample] */
    interface DslSingleLink

    /**
     * Select columns using their [column names][String]
     * ([String API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.StringApi]).
     */
    interface ColumnNames {

        /**
         * Select columns using their [column names][String]
         * ([String API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.StringApi]).
         *
         * #### For example:
         *
         * `df.`operation`("length", "age")`
         *
         */
        interface WithExample
    }

    /** [Column names][ColumnNames.WithExample] */
    interface ColumnNamesLink

    /**
     * Select columns using [column accessors][ColumnReference]
     * ([Column Accessors API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.ColumnAccessorsApi]).
     */
    interface ColumnAccessors {

        /**
         * Select columns using [column accessors][org.jetbrains.kotlinx.dataframe.columns.ColumnReference]
         * ([Column Accessors API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.ColumnAccessorsApi]).
         *
         * #### For example:
         *
         * `val length by `[column][column]`<`[Double][Double]`>()`
         *
         * `val age by `[column][column]`<`[Double][Double]`>()`
         *
         * `df.`operation`(length, age)`
         *
         */
        interface WithExample
    }

    /** [Column references][ColumnAccessors.WithExample] */
    interface ColumnAccessorsLink

    /** Select columns using [KProperties][KProperty] ([KProperties API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.KPropertiesApi]). */
    interface KProperties {

        /**
         * Select columns using [KProperties][KProperty] ([KProperties API][org.jetbrains.kotlinx.dataframe.documentation.AccessApi.KPropertiesApi]).
         *
         * #### For example:
         * ```kotlin
         * data class Person(val length: Double, val age: Double)
         * ```
         *
         * `df.`operation`(Person::length, Person::age)`
         *
         */
        interface WithExample
    }

    /** [KProperties][KProperties.WithExample] */
    interface KPropertiesLink
}
