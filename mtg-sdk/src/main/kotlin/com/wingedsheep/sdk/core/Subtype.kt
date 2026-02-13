package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Subtype(val value: String) {
    override fun toString(): String = value

    companion object {
        // Common creature types
        val ANGEL = Subtype("Angel")
        val APE = Subtype("Ape")
        val ARCHER = Subtype("Archer")
        val ASSASSIN = Subtype("Assassin")
        val CITIZEN = Subtype("Citizen")
        val BEAR = Subtype("Bear")
        val BEAST = Subtype("Beast")
        val BERSERKER = Subtype("Berserker")
        val BIRD = Subtype("Bird")
        val CAT = Subtype("Cat")
        val CLERIC = Subtype("Cleric")
        val DEMON = Subtype("Demon")
        val DRAGON = Subtype("Dragon")
        val DRYAD = Subtype("Dryad")
        val ELEMENTAL = Subtype("Elemental")
        val ELF = Subtype("Elf")
        val FAERIE = Subtype("Faerie")
        val FISH = Subtype("Fish")
        val FROG = Subtype("Frog")
        val GOBLIN = Subtype("Goblin")
        val HORROR = Subtype("Horror")
        val HUMAN = Subtype("Human")
        val ILLUSION = Subtype("Illusion")
        val IMP = Subtype("Imp")
        val KITHKIN = Subtype("Kithkin")
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
        val SHAPESHIFTER = Subtype("Shapeshifter")
        val SOLDIER = Subtype("Soldier")
        val SORCERER = Subtype("Sorcerer")
        val SPIDER = Subtype("Spider")
        val SPIRIT = Subtype("Spirit")
        val WALL = Subtype("Wall")
        val WARLOCK = Subtype("Warlock")
        val WARRIOR = Subtype("Warrior")
        val WIZARD = Subtype("Wizard")
        val WURM = Subtype("Wurm")
        val ZOMBIE = Subtype("Zombie")

        // Phase 7 subtypes
        val BARBARIAN = Subtype("Barbarian")
        val CROCODILE = Subtype("Crocodile")
        val CYCLOPS = Subtype("Cyclops")
        val DJINN = Subtype("Djinn")
        val DRAKE = Subtype("Drake")
        val EEL = Subtype("Eel")
        val GIANT = Subtype("Giant")
        val GOAT = Subtype("Goat")
        val GRIFFIN = Subtype("Griffin")
        val HIPPO = Subtype("Hippo")
        val HORSE = Subtype("Horse")
        val LEVIATHAN = Subtype("Leviathan")
        val LIZARD = Subtype("Lizard")
        val MERCENARY = Subtype("Mercenary")
        val MERFOLK = Subtype("Merfolk")
        val MINOTAUR = Subtype("Minotaur")
        val NIGHTSTALKER = Subtype("Nightstalker")
        val OCTOPUS = Subtype("Octopus")
        val PEGASUS = Subtype("Pegasus")
        val RAT = Subtype("Rat")
        val TREEFOLK = Subtype("Treefolk")
        val TURTLE = Subtype("Turtle")
        val UNICORN = Subtype("Unicorn")
        val VAMPIRE = Subtype("Vampire")
        val WRAITH = Subtype("Wraith")
        val SKELETON = Subtype("Skeleton")
        val SNAKE = Subtype("Snake")

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
        val VEHICLE = Subtype("Vehicle")

        // Planeswalker subtypes
        val AJANI = Subtype("Ajani")
        val JACE = Subtype("Jace")
        val LILIANA = Subtype("Liliana")
        val CHANDRA = Subtype("Chandra")
        val GARRUK = Subtype("Garruk")
        val NISSA = Subtype("Nissa")

        fun of(value: String): Subtype = Subtype(value)

        /**
         * All basic land types. Used for type-changing effects like "is an Island"
         * which replace all existing land subtypes (Rule 305.7).
         */
        val ALL_BASIC_LAND_TYPES: Set<String> = setOf(
            "Plains", "Island", "Swamp", "Mountain", "Forest"
        )

        /**
         * All recognized creature types for text-changing effects.
         * Sorted alphabetically for presentation in choose-option decisions.
         */
        val ALL_CREATURE_TYPES: List<String> = listOf(
            "Angel", "Ape", "Archer", "Assassin", "Barbarian", "Bear", "Beast",
            "Berserker", "Bird", "Cat", "Citizen", "Cleric", "Crocodile", "Cyclops",
            "Demon", "Djinn", "Dragon", "Drake", "Dryad", "Eel", "Elemental",
            "Elf", "Faerie", "Fish", "Frog", "Giant", "Goat", "Goblin", "Griffin",
            "Hippo", "Horror", "Horse", "Human", "Illusion", "Imp", "Insect",
            "Jellyfish", "Kithkin", "Knight", "Leviathan", "Lizard", "Mercenary",
            "Merfolk", "Minotaur", "Monk", "Nightstalker", "Octopus", "Pegasus",
            "Pirate", "Ranger", "Rat", "Rhino", "Rogue", "Scout", "Serpent",
            "Shapeshifter", "Skeleton", "Snake", "Soldier", "Sorcerer", "Spider",
            "Spirit", "Treefolk", "Turtle", "Unicorn", "Vampire", "Wall",
            "Warlock", "Warrior", "Wizard", "Wraith", "Wurm", "Zombie"
        ).sorted()
    }
}
