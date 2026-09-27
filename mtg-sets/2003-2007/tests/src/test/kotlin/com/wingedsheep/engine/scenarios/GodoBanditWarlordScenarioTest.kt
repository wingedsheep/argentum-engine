package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Godo, Bandit Warlord (CHK #169).
 *
 *   When Godo enters, you may search your library for an Equipment card, put it onto the
 *   battlefield, then shuffle.
 *   Whenever Godo attacks for the first time each turn, untap it and all Samurai you control.
 *   After this phase, there is an additional combat phase.
 */
class GodoBanditWarlordScenarioTest : ScenarioTestBase() {

    init {
        context("Godo, Bandit Warlord") {

            test("entering searches for an Equipment and puts it onto the battlefield") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Godo, Bandit Warlord")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Bonesplitter")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Godo, Bandit Warlord").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val bonesplitter = game.findCardsInLibrary(1, "Bonesplitter").single()
                withClue("only the Equipment is offered, not the creature") {
                    decision.options shouldBe listOf(bonesplitter)
                }
                game.selectCards(listOf(bonesplitter))
                game.resolveStack()

                game.isOnBattlefield("Bonesplitter") shouldBe true
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }

            test("first attack untaps Godo and Samurai, not others, and adds one combat phase") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Godo, Bandit Warlord")
                    .withCardOnBattlefield(1, "Kitsune Blademaster")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val godo = game.findPermanent("Godo, Bandit Warlord")!!
                val samurai = game.findPermanent("Kitsune Blademaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Godo, Bandit Warlord" to 2, "Kitsune Blademaster" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("Godo and the Samurai untap; the non-Samurai stays tapped") {
                    game.state.getEntity(godo)!!.has<TappedComponent>() shouldBe false
                    game.state.getEntity(samurai)!!.has<TappedComponent>() shouldBe false
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
                withClue("exactly one additional combat phase is queued") {
                    val queued = game.state.getEntity(game.state.activePlayerId!!)
                        ?.get<AdditionalPhasesComponent>()?.phases.orEmpty()
                    queued.size shouldBe 1
                    queued.single().kind shouldBe ExtraPhaseKind.COMBAT
                }

                // Into the additional combat: attacking again is not the first time this turn.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.step shouldBe Step.DECLARE_ATTACKERS
                game.declareAttackers(mapOf("Godo, Bandit Warlord" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the second attack does not trigger — Godo stays tapped, no new phase") {
                    game.state.getEntity(godo)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(game.state.activePlayerId!!)
                        ?.get<AdditionalPhasesComponent>()?.phases.orEmpty().size shouldBe 0
                }
            }
        }
    }
}
