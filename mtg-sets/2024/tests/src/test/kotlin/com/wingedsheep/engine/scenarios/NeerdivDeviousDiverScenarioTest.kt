package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Neerdiv, Devious Diver (J25 #36).
 *
 * Whenever Neerdiv becomes tapped, target player mills cards equal to its power.
 * Whenever you cast a spell from your graveyard or activate an ability of a card in your
 * graveyard, draw a card and put a +1/+1 counter on Neerdiv.
 */
class NeerdivDeviousDiverScenarioTest : ScenarioTestBase() {

    init {
        fun builder(): ScenarioBuilder {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Neerdiv, Devious Diver")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(2, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(6) { b = b.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island") }
            return b
        }

        fun ScenarioTestBase.TestGame.plusOneCounters(): Int {
            val neerdiv = findPermanent("Neerdiv, Devious Diver")!!
            return state.getEntity(neerdiv)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        }

        context("graveyard trigger") {
            test("flashing back a spell from your graveyard draws a card and grows Neerdiv") {
                val game = builder().withCardInGraveyard(1, "Think Twice").build()
                val thinkTwice = game.findCardsInGraveyard(1, "Think Twice").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = thinkTwice,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("flashback: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // One card from Neerdiv, one from Think Twice.
                game.handSize(1) shouldBe 2
                game.plusOneCounters() shouldBe 1
            }

            test("embalm exiles the card as a cost, and it still counts as a card in your graveyard") {
                val game = builder().withCardInGraveyard(1, "Sacred Cat").build()
                val cat = game.findCardsInGraveyard(1, "Sacred Cat").single()
                val embalm = cardRegistry.requireCard("Sacred Cat").activatedAbilities.single().id

                val activation = game.execute(ActivateAbility(playerId = game.player1Id, sourceId = cat, abilityId = embalm))
                withClue("embalm: ${activation.error}") { activation.error shouldBe null }
                game.isInExile(1, "Sacred Cat") shouldBe true
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.plusOneCounters() shouldBe 1
                game.findPermanents("Sacred Cat").size shouldBe 1
            }

            test("casting a spell from your hand does nothing") {
                val game = builder().withCardInHand(1, "Think Twice").build()

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.plusOneCounters() shouldBe 0
            }

            test("an opponent flashing back from their own graveyard does nothing") {
                val game = builder()
                    .withCardInGraveyard(2, "Think Twice")
                    .withActivePlayer(2)
                    .build()
                val thinkTwice = game.findCardsInGraveyard(2, "Think Twice").single()

                game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = thinkTwice,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.plusOneCounters() shouldBe 0
            }
        }

        context("tap trigger") {
            test("attacking taps Neerdiv and the target player mills cards equal to its power") {
                val game = builder().withCardInGraveyard(1, "Think Twice").build()
                // Grow Neerdiv to 3/3 first, so the mill reads its current power, not the printed 2.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInGraveyard(1, "Think Twice").single(),
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                ).error shouldBe null
                game.resolveStack()
                game.plusOneCounters() shouldBe 1
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Neerdiv, Devious Diver" to 2)).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.graveyardSize(2) shouldBe 3
                game.librarySize(2) shouldBe 3
            }
        }
    }
}
