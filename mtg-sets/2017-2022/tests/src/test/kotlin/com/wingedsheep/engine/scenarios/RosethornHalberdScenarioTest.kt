package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.RosethornHalberd
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rosethorn Halberd (ELD #175) — {G} Artifact — Equipment.
 *
 * "When this Equipment enters, attach it to target non-Human creature you control.
 *  Equipped creature gets +2/+1. Equip {5}"
 */
class RosethornHalberdScenarioTest : FunSpec({

    val testHuman = card("Test Human") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RosethornHalberd, testHuman))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("ETB attaches to a non-Human creature you control for +2/+1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val halberd = driver.putCardInHand(me, "Rosethorn Halberd")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, halberd)
        driver.bothPass()
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(bears))
        driver.resolveAll()

        val halberdId = driver.findPermanent(me, "Rosethorn Halberd")!!
        driver.state.getEntity(halberdId)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 3
    }

    test("a Human isn't a legal ETB target, but Equip {5} can still attach to it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val human = driver.putCreatureOnBattlefield(me, "Test Human")

        val halberd = driver.putCardInHand(me, "Rosethorn Halberd")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, halberd)
        driver.bothPass()
        driver.resolveAll()

        val halberdId = driver.findPermanent(me, "Rosethorn Halberd")!!
        driver.state.getEntity(halberdId)?.get<AttachedToComponent>() shouldBe null

        val equipId = RosethornHalberd.activatedAbilities.single { it.isEquipAbility }.id
        driver.giveColorlessMana(me, 5)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = halberdId,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(human)),
            )
        ).outcome shouldBe Outcome.Done
        driver.resolveAll()

        driver.state.getEntity(halberdId)?.get<AttachedToComponent>()?.targetId shouldBe human
        driver.state.projectedState.getPower(human) shouldBe 4
        driver.state.projectedState.getToughness(human) shouldBe 3
    }
})
