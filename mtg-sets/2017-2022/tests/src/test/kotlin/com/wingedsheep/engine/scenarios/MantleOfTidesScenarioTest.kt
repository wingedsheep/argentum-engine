package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.eld.cards.MantleOfTides
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mantle of Tides (ELD #52) — {U} Artifact — Equipment.
 *
 * "Equipped creature gets +1/+2.
 *  Whenever you draw your second card each turn, attach this Equipment to target creature you control.
 *  Equip {3}"
 */
class MantleOfTidesScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MantleOfTides, Divination))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.attachedTo(mantle: EntityId): EntityId? =
        state.getEntity(mantle)?.get<AttachedToComponent>()?.targetId

    fun GameTestDriver.castDivination(player: EntityId) {
        val divination = putCardInHand(player, "Divination")
        giveMana(player, Color.BLUE, 1)
        giveColorlessMana(player, 2)
        castSpell(player, divination)
        bothPass() // Divination resolves; the second draw fires the trigger
    }

    test("drawing the second card of the turn attaches the Mantle to the chosen creature for +1/+2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val mantle = driver.putPermanentOnBattlefield(me, "Mantle of Tides")
        driver.attachedTo(mantle) shouldBe null

        driver.castDivination(me)
        driver.submitTargetSelection(me, listOf(bears))
        driver.resolveAll()

        driver.attachedTo(mantle) shouldBe bears
        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 4
    }

    test("the trigger moves the Mantle off its current creature, and fires only on the second draw") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val first = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val mantle = driver.putPermanentOnBattlefield(me, "Mantle of Tides")

        val equipId = MantleOfTides.activatedAbilities.single { it.isEquipAbility }.id
        driver.giveColorlessMana(me, 3)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = mantle,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(first)),
            )
        ).outcome shouldBe Outcome.Done
        driver.resolveAll()
        driver.attachedTo(mantle) shouldBe first

        driver.castDivination(me)
        driver.submitTargetSelection(me, listOf(second))
        driver.resolveAll()

        driver.attachedTo(mantle) shouldBe second
        driver.state.projectedState.getPower(first) shouldBe 2
        driver.state.projectedState.getToughness(first) shouldBe 2
        driver.state.projectedState.getPower(second) shouldBe 3
        driver.state.projectedState.getToughness(second) shouldBe 4

        // Draws three and four this turn don't trigger it again.
        driver.castDivination(me)
        driver.state.stack.isEmpty() shouldBe true
        driver.pendingDecision shouldBe null
        driver.attachedTo(mantle) shouldBe second
    }
})
