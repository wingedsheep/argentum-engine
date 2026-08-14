package com.wingedsheep.sdk.core

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

        /**
         * The supertypes present in a *projected* type set — the engine's layer projection stores
         * supertypes, card types and subtypes together in one `Set<String>` of uppercase enum names,
         * so every reader has to isolate them by name. Deriving that from [entries] (rather than
         * repeating a literal `setOf("BASIC", "LEGENDARY", …)`) keeps the readers correct when a
         * supertype is added here.
         *
         * Returned in declaration order, which is printed order: "Basic Snow Land", "Legendary Snow Land".
         */
        fun fromProjectedTypes(types: Set<String>): List<Supertype> =
            entries.filter { it.name in types }
    }
}
