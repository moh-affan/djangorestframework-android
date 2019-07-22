package dev.affan.djangorestframework

fun BooleanArray.mapInPlace(transform: (index: Int, Boolean) -> Boolean) {
    var idx = 0
    this.forEachIndexed { index, _ -> this[index] = transform(idx++, this[index]) }
}