package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Alesha, Who Smiles at Death (FRF #90) — {2}{R} Legendary Creature — Human
 * Warrior, 3/2.
 *
 *   First strike
 *   Whenever Alesha attacks, you may pay {W/B}{W/B}. If you do, return target creature card with
 *   power 2 or less from your graveyard to the battlefield tapped and attacking.
 *
 * Covers the power-2-or-less graveyard target, the hybrid may-pay gate wrapped around the
 * tapped-and-attacking return pipeline, and declining the payment.
 */
class AleshaWhoSmilesAtDeathScenarioTest : ScenarioTestBase() {

    init {
        context("Alesha, Who Smiles at Death") {

            test("paying {W/B}{W/B} returns a power-2-or-less creature card tapped and attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Alesha, Who Smiles at Death", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears") // 2/2 — legal
                    .withCardInGraveyard(1, "Craw Wurm")     // 6/4 — power too high
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Alesha, Who Smiles at Death" to 2)).error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only the creature card with power 2 or less is a legal target") {
                    decision.legalTargets[0]!! shouldContainExactly listOf(bears)
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the trigger offers the optional {W/B}{W/B} payment on resolution") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears returns tapped and attacking") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(bears)?.has<AttackingComponent>() shouldBe true
                }
                withClue("Craw Wurm stays in the graveyard") {
                    game.isInGraveyard(1, "Craw Wurm") shouldBe true
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("Alesha (3) and the returned Bears (2) both dealt combat damage") {
                    game.getLifeTotal(2) shouldBe 15
                }
            }

            test("declining the payment leaves the target in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Alesha, Who Smiles at Death", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Alesha, Who Smiles at Death" to 2)).error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(bears))
                    game.resolveStack()
                }

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("nothing returns when the payment is declined") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
