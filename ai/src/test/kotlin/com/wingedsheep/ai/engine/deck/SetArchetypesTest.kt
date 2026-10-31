package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SetArchetypesTest : StringSpec({

    "Jumpstart 2022 exposes ten supported color-pair plans" {
        val synergies = requireNotNull(SetArchetypes.getForSet("j22"))

        synergies.setName shouldBe "Jumpstart 2022"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Blink Value",
            "Draw and Attrition",
            "Sacrifice and Raid",
            "Ferocious Ramp",
            "Counters and Tokens",
            "Life Gain and Drain",
            "Spells and Cycling",
            "Morbid Recursion",
            "Equipment Aggro",
            "Ramp and Big Spells",
        )
        synergies.archetypes.map { it.colors.toSet() } shouldContainExactly ALL_COLOR_PAIRS.toList()
    }

    "Legends exposes its five allied gold-legend pairs" {
        val synergies = requireNotNull(SetArchetypes.getForSet("leg"))

        synergies.setName shouldBe "Legends"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Banding Skies",
            "Legendary Control",
            "Landwalk Aggro",
            "Rampage Stompy",
            "Legendary Midrange",
        )
        synergies.archetypes.map { it.colors.toSet() } shouldContainExactly listOf(
            setOf(Color.WHITE, Color.BLUE),
            setOf(Color.BLUE, Color.BLACK),
            setOf(Color.BLACK, Color.RED),
            setOf(Color.RED, Color.GREEN),
            setOf(Color.GREEN, Color.WHITE),
        )
    }

    "Dominaria United exposes all ten limited color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("dmu"))

        synergies.setName shouldBe "Dominaria United"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Spells Tempo",
            "Spells Recursion",
            "Sacrifice Aggro",
            "Domain Stompy",
            "Tokens",
            "Sacrifice Drain",
            "Spells Aggro",
            "Lands Graveyard",
            "Power Aggro",
            "Domain Ramp",
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
    }

    "The Brothers' War exposes all ten limited color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("bro"))

        synergies.setName shouldBe "The Brothers' War"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Soldiers",
            "Card Draw",
            "Artifact Sacrifice",
            "Powerstone Ramp",
            "Artifact Counters",
            "Small Recursion",
            "Noncreature Spells",
            "Creatures Die",
            "Go-Wide Aggro",
            "Big Artifacts",
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
        synergies.archetypes.single { it.name == "Soldiers" }.creatureTypes shouldContainExactly listOf("Soldier")
    }

    "Wilds of Eldraine exposes all ten limited color-pair archetypes" {
        val woe = SetArchetypes.getForSet("woe")

        woe?.setName shouldBe "Wilds of Eldraine"
        woe?.archetypes?.map { it.name } shouldContainExactly listOf(
            "Tap Tempo",
            "Faeries",
            "Rats",
            "Ferocious Stompy",
            "Enchanted Creatures",
            "Bargain",
            "Spells",
            "Food",
            "Celebration Aggro",
            "Big Spells",
        )
        woe?.archetypes?.map { it.colors.toSet() }?.toSet() shouldBe ALL_COLOR_PAIRS
    }

    "The Lost Caverns of Ixalan exposes all ten limited color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("lci"))

        synergies.setName shouldBe "The Lost Caverns of Ixalan"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Crafted Artifacts",
            "Descend Control",
            "Descend Aggro",
            "Dinosaurs",
            "Modified Go-Wide",
            "Vampire Sacrifice",
            "Pirate Artifacts",
            "Deep Descend",
            "Tap Midrange",
            "Merfolk Explore",
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
    }

    "The Lost Caverns of Ixalan typal archetypes carry creature type hints" {
        val archetypes = SetArchetypes.getForSet("LCI")!!.archetypes.associateBy { it.name }

        archetypes.getValue("Dinosaurs").creatureTypes shouldContainExactly listOf("Dinosaur")
        archetypes.getValue("Vampire Sacrifice").creatureTypes shouldContainExactly listOf("Vampire")
        archetypes.getValue("Pirate Artifacts").creatureTypes shouldContainExactly listOf("Pirate")
        archetypes.getValue("Merfolk Explore").creatureTypes shouldContainExactly listOf("Merfolk")
    }

    "Aetherdrift exposes all ten two-color limited archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("dft"))

        synergies.setName shouldBe "Aetherdrift"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Artifact Value",
            "Artifact Bleeder",
            "Max Speed Aggro",
            "Exhaust Midrange",
            "Vehicles and Mounts Midrange",
            "Max Speed Attrition",
            "Discard Aggro",
            "Graveyard",
            "Vehicles and Mounts Aggro",
            "Exhaust Ramp",
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
    }

    "Innistrad Remastered exposes all ten two-color draft archetypes" {
        val synergies = SetArchetypes.getForSet("inr")

        synergies?.setName shouldBe "Innistrad Remastered"
        synergies?.archetypes?.map { it.colors.toSet() } shouldContainExactly ALL_COLOR_PAIRS.toList()
    }

    "Innistrad Remastered tribal archetypes carry creature type hints" {
        val archetypes = SetArchetypes.getForSet("INR")!!.archetypes.associateBy { it.name }

        archetypes.getValue("Spirits Tempo").creatureTypes shouldContainExactly listOf("Spirit")
        archetypes.getValue("Zombies").creatureTypes shouldContainExactly listOf("Zombie")
        archetypes.getValue("Vampires / Madness").creatureTypes shouldContainExactly listOf("Vampire")
        archetypes.getValue("Werewolves").creatureTypes shouldContainExactly listOf("Werewolf", "Wolf")
        archetypes.getValue("Humans / Tokens").creatureTypes shouldContainExactly listOf("Human")
    }

    "Innistrad Remastered archetypes can be matched by deck colors" {
        SetArchetypes.getMatchingArchetypes("INR", setOf(Color.WHITE, Color.BLUE))
            .map { it.name } shouldContainExactly listOf("Spirits Tempo")
    }
    "Streets of New Capenna exposes its five families and five allied-pair archetypes" {
        val synergies = SetArchetypes.getForSet("SNC")!!

        synergies.setName shouldBe "Streets of New Capenna"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Brokers" to setOf(Color.WHITE, Color.BLUE, Color.GREEN),
            "Obscura" to setOf(Color.WHITE, Color.BLUE, Color.BLACK),
            "Maestros" to setOf(Color.BLUE, Color.BLACK, Color.RED),
            "Riveteers" to setOf(Color.BLACK, Color.RED, Color.GREEN),
            "Cabaretti" to setOf(Color.RED, Color.GREEN, Color.WHITE),
            "Counters Flyers" to setOf(Color.WHITE, Color.BLUE),
            "Graveyard Value" to setOf(Color.BLUE, Color.BLACK),
            "Sacrifice" to setOf(Color.BLACK, Color.RED),
            "Treasure Ramp" to setOf(Color.RED, Color.GREEN),
            "Citizens" to setOf(Color.GREEN, Color.WHITE),
        )
        synergies.archetypes.single { it.name == "Citizens" }.creatureTypes shouldContainExactly listOf("Citizen")
    }

    "Throne of Eldraine exposes one archetype per gold signpost uncommon" {
        val synergies = SetArchetypes.getForSet("ELD")!!

        synergies.setName shouldBe "Throne of Eldraine"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Artifacts & Enchantments" to setOf(Color.WHITE, Color.BLUE),
            "Mill Control" to setOf(Color.BLUE, Color.BLACK),
            "Sacrifice Knights" to setOf(Color.BLACK, Color.RED),
            "Non-Humans" to setOf(Color.RED, Color.GREEN),
            "Adventures" to setOf(Color.GREEN, Color.WHITE),
            "Knights" to setOf(Color.WHITE, Color.BLACK),
            "Draw Two" to setOf(Color.BLUE, Color.RED),
            "Food" to setOf(Color.BLACK, Color.GREEN),
            "Knights Aggro" to setOf(Color.RED, Color.WHITE),
            "Ramp" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.filter { it.creatureTypes.isNotEmpty() }.map { it.name } shouldContainExactly
            listOf("Sacrifice Knights", "Knights", "Knights Aggro")
    }

    "Core Set 2020 exposes one archetype per gold signpost uncommon" {
        val synergies = SetArchetypes.getForSet("M20")!!

        synergies.setName shouldBe "Core Set 2020"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Fliers" to setOf(Color.WHITE, Color.BLUE),
            "Loot Control" to setOf(Color.BLUE, Color.BLACK),
            "Damage Removal" to setOf(Color.BLACK, Color.RED),
            "Elemental Aggro" to setOf(Color.RED, Color.GREEN),
            "Go Wide" to setOf(Color.GREEN, Color.WHITE),
            "Life Drain" to setOf(Color.WHITE, Color.BLACK),
            "Flying Tempo" to setOf(Color.BLUE, Color.RED),
            "Creatures Dying" to setOf(Color.BLACK, Color.GREEN),
            "Air and Ground Aggro" to setOf(Color.RED, Color.WHITE),
            "Elemental Ramp" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.filter { it.creatureTypes.isNotEmpty() }.map { it.name to it.creatureTypes } shouldContainExactly
            listOf("Elemental Aggro" to listOf("Elemental"), "Elemental Ramp" to listOf("Elemental"))
    }

    "Ikoria exposes one archetype per gold signpost uncommon" {
        val synergies = SetArchetypes.getForSet("IKO")!!

        synergies.setName shouldBe "Ikoria: Lair of Behemoths"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Flying Counters" to setOf(Color.WHITE, Color.BLUE),
            "Flash" to setOf(Color.BLUE, Color.BLACK),
            "Menace" to setOf(Color.BLACK, Color.RED),
            "Trample" to setOf(Color.RED, Color.GREEN),
            "Vigilance" to setOf(Color.GREEN, Color.WHITE),
            "Humans" to setOf(Color.WHITE, Color.BLACK),
            "Spells" to setOf(Color.BLUE, Color.RED),
            "Mutate Graveyard" to setOf(Color.BLACK, Color.GREEN),
            "Cycling" to setOf(Color.RED, Color.WHITE),
            "Mutate" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.single { it.name == "Humans" }.creatureTypes shouldContainExactly listOf("Human")
    }

    "Kamigawa: Neon Dynasty exposes one archetype per allied and enemy pair" {
        val synergies = SetArchetypes.getForSet("NEO")!!

        synergies.setName shouldBe "Kamigawa: Neon Dynasty"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Vehicles" to setOf(Color.WHITE, Color.BLUE),
            "Ninjas" to setOf(Color.BLUE, Color.BLACK),
            "Artifact Sacrifice" to setOf(Color.BLACK, Color.RED),
            "Modified" to setOf(Color.RED, Color.GREEN),
            "Enchantments" to setOf(Color.GREEN, Color.WHITE),
            "Artifacts & Enchantments" to setOf(Color.WHITE, Color.BLACK),
            "Artifacts Tempo" to setOf(Color.BLUE, Color.RED),
            "Graveyard Value" to setOf(Color.BLACK, Color.GREEN),
            "Samurai & Warriors" to setOf(Color.RED, Color.WHITE),
            "Channel Ramp" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.single { it.name == "Ninjas" }.creatureTypes shouldContainExactly listOf("Ninja", "Rogue")
        synergies.archetypes.single { it.name == "Samurai & Warriors" }.creatureTypes shouldContainExactly
            listOf("Samurai", "Warrior")
    }

    "Innistrad: Midnight Hunt exposes one archetype per gold signpost uncommon" {
        val synergies = SetArchetypes.getForSet("MID")!!

        synergies.setName shouldBe "Innistrad: Midnight Hunt"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Disturb Spirits" to setOf(Color.WHITE, Color.BLUE),
            "Decayed Zombies" to setOf(Color.BLUE, Color.BLACK),
            "Vampire Aggro" to setOf(Color.BLACK, Color.RED),
            "Werewolves" to setOf(Color.RED, Color.GREEN),
            "Coven Humans" to setOf(Color.GREEN, Color.WHITE),
            "Sacrifice" to setOf(Color.WHITE, Color.BLACK),
            "Flashback Spells" to setOf(Color.BLUE, Color.RED),
            "Graveyard Value" to setOf(Color.BLACK, Color.GREEN),
            "Day/Night Aggro" to setOf(Color.RED, Color.WHITE),
            "Self-Mill Flashback" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.single { it.name == "Werewolves" }.creatureTypes shouldContainExactly listOf("Werewolf")
        synergies.archetypes.single { it.name == "Decayed Zombies" }.creatureTypes shouldContainExactly listOf("Zombie")
    }

    "March of the Machine: The Aftermath exposes one archetype per gold signpost uncommon" {
        val synergies = SetArchetypes.getForSet("MAT")!!

        synergies.setName shouldBe "March of the Machine: The Aftermath"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Legends Matter" to setOf(Color.WHITE, Color.BLUE),
            "Death Rewards" to setOf(Color.WHITE, Color.BLACK),
            "Artifact & Enchantment Value" to setOf(Color.RED, Color.WHITE),
            "Graveyard Recursion" to setOf(Color.GREEN, Color.WHITE),
        )
        synergies.archetypes.all { it.creatureTypes.isEmpty() } shouldBe true
    }
    "Strixhaven: School of Mages exposes its five college archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("stx"))

        synergies.setName shouldBe "Strixhaven: School of Mages"
        // STX prints gold cards only in the five enemy-pair colleges; the allied pairs have no signposts.
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Silverquill" to setOf(Color.WHITE, Color.BLACK),
            "Prismari" to setOf(Color.BLUE, Color.RED),
            "Witherbloom" to setOf(Color.BLACK, Color.GREEN),
            "Lorehold" to setOf(Color.RED, Color.WHITE),
            "Quandrix" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.filter { it.creatureTypes.isNotEmpty() }.associate { it.name to it.creatureTypes } shouldBe mapOf(
            "Lorehold" to listOf("Spirit"),
        )
    }
    "Core Set 2021 exposes its ten signpost color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("m21"))

        synergies.setName shouldBe "Core Set 2021"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Fliers" to setOf(Color.WHITE, Color.BLUE),
            "Graveyard Value" to setOf(Color.BLUE, Color.BLACK),
            "Sacrifice Aggro" to setOf(Color.BLACK, Color.RED),
            "Big Creatures" to setOf(Color.RED, Color.GREEN),
            "+1/+1 Counters" to setOf(Color.GREEN, Color.WHITE),
            "Life Gain" to setOf(Color.WHITE, Color.BLACK),
            "Spells" to setOf(Color.BLUE, Color.RED),
            "Creatures Dying" to setOf(Color.BLACK, Color.GREEN),
            "Go-Wide Aggro" to setOf(Color.RED, Color.WHITE),
            "Card Draw" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
    }
    "Zendikar Rising exposes its ten signpost color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("znr"))

        synergies.setName shouldBe "Zendikar Rising"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Party Up" to setOf(Color.WHITE, Color.BLUE),
            "Rogue Mill" to setOf(Color.BLUE, Color.BLACK),
            "Party Aggro" to setOf(Color.BLACK, Color.RED),
            "Landfall Aggro" to setOf(Color.RED, Color.GREEN),
            "Lands Ramp" to setOf(Color.GREEN, Color.WHITE),
            "Clerics Life Gain" to setOf(Color.WHITE, Color.BLACK),
            "Wizards Spells" to setOf(Color.BLUE, Color.RED),
            "Counters & Kicker" to setOf(Color.BLACK, Color.GREEN),
            "Warriors Aggro" to setOf(Color.RED, Color.WHITE),
            "Kicker Ramp" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
        synergies.archetypes.filter { it.creatureTypes.isNotEmpty() }.associate { it.name to it.creatureTypes } shouldBe mapOf(
            "Rogue Mill" to listOf("Rogue"),
            "Clerics Life Gain" to listOf("Cleric"),
            "Wizards Spells" to listOf("Wizard"),
            "Warriors Aggro" to listOf("Warrior"),
        )
    }
    "Kaldheim exposes its ten signpost color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("khm"))

        synergies.setName shouldBe "Kaldheim"
        synergies.archetypes.associate { it.name to it.colors.toSet() } shouldBe mapOf(
            "Foretell" to setOf(Color.WHITE, Color.BLUE),
            "Snow Zombies" to setOf(Color.BLUE, Color.BLACK),
            "Berserkers" to setOf(Color.BLACK, Color.RED),
            "Big Spells Ramp" to setOf(Color.RED, Color.GREEN),
            "Go Wide" to setOf(Color.GREEN, Color.WHITE),
            "Double Spells" to setOf(Color.WHITE, Color.BLACK),
            "Giants & Wizards" to setOf(Color.BLUE, Color.RED),
            "Elves" to setOf(Color.BLACK, Color.GREEN),
            "Equipment & Auras" to setOf(Color.RED, Color.WHITE),
            "Snow" to setOf(Color.GREEN, Color.BLUE),
        )
        synergies.archetypes.map { it.colors.toSet() }.toSet() shouldBe ALL_COLOR_PAIRS
        synergies.archetypes.filter { it.creatureTypes.isNotEmpty() }.associate { it.name to it.creatureTypes } shouldBe mapOf(
            "Snow Zombies" to listOf("Zombie"),
            "Berserkers" to listOf("Berserker"),
            "Giants & Wizards" to listOf("Giant", "Wizard"),
            "Elves" to listOf("Elf"),
        )
    }
    "Adventures in the Forgotten Realms exposes its ten signpost color-pair archetypes" {
        val synergies = requireNotNull(SetArchetypes.getForSet("afr"))

        synergies.setName shouldBe "Adventures in the Forgotten Realms"
        synergies.archetypes.map { it.name } shouldContainExactly listOf(
            "Dungeon Delvers",
            "Evasive Control",
            "Treasure Sacrifice",
            "Pack Tactics",
            "Life Gain Counters",
            "Venture Recursion",
            "Dice Rolling",
            "Death Value",
            "Equipment Aggro",
            "Ramp Value",
        )
        synergies.archetypes.map { it.colors.toSet() } shouldContainExactly ALL_COLOR_PAIRS.toList()
    }
})

private val ALL_COLOR_PAIRS = linkedSetOf(
    setOf(Color.WHITE, Color.BLUE),
    setOf(Color.BLUE, Color.BLACK),
    setOf(Color.BLACK, Color.RED),
    setOf(Color.RED, Color.GREEN),
    setOf(Color.GREEN, Color.WHITE),
    setOf(Color.WHITE, Color.BLACK),
    setOf(Color.BLUE, Color.RED),
    setOf(Color.BLACK, Color.GREEN),
    setOf(Color.RED, Color.WHITE),
    setOf(Color.GREEN, Color.BLUE),
)
