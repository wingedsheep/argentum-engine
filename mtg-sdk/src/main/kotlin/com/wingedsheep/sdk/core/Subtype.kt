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

        // Additional creature types (alphabetical)
        val ADVISOR = Subtype("Advisor")
        val ALLY = Subtype("Ally")
        val ARCHON = Subtype("Archon")
        val ARTIFICER = Subtype("Artificer")
        val ATOG = Subtype("Atog")
        val AVATAR = Subtype("Avatar")
        val BARBARIAN = Subtype("Barbarian")
        val BARD = Subtype("Bard")
        val BASILISK = Subtype("Basilisk")
        val BAT = Subtype("Bat")
        val BOAR = Subtype("Boar")
        val CENTAUR = Subtype("Centaur")
        val CEPHALID = Subtype("Cephalid")
        val CHIMERA = Subtype("Chimera")
        val CONSTRUCT = Subtype("Construct")
        val CRAB = Subtype("Crab")
        val CROCODILE = Subtype("Crocodile")
        val CYCLOPS = Subtype("Cyclops")
        val DEVIL = Subtype("Devil")
        val DINOSAUR = Subtype("Dinosaur")
        val DJINN = Subtype("Djinn")
        val DOG = Subtype("Dog")
        val DRAKE = Subtype("Drake")
        val DRUID = Subtype("Druid")
        val DWARF = Subtype("Dwarf")
        val EEL = Subtype("Eel")
        val EFREET = Subtype("Efreet")
        val ELDRAZI = Subtype("Eldrazi")
        val ELEPHANT = Subtype("Elephant")
        val ELK = Subtype("Elk")
        val FOX = Subtype("Fox")
        val FUNGUS = Subtype("Fungus")
        val GARGOYLE = Subtype("Gargoyle")
        val GIANT = Subtype("Giant")
        val GNOME = Subtype("Gnome")
        val GOAT = Subtype("Goat")
        val GOD = Subtype("God")
        val GOLEM = Subtype("Golem")
        val GORGON = Subtype("Gorgon")
        val GREMLIN = Subtype("Gremlin")
        val GRIFFIN = Subtype("Griffin")
        val HALFLING = Subtype("Halfling")
        val HARPY = Subtype("Harpy")
        val HELLION = Subtype("Hellion")
        val HIPPO = Subtype("Hippo")
        val HOMUNCULUS = Subtype("Homunculus")
        val HORSE = Subtype("Horse")
        val HYDRA = Subtype("Hydra")
        val HYENA = Subtype("Hyena")
        val INCARNATION = Subtype("Incarnation")
        val KAVU = Subtype("Kavu")
        val KIRIN = Subtype("Kirin")
        val KOBOLD = Subtype("Kobold")
        val KOR = Subtype("Kor")
        val KRAKEN = Subtype("Kraken")
        val LEVIATHAN = Subtype("Leviathan")
        val LIZARD = Subtype("Lizard")
        val MANTICORE = Subtype("Manticore")
        val MERCENARY = Subtype("Mercenary")
        val MERFOLK = Subtype("Merfolk")
        val MINION = Subtype("Minion")
        val MINOTAUR = Subtype("Minotaur")
        val MOONFOLK = Subtype("Moonfolk")
        val MOUSE = Subtype("Mouse")
        val MUTANT = Subtype("Mutant")
        val MYR = Subtype("Myr")
        val NIGHTMARE = Subtype("Nightmare")
        val NIGHTSTALKER = Subtype("Nightstalker")
        val NINJA = Subtype("Ninja")
        val NOMAD = Subtype("Nomad")
        val OCTOPUS = Subtype("Octopus")
        val OGRE = Subtype("Ogre")
        val OOZE = Subtype("Ooze")
        val ORC = Subtype("Orc")
        val ORGG = Subtype("Orgg")
        val OTTER = Subtype("Otter")
        val PANGOLIN = Subtype("Pangolin")
        val PEGASUS = Subtype("Pegasus")
        val PHOENIX = Subtype("Phoenix")
        val PHYREXIAN = Subtype("Phyrexian")
        val PLANT = Subtype("Plant")
        val PRAETOR = Subtype("Praetor")
        val RAT = Subtype("Rat")
        val REBEL = Subtype("Rebel")
        val SALAMANDER = Subtype("Salamander")
        val SAMURAI = Subtype("Samurai")
        val SAPROLING = Subtype("Saproling")
        val SATYR = Subtype("Satyr")
        val SCARECROW = Subtype("Scarecrow")
        val SHADE = Subtype("Shade")
        val SHAMAN = Subtype("Shaman")
        val SHARK = Subtype("Shark")
        val SKELETON = Subtype("Skeleton")
        val SLIVER = Subtype("Sliver")
        val SNAKE = Subtype("Snake")
        val SPECTER = Subtype("Specter")
        val SPHINX = Subtype("Sphinx")
        val SQUIRREL = Subtype("Squirrel")
        val THOPTER = Subtype("Thopter")
        val THRULL = Subtype("Thrull")
        val TREEFOLK = Subtype("Treefolk")
        val TROLL = Subtype("Troll")
        val TURTLE = Subtype("Turtle")
        val UNICORN = Subtype("Unicorn")
        val VAMPIRE = Subtype("Vampire")
        val VEDALKEN = Subtype("Vedalken")
        val WEREWOLF = Subtype("Werewolf")
        val WOLF = Subtype("Wolf")
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
         * Lookup index: lowercase name -> canonical creature type name.
         * Supports normalized lookup for names not covered by constants.
         */
        private val CREATURE_TYPE_LOOKUP: Map<String, String> by lazy {
            ALL_CREATURE_TYPES.associateBy { it.lowercase() }
        }

        /**
         * Resolve a creature type from a normalized (case-insensitive) name.
         * Returns the canonical Subtype if found in ALL_CREATURE_TYPES,
         * or creates one with Title Case if not (for forward-compatibility).
         *
         * Examples: "ooze" -> Subtype("Ooze"), "time lord" -> Subtype("Time Lord")
         */
        fun fromName(name: String): Subtype {
            val canonical = CREATURE_TYPE_LOOKUP[name.lowercase().trim()]
            if (canonical != null) return Subtype(canonical)
            // Not in official list — title-case it for consistency
            return Subtype(
                name.trim().split(" ", "-").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercaseChar() }
                }
            )
        }

        /**
         * All basic land types. Used for type-changing effects like "is an Island"
         * which replace all existing land subtypes (Rule 305.7).
         */
        val ALL_BASIC_LAND_TYPES: Set<String> = setOf(
            "Plains", "Island", "Swamp", "Mountain", "Forest"
        )

        /**
         * All official creature types per comprehensive rules 205.3m.
         * Sorted alphabetically for presentation in choose-option decisions.
         */
        val ALL_CREATURE_TYPES: List<String> = listOf(
            "Advisor", "Aetherborn", "Alien", "Ally", "Angel", "Antelope", "Ape",
            "Archer", "Archon", "Armadillo", "Army", "Artificer", "Assassin",
            "Assembly-Worker", "Astartes", "Atog", "Aurochs", "Avatar", "Azra",
            "Badger", "Balloon", "Barbarian", "Bard", "Basilisk", "Bat", "Bear",
            "Beast", "Beaver", "Beeble", "Beholder", "Berserker", "Bird", "Bison",
            "Blinkmoth", "Boar", "Bringer", "Brushwagg",
            "Camarid", "Camel", "Capybara", "Caribou", "Carrier", "Cat", "Centaur",
            "Cephalid", "Child", "Chimera", "Citizen", "Cleric", "Clown",
            "Cockatrice", "Construct", "Coward", "Coyote", "Crab", "Crocodile",
            "C'tan", "Custodes", "Cyberman", "Cyclops",
            "Dalek", "Dauthi", "Demigod", "Demon", "Deserter", "Detective",
            "Devil", "Dinosaur", "Djinn", "Doctor", "Dog", "Dragon", "Drake",
            "Dreadnought", "Drix", "Drone", "Druid", "Dryad", "Dwarf",
            "Echidna", "Eel", "Efreet", "Egg", "Elder", "Eldrazi", "Elemental",
            "Elephant", "Elf", "Elk", "Employee", "Eternal", "Eye",
            "Faerie", "Ferret", "Fish", "Flagbearer", "Fox", "Fractal", "Frog",
            "Fungus",
            "Gamer", "Gamma", "Gargoyle", "Germ", "Giant", "Gith", "Glimmer",
            "Gnoll", "Gnome", "Goat", "Goblin", "God", "Golem", "Gorgon",
            "Graveborn", "Gremlin", "Griffin", "Guest",
            "Hag", "Halfling", "Hamster", "Harpy", "Hedgehog", "Hellion", "Hero",
            "Hippo", "Hippogriff", "Homarid", "Homunculus", "Horror", "Horse",
            "Human", "Hydra", "Hyena",
            "Illusion", "Imp", "Incarnation", "Inkling", "Inquisitor", "Insect",
            "Jackal", "Jellyfish", "Juggernaut",
            "Kangaroo", "Kavu", "Kirin", "Kithkin", "Knight", "Kobold", "Kor",
            "Kraken",
            "Lamia", "Lammasu", "Leech", "Lemur", "Leviathan", "Lhurgoyf", "Licid",
            "Lizard", "Llama", "Lobster",
            "Manticore", "Masticore", "Mercenary", "Merfolk", "Metathran", "Minion",
            "Minotaur", "Mite", "Mole", "Monger", "Mongoose", "Monk", "Monkey",
            "Moogle", "Moonfolk", "Mount", "Mouse", "Mutant", "Myr", "Mystic",
            "Nautilus", "Necron", "Nephilim", "Nightmare", "Nightstalker", "Ninja",
            "Noble", "Noggle", "Nomad", "Nymph",
            "Octopus", "Ogre", "Ooze", "Orb", "Orc", "Orgg", "Otter", "Ouphe", "Ox",
            "Oyster",
            "Pangolin", "Peasant", "Pegasus", "Pentavite", "Performer", "Pest",
            "Phelddagrif", "Phoenix", "Phyrexian", "Pilot", "Pincher", "Pirate",
            "Plant", "Platypus", "Porcupine", "Possum", "Praetor", "Primarch",
            "Prism", "Processor",
            "Qu",
            "Rabbit", "Raccoon", "Ranger", "Rat", "Rebel", "Reflection", "Rhino",
            "Rigger", "Robot", "Rogue",
            "Sable", "Salamander", "Samurai", "Sand", "Saproling", "Satyr",
            "Scarecrow", "Scientist", "Scion", "Scorpion", "Scout", "Sculpture",
            "Seal", "Serf", "Serpent", "Servo", "Shade", "Shaman", "Shapeshifter",
            "Shark", "Sheep", "Siren", "Skeleton", "Skrull", "Skunk", "Slith",
            "Sliver", "Sloth", "Slug", "Snail", "Snake", "Soldier", "Soltari",
            "Sorcerer", "Spawn", "Specter", "Spellshaper", "Sphinx", "Spider",
            "Spike", "Spirit", "Splinter", "Sponge", "Spy", "Squid", "Squirrel",
            "Starfish", "Surrakar", "Survivor", "Symbiote", "Synth",
            "Tentacle", "Tetravite", "Thalakos", "Thopter", "Thrull", "Tiefling",
            "Time Lord", "Toy", "Treefolk", "Trilobite", "Triskelavite", "Troll",
            "Turtle", "Tyranid",
            "Unicorn", "Utrom",
            "Vampire", "Varmint", "Vedalken", "Villain", "Volver",
            "Wall", "Walrus", "Warlock", "Warrior", "Weasel", "Weird", "Werewolf",
            "Whale", "Wizard", "Wolf", "Wolverine", "Wombat", "Worm", "Wraith",
            "Wurm",
            "Yeti",
            "Zombie", "Zubera"
        ).sorted()
    }
}
