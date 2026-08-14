package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SetArchetypesTest : StringSpec({

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
