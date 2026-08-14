package com.wingedsheep.engine.event

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.DuskmournSet
import com.wingedsheep.mtg.sets.definitions.inv.InvasionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * [BattlefieldStaticsIndex] replaced three per-entity battlefield scans inside
 * `TriggerAbilityResolver` with one walk per detection pass. These pin what each bucket collects;
 * the ward behaviour itself is covered by the ward scenario tests.
 */
class BattlefieldStaticsIndexTest : FunSpec({

    fun mirrorMatch(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(InvasionSet.cards)
        driver.registerCards(DuskmournSet.cards)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("a board with no attachments and no ward statics indexes to EMPTY") {
        val driver = mirrorMatch()
        driver.putPermanentOnBattlefield(driver.activePlayer!!, "Forest")

        val index = BattlefieldStaticsIndex.build(driver.state, driver.cardRegistry)

        index shouldBe BattlefieldStaticsIndex.EMPTY
        index.attachmentsOn(driver.activePlayer!!) shouldHaveSize 0
    }

    test("attachments are indexed by the permanent they are attached to") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!

        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        val aura = driver.putCardInHand(player, "Fertile Ground")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, aura, listOf(forest))
        driver.bothPass()

        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe forest

        val index = BattlefieldStaticsIndex.build(driver.state, driver.cardRegistry)

        index.attachmentsOn(forest) shouldContain aura
        index.attachmentsOn(aura) shouldHaveSize 0
    }

    test("a ward suppressor is collected with the controller its filter reads as you") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!
        val nowhereToRun = driver.putPermanentOnBattlefield(player, "Nowhere to Run")

        val index = BattlefieldStaticsIndex.build(driver.state, driver.cardRegistry)

        index.wardSuppressors shouldHaveSize 1
        val suppressor = index.wardSuppressors.single()
        suppressor.sourceId shouldBe nowhereToRun
        suppressor.sourceControllerId shouldBe player
    }
})
