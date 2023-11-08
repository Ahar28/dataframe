package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.samples.api.age
import org.jetbrains.kotlinx.dataframe.samples.api.firstName
import org.jetbrains.kotlinx.dataframe.samples.api.lastName
import org.jetbrains.kotlinx.dataframe.samples.api.name
import org.jetbrains.kotlinx.dataframe.samples.api.weight
import org.junit.Test

class AndTests : ColumnsSelectionDslTests() {

    @Test
    fun `and 2`() {
        val ageSelector: ColumnsSelector<*, Int> = { "age"<Int>() }
        listOf(
            df.select { cols(age, name) },

            df.select { age and name },
            df.select { age and "name" },
            df.select { age and Person::name },
            df.select { age and pathOf("name") },
            df.select { age and cols(name) },
            df.select { age.and(name) },
            df.select { age.and("name") },
            df.select { age.and(Person::name) },
            df.select { age.and(pathOf("name")) },
            df.select { age.and(cols(name)) },
            df.select { age and { name } },
            df.select { age and { "name"<String>() } },
            df.select { age and { (Person::name)() } },
            df.select { age and { pathOf("name") } },
            df.select { age and { cols(name) } },
            df.select { age.and { name } },

            df.select { "age" and name },
            df.select { "age" and "name" },
            df.select { "age" and Person::name },
            df.select { "age" and pathOf("name") },
            df.select { "age" and cols(name) },
            df.select { "age".and(name) },
            df.select { "age".and("name") },
            df.select { "age".and(Person::name) },
            df.select { "age".and(pathOf("name")) },
            df.select { "age".and(cols(name)) },
            df.select { "age" and { name } },
            df.select { "age" and { "name"<String>() } },
            df.select { "age" and { (Person::name)() } },
            df.select { "age" and { pathOf("name") } },
            df.select { "age" and { cols(name) } },
            df.select { "age".and { name } },

            df.select { Person::age and name },
            df.select { Person::age and "name" },
            df.select { Person::age and Person::name },
            df.select { Person::age and pathOf("name") },
            df.select { Person::age and cols(name) },
            df.select { Person::age.and(name) },
            df.select { Person::age.and("name") },
            df.select { Person::age.and(Person::name) },
            df.select { Person::age.and(pathOf("name")) },
            df.select { Person::age.and(cols(name)) },
            df.select { Person::age and { name } },
            df.select { Person::age and { "name"<String>() } },
            df.select { Person::age and { (Person::name)() } },
            df.select { Person::age and { pathOf("name") } },
            df.select { Person::age and { cols(name) } },
            df.select { Person::age.and { name } },

            df.select { pathOf("age") and name },
            df.select { pathOf("age") and "name" },
            df.select { pathOf("age") and Person::name },
            df.select { pathOf("age") and pathOf("name") },
            df.select { pathOf("age") and cols(name) },
            df.select { pathOf("age").and(name) },
            df.select { pathOf("age").and("name") },
            df.select { pathOf("age").and(Person::name) },
            df.select { pathOf("age").and(pathOf("name")) },
            df.select { pathOf("age").and(cols(name)) },
            df.select { pathOf("age") and { name } },
            df.select { pathOf("age") and { "name"<String>() } },
            df.select { pathOf("age") and { (Person::name)() } },
            df.select { pathOf("age") and { pathOf("name") } },
            df.select { pathOf("age") and { cols(name) } },
            df.select { pathOf("age").and { name } },

            df.select { ageSelector and name },
            df.select { ageSelector and "name" },
            df.select { ageSelector and Person::name },
            df.select { ageSelector and pathOf("name") },
            df.select { ageSelector and cols(name) },
            df.select { ageSelector.and(name) },
            df.select { ageSelector.and("name") },
            df.select { ageSelector.and(Person::name) },
            df.select { ageSelector.and(pathOf("name")) },
            df.select { ageSelector.and(cols(name)) },
            df.select { ageSelector and { name } },
            df.select { ageSelector and { "name"<String>() } },
            df.select { ageSelector and { (Person::name)() } },
            df.select { ageSelector and { pathOf("name") } },
            df.select { ageSelector and { cols(name) } },
            df.select { ageSelector.and { name } },
        ).shouldAllBeEqual()
    }

    @Test
    fun `and 3`() {
        listOf(
            df.select { cols(age, name, weight) },
            df.select { age and name and weight },
            df.select { age and "name" and weight },
            df.select { age and Person::name and weight },
            df.select { age and pathOf("name") and weight },
            df.select { age and cols(name) and weight },
            df.select { age.and(name).and(weight) },
            df.select { age.and("name").and(weight) },
            df.select { age.and(Person::name).and(weight) },
            df.select { age.and(pathOf("name")).and(weight) },
            df.select { age.and(cols(name)).and(weight) },
            df.select { age.and(name and weight) },
            df.select { age.and("name" and weight) },
            df.select { age.and(Person::name and weight) },
            df.select { age.and(pathOf("name") and weight) },
            df.select { age.and(cols(name) and weight) },
            df.select { age.and(name).and("weight") },
            df.select { age.and("name").and("weight") },
            df.select { age.and(Person::name).and("weight") },
            df.select { age.and(pathOf("name")).and("weight") },
            df.select { age.and(cols(name)).and("weight") },
            df.select { age.and(name and "weight") },
            df.select { age.and("name" and "weight") },
            df.select { age.and(Person::name and "weight") },
            df.select { age.and(pathOf("name") and "weight") },
            df.select { age.and(cols(name) and "weight") },
            df.select { age.and(name).and(Person::weight) },
            df.select { age.and("name").and(Person::weight) },
            df.select { age.and(Person::name).and(Person::weight) },
            df.select { age.and(pathOf("name")).and(Person::weight) },
            df.select { age.and(cols(name)).and(Person::weight) },
            df.select { age.and(name and Person::weight) },
            df.select { age.and("name" and Person::weight) },
            df.select { age.and(Person::name and Person::weight) },
            df.select { age.and(pathOf("name") and Person::weight) },
            df.select { age.and(cols(name) and Person::weight) }
        ).shouldAllBeEqual()
    }
}
