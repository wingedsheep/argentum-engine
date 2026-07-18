package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.TwistsAndTurns
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Twists and Turns // Mycoid Maze (LCI #217).
 *
 * Front — Twists and Turns: "If a creature you control would explore, instead you scry 1, then
 * that creature explores." + ETB "target creature you control explores" + "when a land you control
 * enters, if you control seven or more lands, transform".
 *
 * The headline feature is the [com.wingedsheep.sdk.scripting.ModifyExplore] replacement (CR 614).
 * The discriminating test seeds a nonland on top of the library and a land beneath it: the
 * replacement's scry 1 lets the controller bottom the nonland so the explore that follows reveals
 * the land (→ hand, no +1/+1 counter). Without the replacement the explore would reveal the nonland
 * and add a counter — so the land-in-hand + no-counter outcome proves scry ran before the explore.
 */
class TwistsAndTurnsScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TwistsAndTurns))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("the explore replacement scries 1 before the ETB explore, reordering what is revealed") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(p1, "Savannah Lions")

        // Top of library: a nonland, then a land beneath it. Prepend the land first so the
        // nonland ends up on top.
        driver.putCardOnTopOfLibrary(p1, "Forest")
        val bearsCard = driver.putCardOnTopOfLibrary(p1, "Grizzly Bears") // nonland, now on top

        val twists = driver.putCardInHand(p1, "Twists and Turns")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.castSpell(p1, twists).isSuccess shouldBe true

        var sawScry = false
        var guard = 0
        while (guard++ < 60) {
            val d = driver.pendingDecision
            when {
                // ETB target: the lone creature you control.
                d is ChooseTargetsDecision -> driver.submitTargetSelection(p1, listOf(bear))
                // The replacement's scry 1 surfaces first (top card = Grizzly Bears); bottom it so
                // the explore reveals the Forest underneath. The explore's own nonland graveyard
                // decision never appears because the explore reveals a land.
                d is SelectCardsDecision -> {
                    sawScry = true
                    driver.submitCardSelection(p1, listOf(bearsCard)) // put the nonland on the bottom
                }
                d != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        // The scry decision surfaced (the replacement fired) ...
        sawScry shouldBe true
        // ... it reordered so the explore revealed the LAND → Forest went to hand ...
        driver.getHand(p1).any { cardName(driver, it) == "Forest" } shouldBe true
        // ... and because a land was explored, no +1/+1 counter landed on the creature.
        plusOne(driver, bear) shouldBe 0
    }

    test("transforms into Mycoid Maze when a land enters and you control seven or more lands") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val twists = driver.putPermanentOnBattlefield(p1, "Twists and Turns")
        // Six lands already out; playing a seventh triggers the land-ETB intervening-if.
        repeat(6) { driver.putLandOnBattlefield(p1, "Forest") }

        val seventh = driver.putCardInHand(p1, "Forest")
        driver.playLand(p1, seventh).isSuccess shouldBe true
        var guard = 0
        while (guard++ < 20 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }

        cardName(driver, twists) shouldBe "Mycoid Maze"
    }

    test("does not transform when fewer than seven lands are controlled") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val twists = driver.putPermanentOnBattlefield(p1, "Twists and Turns")
        repeat(4) { driver.putLandOnBattlefield(p1, "Forest") }

        val fifth = driver.putCardInHand(p1, "Forest")
        driver.playLand(p1, fifth).isSuccess shouldBe true
        var guard = 0
        while (guard++ < 20 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }

        cardName(driver, twists) shouldBe "Twists and Turns"
    }
})
