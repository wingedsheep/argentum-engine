package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.UndercoverOperative
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Undercover Operative (SNC #63) — {2}{U}{U} Creature — Shapeshifter Rogue 0/0.
 *
 *   You may have this creature enter as a copy of any creature on the battlefield, except it
 *   enters with a shield counter on it if you control that creature.
 *
 * Pins the conditional copy rider: the shield counter arrives only when the copied creature is
 * one you control, never when copying an opponent's, and declining the copy leaves a 0/0 that dies.
 */
class UndercoverOperativeScenarioTest : FunSpec({

    fun shields(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId) =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0

    test("copying a creature you control enters with a shield counter") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UndercoverOperative)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val mine = driver.putCreatureOnBattlefield(player, "Elvish Warrior")
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        val operative = driver.putCardInHand(player, "Undercover Operative")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, operative).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainAll listOf(mine, theirs)
        driver.submitCardSelection(player, listOf(mine)).error shouldBe null

        driver.state.getEntity(operative)?.get<CardComponent>()?.name shouldBe "Elvish Warrior"
        shields(driver, operative) shouldBe 1
    }

    test("copying an opponent's creature enters without a shield counter") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UndercoverOperative)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        val operative = driver.putCardInHand(player, "Undercover Operative")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, operative).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(theirs)).error shouldBe null

        driver.state.getEntity(operative)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
        driver.state.getBattlefield().contains(operative) shouldBe true
        shields(driver, operative) shouldBe 0
    }

    test("declining the copy enters as a 0/0 with no shield and it dies") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UndercoverOperative)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "Elvish Warrior")
        val operative = driver.putCardInHand(player, "Undercover Operative")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, operative).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, emptyList()).error shouldBe null

        driver.state.getBattlefield() shouldNotContain operative
    }
})
