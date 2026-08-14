package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Flash Thompson, Spider-Fan (SPM #7) — {1}{W} Legendary Creature — Human Citizen, 2/2.
 *
 * "Flash
 *  When Flash Thompson enters, choose one or both —
 *  • Heckle — Tap target creature.
 *  • Hero Worship — Untap target creature."
 *
 * Exercises the "choose one or both" modal ETB triggered ability (`chooseCount = 2,
 * minChooseCount = 1`). A modal triggered ability picks its mode(s) and target(s) as it goes on
 * the stack (CR 603.3c), so the mode-selection loop offers a "Done" option once the minimum of
 * one mode is chosen. All three legal shapes are covered: Heckle only, Hero Worship only, and
 * both modes on two distinct creatures.
 */
class FlashThompsonSpiderFanScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Ally Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        /** After casting Flash Thompson, resolve it so its ETB modal trigger begins. */
        fun TestGame.castAndResolveFlash() {
            castSpell(1, "Flash Thompson, Spider-Fan").error shouldBe null
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
            resolveStack()
        }

        /** Choose the mode whose option text starts with [label]; returns nothing. */
        fun TestGame.chooseMode(label: String) {
            val decision = state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision for mode selection; got ${state.pendingDecision}")
            val index = decision.options.indexOfFirst { it.startsWith(label) }
            require(index >= 0) { "mode option '$label' not offered; options=${decision.options}" }
            submitDecision(OptionChosenResponse(decision.id, index))
        }

        /** Choose the trailing decline option to finalize mode selection after one mode. */
        fun TestGame.chooseDone() {
            val decision = state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision offering decline; got ${state.pendingDecision}")
            val index = decision.options.indexOf(ModalEffectExecutor.DECLINE_MODE_LABEL)
            require(index >= 0) { "decline option not offered; options=${decision.options}" }
            submitDecision(OptionChosenResponse(decision.id, index))
        }

        /** Answer the next per-mode target decision with [creature]. */
        fun TestGame.pickTarget(creature: EntityId) {
            val decision = state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${state.pendingDecision}")
            submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(creature))))
        }

        fun TestGame.isTapped(id: EntityId): Boolean =
            state.getEntity(id)?.has<TappedComponent>() == true

        context("Flash Thompson's ETB choose-one-or-both trigger") {

            test("Heckle only — taps target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flash Thompson, Spider-Fan")
                    .withCardOnBattlefield(2, "Ally Bear", tapped = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Ally Bear")!!
                game.castAndResolveFlash()

                game.chooseMode("Heckle")
                game.chooseDone()
                game.pickTarget(bear)
                game.resolveStack()

                withClue("Ally Bear should be tapped by the Heckle mode") {
                    game.isTapped(bear) shouldBe true
                }
            }

            test("Hero Worship only — untaps target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flash Thompson, Spider-Fan")
                    .withCardOnBattlefield(2, "Ally Bear", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Ally Bear")!!
                withClue("Ally Bear starts tapped") { game.isTapped(bear) shouldBe true }

                game.castAndResolveFlash()

                game.chooseMode("Hero Worship")
                game.chooseDone()
                game.pickTarget(bear)
                game.resolveStack()

                withClue("Ally Bear should be untapped by the Hero Worship mode") {
                    game.isTapped(bear) shouldBe false
                }
            }

            test("both modes — taps one creature and untaps another") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flash Thompson, Spider-Fan")
                    .withCardOnBattlefield(1, "Ally Bear", tapped = false)
                    .withCardOnBattlefield(2, "Ally Bear", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanents("Ally Bear")
                val untappedBear = bears.first { !game.isTapped(it) }
                val tappedBear = bears.first { game.isTapped(it) }

                game.castAndResolveFlash()

                // Pick both modes: Heckle (mode 0) then Hero Worship (mode 1). chooseCount is
                // reached at two picks, so no "Done" step — target selection follows in mode order.
                game.chooseMode("Heckle")
                game.chooseMode("Hero Worship")
                game.pickTarget(untappedBear) // Heckle taps this one
                game.pickTarget(tappedBear)   // Hero Worship untaps this one
                game.resolveStack()

                withClue("Heckle tapped the previously-untapped bear") {
                    game.isTapped(untappedBear) shouldBe true
                }
                withClue("Hero Worship untapped the previously-tapped bear") {
                    game.isTapped(tappedBear) shouldBe false
                }
                withClue("the two bears are distinct permanents") {
                    untappedBear shouldNotBe tappedBear
                }
            }
        }
    }
}
