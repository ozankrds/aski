package com.example.aski.model

data class Category(
    val id: Int,
    val name: String
)

val mockCategories = listOf(
    Category(0, "All"),
    Category(1, "Clothing"),
    Category(2, "Electronics"),
    Category(3, "Books"),
    Category(4, "Furniture")
)
