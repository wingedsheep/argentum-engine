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

        // Additional creature types
        val AVATAR = Subtype("Avatar")
        val BARBARIAN = Subtype("Barbarian")
        val BASILISK = Subtype("Basilisk")
        val BAT = Subtype("Bat")
        val BOAR = Subtype("Boar")
        val CENTAUR = Subtype("Centaur")
        val CEPHALID = Subtype("Cephalid")
        val CROCODILE = Subtype("Crocodile")
        val CYCLOPS = Subtype("Cyclops")
        val DINOSAUR = Subtype("Dinosaur")
        val DJINN = Subtype("Djinn")
        val DRAKE = Subtype("Drake")
        val DRUID = Subtype("Druid")
        val DWARF = Subtype("Dwarf")
        val EEL = Subtype("Eel")
        val ELEPHANT = Subtype("Elephant")
        val ELK = Subtype("Elk")
        val GIANT = Subtype("Giant")
        val GOAT = Subtype("Goat")
        val GOLEM = Subtype("Golem")
        val GORGON = Subtype("Gorgon")
        val GRIFFIN = Subtype("Griffin")
        val HIPPO = Subtype("Hippo")
        val HORSE = Subtype("Horse")
        val HYDRA = Subtype("Hydra")
        val KIRIN = Subtype("Kirin")
        val LEVIATHAN = Subtype("Leviathan")
        val LIZARD = Subtype("Lizard")
        val MERCENARY = Subtype("Mercenary")
        val MERFOLK = Subtype("Merfolk")
        val MINOTAUR = Subtype("Minotaur")
        val MUTANT = Subtype("Mutant")
        val NIGHTSTALKER = Subtype("Nightstalker")
        val NOMAD = Subtype("Nomad")
        val OCTOPUS = Subtype("Octopus")
        val OGRE = Subtype("Ogre")
        val OOZE = Subtype("Ooze")
        val ORC = Subtype("Orc")
        val ORGG = Subtype("Orgg")
        val PANGOLIN = Subtype("Pangolin")
        val PEGASUS = Subtype("Pegasus")
        val PLANT = Subtype("Plant")
        val RAT = Subtype("Rat")
        val REBEL = Subtype("Rebel")
        val SHAMAN = Subtype("Shaman")
        val SKELETON = Subtype("Skeleton")
        val SLIVER = Subtype("Sliver")
        val SNAKE = Subtype("Snake")
        val SPECTER = Subtype("Specter")
        val SPHINX = Subtype("Sphinx")
        val TREEFOLK = Subtype("Treefolk")
        val TURTLE = Subtype("Turtle")
        val UNICORN = Subtype("Unicorn")
        val VAMPIRE = Subtype("Vampire")
        val WOLVERINE = Subtype("Wolverine")
        val WRAITH = Subtype("Wraith")
        val YETI = Subtype("Yeti")

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
            "Angel", "Ape", "Archer", "Assassin", "Avatar", "Barbarian", "Basilisk",
            "Bat", "Bear", "Beast", "Berserker", "Bird", "Boar", "Cat", "Centaur",
            "Cephalid", "Citizen", "Cleric", "Crocodile", "Cyclops", "Demon",
            "Dinosaur", "Djinn", "Dragon", "Drake", "Druid", "Dryad", "Dwarf",
            "Eel", "Elemental", "Elephant", "Elf", "Elk", "Faerie", "Fish", "Frog",
            "Giant", "Goat", "Goblin", "Golem", "Gorgon", "Griffin", "Hippo",
            "Horror", "Horse", "Human", "Hydra", "Illusion", "Imp", "Insect",
            "Jellyfish", "Kirin", "Kithkin", "Knight", "Leviathan", "Lizard",
            "Mercenary", "Merfolk", "Minotaur", "Monk", "Mutant", "Nightstalker",
            "Nomad", "Octopus", "Ogre", "Ooze", "Orc", "Orgg", "Pangolin",
            "Pegasus", "Pirate", "Plant", "Ranger", "Rat", "Rebel", "Rhino",
            "Rogue", "Scout", "Serpent", "Shaman", "Shapeshifter", "Skeleton",
            "Sliver", "Snake", "Soldier", "Sorcerer", "Specter", "Spider",
            "Sphinx", "Spirit", "Treefolk", "Turtle", "Unicorn", "Vampire", "Wall",
            "Warlock", "Warrior", "Wizard", "Wolverine", "Wraith", "Wurm", "Yeti",
            "Zombie"
        ).sorted()
    }
}
