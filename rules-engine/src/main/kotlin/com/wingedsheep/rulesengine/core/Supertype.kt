package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
enum class Supertype(val displayName: String) {
    BASIC("Basic"),
    LEGENDARY("Legendary"),
    SNOW("Snow"),
    WORLD("World");

    companion object {
        fun fromString(value: String): Supertype? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }
    }
}
