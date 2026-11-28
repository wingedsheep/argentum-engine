package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Altar of the Lost (DKA) — enters tapped; its two mana in any combination of colors may be spent
 * only on spells with flashback cast from a graveyard (`AllOf(SpellsWithKeywordOnly(FLASHBACK),
 * CastFromGraveyardOnly(anyGraveyard = true))`). It pays a flashback cast, but not the same
 * flashback card cast from hand. Flashback granted by Snapcaster Mage counts as "with flashback".
 */
class AltarOfTheLostScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .also { b -> repeat(5) { b.withCardInLibrary(1, "Island"); b.withCardInLibrary(2, "Island") } }

    init {
        context("Altar of the Lost") {

            test("it enters the battlefield tapped") {
                val game = base()
                    .withCardInHand(1, "Altar of the Lost")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .build()

                val result = game.castSpell(1, "Altar of the Lost")
                withClue("error=${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val altar = game.findPermanent("Altar of the Lost")
                altar shouldNotBe null
                game.state.getEntity(altar!!)?.get<TappedComponent>() shouldNotBe null
            }

            test("its mana pays a flashback cast from the graveyard") {
                val game = base()
                    .withCardOnBattlefield(1, "Altar of the Lost")
                    .withCardInGraveyard(1, "Otherworldly Gaze")
                    .build()

                val gaze = game.findCardsInGraveyard(1, "Otherworldly Gaze").single()
                withClue("flashback {1}{U} is affordable off the Altar alone") {
                    game.getLegalActions(1).single {
                        it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == gaze
                    }.isAffordable shouldBe true
                }

                val result = game.execute(
                    CastSpell(
                        game.player1Id, gaze,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.state.getEntity(game.findPermanent("Altar of the Lost")!!)?.get<TappedComponent>() shouldNotBe null
                game.state.stack.size shouldBe 1
            }

            test("its mana pays a flashback Snapcaster Mage granted") {
                val game = base()
                    .withCardOnBattlefield(1, "Altar of the Lost")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Snapcaster Mage")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .build()
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                withClue("Lightning Bolt has no printed flashback, so nothing offers it yet") {
                    game.getLegalActions(1).none {
                        it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == bolt
                    } shouldBe true
                }

                game.castSpell(1, "Snapcaster Mage").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.selectTargets(listOf(bolt)).error shouldBe null
                game.resolveStack()

                withClue("the granted flashback {R} is affordable off the Altar alone (the Islands are tapped)") {
                    game.getLegalActions(1).single {
                        it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == bolt
                    }.isAffordable shouldBe true
                }
                val result = game.execute(
                    CastSpell(
                        game.player1Id, bolt,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.state.getEntity(game.findPermanent("Altar of the Lost")!!)?.get<TappedComponent>() shouldNotBe null
            }

            test("its mana can't pay for a flashback card cast from hand") {
                val game = base()
                    .withCardOnBattlefield(1, "Altar of the Lost")
                    .withCardInHand(1, "Otherworldly Gaze")
                    .build()

                withClue("Otherworldly Gaze has flashback, but cast from hand it isn't cast from a graveyard") {
                    game.castSpell(1, "Otherworldly Gaze").error shouldNotBe null
                }
                game.isInHand(1, "Otherworldly Gaze") shouldBe true
            }
        }
    }
}
