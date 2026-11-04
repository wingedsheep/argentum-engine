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
 * decline to target at all and never asks for consent. Argentum Assay's differential reported it.
 *
 * Two artifact cards go in the graveyard rather than one, deliberately: with a single legal target
 * the engine auto-selects it and no `ChooseTargetsDecision` is raised at all, so the requirement's
 * minimum — the thing that changed — would not be observable.
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

    test("the target is mandatory before resolution consent") {
        val d = driver()
        val (you, golem, myr) = d.castOverTwoArtifacts()


        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0].shouldNotBeNull() shouldContainAll listOf(golem, myr)
        // The half that fails when the consent is spelled as `optional = true` on the requirement:
        // that spelling makes this 0, i.e. "up to one target artifact card".
        decision.targetRequirements.single().minTargets shouldBe 1

        d.submitTargetSelection(you, listOf(golem))
        d.bothPass().error shouldBe null
        d.submitYesNo(you, true).error shouldBe null

        d.getHand(you) shouldContain golem
    }

    test("declining after target selection leaves both cards in the graveyard") {
        val d = driver()
        val (you, golem, myr) = d.castOverTwoArtifacts()

        d.submitTargetSelection(you, listOf(golem)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitYesNo(you, false).error shouldBe null

        d.getGraveyard(you) shouldContainAll listOf(golem, myr)
    }
})
