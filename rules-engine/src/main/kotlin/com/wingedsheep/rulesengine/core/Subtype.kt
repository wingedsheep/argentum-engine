package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Subtype(val value: String) {
    override fun toString(): String = value

    companion object {
        // Common creature types
        val DRAGON = Subtype("Dragon")
        val GOBLIN = Subtype("Goblin")
        val HUMAN = Subtype("Human")
        val SOLDIER = Subtype("Soldier")
        val WIZARD = Subtype("Wizard")
        val ZOMBIE = Subtype("Zombie")
        val ELF = Subtype("Elf")
        val ANGEL = Subtype("Angel")
        val DEMON = Subtype("Demon")
        val BEAST = Subtype("Beast")
        val ELEMENTAL = Subtype("Elemental")
        val KNIGHT = Subtype("Knight")
        val WARRIOR = Subtype("Warrior")
        val CLERIC = Subtype("Cleric")
        val ROGUE = Subtype("Rogue")
        val PIRATE = Subtype("Pirate")
        val SERPENT = Subtype("Serpent")

        // Basic land types
        val PLAINS = Subtype("Plains")
        val ISLAND = Subtype("Island")
        val SWAMP = Subtype("Swamp")
        val MOUNTAIN = Subtype("Mountain")
        val FOREST = Subtype("Forest")

        fun of(value: String): Subtype = Subtype(value)
    }
}
