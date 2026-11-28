package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.SoulSeizer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Soul Seizer // Ghastly Haunting (DKA #50)
 *
 * A creature that transforms into an Aura and attaches itself to the target its trigger announced:
 * "you may transform it. If you do, attach it to target creature that player controls." Covers the
 * happy path (control of the enchanted creature), declining, the target becoming illegal (the ability
 * doesn't resolve, so no transform), and Soul Seizer leaving in response (no transform, no attach).
 */
class SoulSeizerScenarioTest : FunSpec({

    data class Board(
        val driver: GameTestDriver,
        val attacker: EntityId,
        val defender: EntityId,
        val seizer: EntityId,
        val bears: EntityId,
    )

    fun GameTestDriver.faceName(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    /** Attack with Soul Seizer unblocked, and announce Grizzly Bears as the trigger's target. */
    fun connectAndTarget(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SoulSeizer)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val seizer = driver.putCreatureOnBattlefield(attacker, "Soul Seizer")
        driver.removeSummoningSickness(seizer)
        val bears = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(seizer), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()
        driver.bothPass()

        driver.currentStep shouldBe Step.COMBAT_DAMAGE
        driver.assertLifeTotal(defender, 19)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(attacker, listOf(bears)).error shouldBe null
        driver.stackSize shouldBe 1
        return Board(driver, attacker, defender, seizer, bears)
    }

    test("transforming attaches Ghastly Haunting to the target and steals it") {
        val (d, attacker, _, seizer, bears) = connectAndTarget()
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(attacker, true).error shouldBe null

        withClue("Soul Seizer transformed into an Aura on the battlefield") {
            d.faceName(seizer) shouldBe "Ghastly Haunting"
            d.state.projectedState.isCreature(seizer) shouldBe false
            d.getController(seizer) shouldBe attacker
        }
        withClue("attached to the targeted creature") {
            d.state.getEntity(seizer)!!.get<AttachedToComponent>()!!.targetId shouldBe bears
            d.state.getEntity(bears)!!.get<AttachmentsComponent>()!!.attachedIds.contains(seizer) shouldBe true
        }
        withClue("you control enchanted creature") {
            d.state.projectedState.getController(bears) shouldBe attacker
        }
    }

    test("declining leaves Soul Seizer an unattached creature") {
        val (d, attacker, defender, seizer, bears) = connectAndTarget()
        d.bothPass()
        d.submitYesNo(attacker, false).error shouldBe null

        d.faceName(seizer) shouldBe "Soul Seizer"
        d.state.projectedState.isCreature(seizer) shouldBe true
        d.state.getEntity(seizer)!!.has<AttachedToComponent>() shouldBe false
        d.state.projectedState.getController(bears) shouldBe defender
    }

    test("an illegal target means the ability doesn't resolve and Soul Seizer can't transform") {
        val (d, _, _, seizer, bears) = connectAndTarget()
        d.moveToGraveyard(bears)
        d.bothPass()

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.stackSize shouldBe 0
        d.faceName(seizer) shouldBe "Soul Seizer"
        d.state.projectedState.isCreature(seizer) shouldBe true
    }

    test("Soul Seizer leaving in response is neither transformed nor attached") {
        val (d, attacker, defender, seizer, bears) = connectAndTarget()
        d.moveToGraveyard(seizer)
        d.bothPass()

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.stackSize shouldBe 0
        d.getGraveyard(attacker).contains(seizer) shouldBe true
        d.faceName(seizer) shouldBe "Soul Seizer"
        d.state.getEntity(seizer)!!.has<AttachedToComponent>() shouldBe false
        d.state.projectedState.getController(bears) shouldBe defender
    }
})
