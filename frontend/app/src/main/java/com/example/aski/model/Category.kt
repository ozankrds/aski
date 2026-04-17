package com.example.aski.model

data class Category(val id: Int, val name: String)

val categories = listOf(
    Category(0, "All"),
    Category(1, "Clothing"),
    Category(2, "Electronics"),
    Category(3, "Books"),
    Category(4, "Furniture"),
    Category(5, "Home & Kitchen"),
    Category(6, "Baby & Child"),
    Category(7, "Toys"),
    Category(8, "Sports & Outdoor"),
    Category(9, "Pet Supplies"),
    Category(10, "Hobbies & Art"),
    Category(11, "Garden"),
    Category(12, "Music & Instruments"),
    Category(13, "Automotive"),
    Category(14, "Other")
)