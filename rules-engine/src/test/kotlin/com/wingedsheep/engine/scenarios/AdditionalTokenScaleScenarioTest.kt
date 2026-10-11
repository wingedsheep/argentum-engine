package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalTokenScale
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [CreateAdditionalToken]'s [AdditionalTokenScale] axis and how several riders compose.
 *
 * - "those tokens plus **that many** [token]" (Chatterfang) adds one per matching token of the
 *   batch, where the plain rider (Quina, Worldwalker Helm) adds once per event.
 * - Each replacement modifies the event once (CR 614.5) and the next applies to the event as
 *   modified, so the batch grows source by source: two Academy Manufactors make three of each
 *   (the card's ruling). The entries one permanent carries are one ability and judge the batch as
 *   it stood before that permanent applied, so a Manufactor's added Clue doesn't add more.
 * - The affected player orders the replacements (CR 616.1); the engine applies the order that
 *   makes the most tokens — fixed-count riders before "that many"; an unfiltered fixed-count rider
 *   before a filtered one, a filtered "that many" rider before an unfiltered one.
 */
class AdditionalTokenScaleScenarioTest : ScenarioTestBase() {

    private val squirrelMaker = card("Test Squirrel General") {
        manaCost = "{2}{G}"
        typeLine = "Legendary Creature — Squirrel"
        power = 3
        toughness = 3
        replacementEffect(
            CreateAdditionalToken(
                additionalTokenType = "Squirrel",
                inheritTapped = true,
                scale = AdditionalTokenScale.PER_MATCHING_TOKEN
            )
        )
    }

    private val foodMaker = card("Test Food Rider") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Halfling"
        power = 2
        toughness = 3
        replacementEffect(CreateAdditionalToken(additionalTokenType = "Food"))
    }

    private val clueForArtifacts = card("Test Clue Rider") {
        manaCost = "{2}"
        typeLine = "Artifact"
        replacementEffect(
            CreateAdditionalToken(
                additionalTokenType = "Clue",
                appliesTo = EventPattern.TokenCreationEvent(tokenFilter = GameObjectFilter.Artifact)
            )
        )
    }

    private val manufactor = card("Test Manufactor") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Assembly-Worker"
        power = 1
        toughness = 3
        val kinds = listOf("Clue", "Food", "Treasure")
        for (created in kinds) {
            for (added in kinds - created) {
                replacementEffect(
                    CreateAdditionalToken(
                        additionalTokenType = added,
                        appliesTo = EventPattern.TokenCreationEvent(
                            tokenFilter = GameObjectFilter.Artifact.withSubtype(created)
                        ),
                        scale = AdditionalTokenScale.PER_MATCHING_TOKEN
                    )
                )
            }
        }
    }

    private val twoSoldiers = card("Test Two Soldiers") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"), count = 2) }
    }

    private val oneTappedSoldier = card("Test Tapped Soldier") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"), tapped = true) }
    }

    private val twoTreasures = card("Test Two Treasures") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(2) }
    }

    private val oneTreasure = card("Test One Treasure") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(1) }
    }

    init {
        listOf(squirrelMaker, foodMaker, clueForArtifacts, manufactor, twoSoldiers, oneTappedSoldier, twoTreasures, oneTreasure)
            .forEach { cardRegistry.register(it) }

        fun game(vararg battlefield: Pair<Int, String>, spell: String) = scenario()
            .withPlayers("Player", "Opponent")
            .apply { battlefield.forEach { (player, name) -> withCardOnBattlefield(player, name) } }
            .withCardInHand(1, spell)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
            .also {
                it.castSpell(1, spell)
                it.resolveStack()
            }

        context("PER_MATCHING_TOKEN") {
            test("adds one token per token of the batch") {
                val g = game(1 to "Test Squirrel General", spell = "Test Two Soldiers")
                g.findPermanents("Soldier Token").size shouldBe 2
                withClue("that many Squirrels — two, not one") { g.findPermanents("Squirrel").size shouldBe 2 }
            }

            test("the added tokens inherit the creation's tapped rider") {
                val g = game(1 to "Test Squirrel General", spell = "Test Tapped Soldier")
                val squirrel = g.findPermanents("Squirrel").single()
                (g.state.getEntity(squirrel)!!.get<TappedComponent>() != null) shouldBe true
            }

            test("only tokens created under the rider's controller count") {
                val g = game(2 to "Test Squirrel General", spell = "Test Two Soldiers")
                g.findPermanents("Soldier Token").size shouldBe 2
                g.findPermanents("Squirrel").size shouldBe 0
            }

            test("a filtered rider counts only the matching tokens — two Treasures become two of each") {
                val g = game(1 to "Test Manufactor", spell = "Test Two Treasures")
                g.findPermanents("Treasure").size shouldBe 2
                g.findPermanents("Clue").size shouldBe 2
                g.findPermanents("Food").size shouldBe 2
            }

            test("a filtered rider ignores tokens it doesn't match") {
                val g = game(1 to "Test Manufactor", spell = "Test Two Soldiers")
                g.findPermanents("Clue").size shouldBe 0
                g.findPermanents("Food").size shouldBe 0
                g.findPermanents("Treasure").size shouldBe 0
            }
        }

        context("several riders compose source by source") {
            test("one permanent's entries don't feed each other: one Treasure makes exactly one of each") {
                val g = game(1 to "Test Manufactor", spell = "Test One Treasure")
                g.findPermanents("Treasure").size shouldBe 1
                g.findPermanents("Clue").size shouldBe 1
                g.findPermanents("Food").size shouldBe 1
            }

            test("a second permanent judges the grown batch: two Manufactors make three of each") {
                val g = game(1 to "Test Manufactor", 1 to "Test Manufactor", spell = "Test One Treasure")
                g.findPermanents("Treasure").size shouldBe 3
                g.findPermanents("Clue").size shouldBe 3
                g.findPermanents("Food").size shouldBe 3
            }

            test("a fixed-count rider applies before \"that many\", so its token is counted too") {
                val g = game(1 to "Test Squirrel General", 1 to "Test Food Rider", spell = "Test Tapped Soldier")
                g.findPermanents("Soldier Token").size shouldBe 1
                g.findPermanents("Food").size shouldBe 1
                withClue("one Squirrel for the Soldier, one for the Food") { g.findPermanents("Squirrel").size shouldBe 2 }
            }

            test("an unfiltered fixed-count rider applies before a filtered one, so its Food earns the Clue") {
                val g = game(1 to "Test Clue Rider", 1 to "Test Food Rider", spell = "Test Two Soldiers")
                g.findPermanents("Soldier Token").size shouldBe 2
                g.findPermanents("Food").size shouldBe 1
                withClue("the artifact rider sees the added Food") { g.findPermanents("Clue").size shouldBe 1 }
            }

            test("a filtered rider applies before an unfiltered one: Manufactor then Squirrels for all three") {
                val g = game(1 to "Test Squirrel General", 1 to "Test Manufactor", spell = "Test One Treasure")
                g.findPermanents("Treasure").size shouldBe 1
                g.findPermanents("Clue").size shouldBe 1
                g.findPermanents("Food").size shouldBe 1
                g.findPermanents("Squirrel").size shouldBe 3
            }
        }
    }
}
