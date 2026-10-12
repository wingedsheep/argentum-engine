package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome

/**
 * Daring Archaeologist (DOM #13) — "When this creature enters, you may return target artifact card
 * from your graveyard to your hand."
 *
 * Proves where the "you may" lives. The consent is permission to perform the action; the *target* is
 * a requirement of the ability, and CR 603.3d routes a trigger's targets through CR 601.2c, which
 * requires a choice for each of them — only "up to one target" makes one optional. The card
 * used to spell the consent as `optional = true` on the target requirement, which is the SDK's
 * phrasing for "up to one target" — a strictly different ability, since it lets the controller
 * put the ability on the stack without a target. Argentum Assay's differential reported it.
 *
 * For this plain "you may <verb> target X" shape the engine takes the may-answer at the target
 * prompt: choosing a target is the yes (no second yes/no on resolution), and an empty selection
 * declines — the ability never goes on the stack. That is what tells it apart from "up to one",
 * where an empty selection would still put a targetless ability on the stack.
 *
 * Two artifact cards go in the graveyard rather than one, deliberately: with a single legal target
 * the engine auto-selects it and no `ChooseTargetsDecision` is raised at all, so the prompt would
 * not be observable.
 */
class DaringArchaeologistScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 30))
        return d
    }

    /** Cast the Archaeologist over two artifact cards in the graveyard, stopping at its ETB target choice. */
    fun GameTestDriver.castOverTwoArtifacts(): Triple<EntityId, EntityId, EntityId> {
        val you = player1
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val golem = putCardInGraveyard(you, "Artifact Creature")
        val myr = putCardInGraveyard(you, "Palladium Myr")
        val archaeologist = putCardInHand(you, "Daring Archaeologist")
        repeat(4) { putLandOnBattlefield(you, "Plains") }
        submit(
            CastSpell(
                playerId = you,
                cardId = archaeologist,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        bothPass() // resolve the creature and announce the trigger target
        return Triple(you, golem, myr)
    }

    test("choosing a target is the yes: the card returns with no second may-prompt") {
        val d = driver()
        val (you, golem, myr) = d.castOverTwoArtifacts()

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0].shouldNotBeNull() shouldContainAll listOf(golem, myr)
        // The target prompt carries the decline, so its minimum is zero.
        decision.targetRequirements.single().minTargets shouldBe 0

        d.submitTargetSelection(you, listOf(golem)).error shouldBe null
        d.bothPass().error shouldBe null

        d.pendingDecision shouldBe null
        d.getHand(you) shouldContain golem
    }

    test("declining at the target prompt keeps the ability off the stack and both cards in the graveyard") {
        val d = driver()
        val (you, golem, myr) = d.castOverTwoArtifacts()

        d.submitTargetSelection(you, emptyList()).error shouldBe null

        // An "up to one target" spelling would put a targetless ability on the stack here.
        d.stackSize shouldBe 0
        d.pendingDecision shouldBe null
        d.getGraveyard(you) shouldContainAll listOf(golem, myr)
    }
})
