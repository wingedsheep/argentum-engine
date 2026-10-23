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
