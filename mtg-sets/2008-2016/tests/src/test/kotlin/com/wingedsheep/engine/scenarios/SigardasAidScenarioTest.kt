package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sigarda's Aid — {W} Enchantment.
 *
 * "You may cast Aura and Equipment spells as though they had flash.
 *  Whenever an Equipment you control enters, you may attach it to target creature you control."
 *
 * Proves the Aura-or-Equipment flash grant (on the opponent's turn, where only flash allows a cast)
 * and that the trigger's "it" binds to the entering Equipment — the attachment comes from the
 * triggering entity, not from Sigarda's Aid itself.
 */
class SigardasAidScenarioTest : ScenarioTestBase() {

    /** Opponent's main phase, Player1 holding priority, with a creature and two Plains. */
    private fun onOpponentsTurn(withAid: Boolean = true) = scenario()
        .withPlayers("Player1", "Player2")
        .apply { if (withAid) withCardOnBattlefield(1, "Sigarda's Aid") }
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    /** Resolve the stack, choosing [host] for the trigger's target and answering the may with [attach]. */
    private fun TestGame.resolveAttachTrigger(host: EntityId, attach: Boolean) {
        repeat(12) {
            when (state.pendingDecision) {
                is ChooseTargetsDecision -> selectTargets(listOf(host)).error shouldBe null
                is YesNoDecision -> answerYesNo(attach).error shouldBe null
                null -> if (state.stack.isNotEmpty()) passPriority() else return
                else -> error("Unexpected decision ${state.pendingDecision}")
            }
        }
    }

    init {
        test("an Equipment cast at instant speed can be attached to a creature you control as it enters") {
            val game = onOpponentsTurn().withCardInHand(1, "Bonesplitter").build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Bonesplitter").error shouldBe null
            game.resolveAttachTrigger(bears, attach = true)

            val bonesplitter = game.findPermanent("Bonesplitter")!!
            game.state.getEntity(bonesplitter)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 4 // 2 + Bonesplitter's +2
        }

        test("declining the may leaves the Equipment unattached") {
            val game = onOpponentsTurn().withCardInHand(1, "Bonesplitter").build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Bonesplitter").error shouldBe null
            game.resolveAttachTrigger(bears, attach = false)

            val bonesplitter = game.findPermanent("Bonesplitter")!!
            game.state.getEntity(bonesplitter)?.get<AttachedToComponent>() shouldBe null
        }

        test("an Aura can be cast at instant speed") {
            val game = onOpponentsTurn().withCardInHand(1, "Holy Strength").build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Holy Strength", bears).error shouldBe null
            game.resolveStack()

            val aura = game.findPermanent("Holy Strength")!!
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
        }

        test("without Sigarda's Aid an Equipment can't be cast on the opponent's turn") {
            val game = onOpponentsTurn(withAid = false).withCardInHand(1, "Bonesplitter").build()

            game.castSpell(1, "Bonesplitter").error shouldNotBe null
        }

        test("other spells don't gain flash") {
            val game = onOpponentsTurn().withCardInHand(1, "Savannah Lions").build()

            game.castSpell(1, "Savannah Lions").error shouldNotBe null
        }
    }
}
