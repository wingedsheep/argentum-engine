package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Subtype(val value: String) {
    override fun toString(): String = value

    companion object {
        // Common creature types
        val ANGEL = Subtype("Angel")
        val ASSASSIN = Subtype("Assassin")
        val BEAR = Subtype("Bear")
        val BEAST = Subtype("Beast")
        val BIRD = Subtype("Bird")
        val CAT = Subtype("Cat")
        val CLERIC = Subtype("Cleric")
        val DEMON = Subtype("Demon")
        val DRAGON = Subtype("Dragon")
        val ELEMENTAL = Subtype("Elemental")
        val ELF = Subtype("Elf")
        val FROG = Subtype("Frog")
        val GOBLIN = Subtype("Goblin")
        val HORROR = Subtype("Horror")
        val HUMAN = Subtype("Human")
        val IMP = Subtype("Imp")
        val INSECT = Subtype("Insect")
        val JELLYFISH = Subtype("Jellyfish")
        val KNIGHT = Subtype("Knight")
        val MONK = Subtype("Monk")
        val PIRATE = Subtype("Pirate")
        val RANGER = Subtype("Ranger")
        val RHINO = Subtype("Rhino")
        val ROGUE = Subtype("Rogue")
        val SCOUT = Subtype("Scout")
        val SERPENT = Subtype("Serpent")
        val SOLDIER = Subtype("Soldier")
        val SPIRIT = Subtype("Spirit")
        val WALL = Subtype("Wall")
        val WARRIOR = Subtype("Warrior")
        val WIZARD = Subtype("Wizard")
        val WURM = Subtype("Wurm")
        val ZOMBIE = Subtype("Zombie")

        // Phase 7 subtypes
        val CROCODILE = Subtype("Crocodile")
        val CYCLOPS = Subtype("Cyclops")
        val DJINN = Subtype("Djinn")
        val DRAKE = Subtype("Drake")
        val EEL = Subtype("Eel")
        val GIANT = Subtype("Giant")
        val GRIFFIN = Subtype("Griffin")
        val LIZARD = Subtype("Lizard")
        val MERFOLK = Subtype("Merfolk")
        val MINOTAUR = Subtype("Minotaur")
        val OCTOPUS = Subtype("Octopus")
        val PEGASUS = Subtype("Pegasus")
        val RAT = Subtype("Rat")
        val TREEFOLK = Subtype("Treefolk")
        val TURTLE = Subtype("Turtle")
        val UNICORN = Subtype("Unicorn")

        // Basic land types
        val PLAINS = Subtype("Plains")
        val ISLAND = Subtype("Island")
        val SWAMP = Subtype("Swamp")
        val MOUNTAIN = Subtype("Mountain")
        val FOREST = Subtype("Forest")

        // Enchantment subtypes
        val AURA = Subtype("Aura")

        // Artifact subtypes
        val EQUIPMENT = Subtype("Equipment")

        fun of(value: String): Subtype = Subtype(value)
    }
}
