package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.FeedThePack
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feed the Pack (DKA #114) — {5}{G} Enchantment.
 *
 *   At the beginning of your end step, you may sacrifice a nontoken creature. If you do, create X
 *   2/2 green Wolf creature tokens, where X is the sacrificed creature's toughness.
 *
 * Pins the parts the script can't show on its own: the sacrificed creature is *chosen* at
 * resolution (a paused selection inside the may-pay gate), X is its toughness as it last existed
 * on the battlefield (a Giant Growth bonus counts), only nontoken creatures qualify, and declining
 * makes nothing.
 */
class FeedThePackScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FeedThePack))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    fun wolves(driver: GameTestDriver, player: EntityId): Int =
        driver.getCreatures(player).count { id ->
            val entity = driver.state.getEntity(id)
            entity?.get<TokenComponent>() != null &&
                entity.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Wolf" } == true
        }

    /** Resolve the end-step trigger, answering the may with [pay] and picking [sacrifice]. */
    fun resolveTrigger(driver: GameTestDriver, you: EntityId, pay: Boolean, sacrifice: EntityId?) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val decision = driver.pendingDecision) {
                null -> driver.bothPass()
                is YesNoDecision -> driver.submitYesNo(you, pay)
                is SelectCardsDecision -> driver.submitCardSelection(you, listOfNotNull(sacrifice))
                else -> error("unexpected decision $decision")
            }
        }
    }

    test("sacrificing a chosen creature creates Wolves equal to its last-known toughness") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Feed the Pack")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val guide = driver.putCreatureOnBattlefield(you, "Goblin Guide") // 2/1

        // Giant Growth the Goblin Guide: 5/4 until end of turn, still in force at the end step.
        val growth = driver.putCardInHand(you, "Giant Growth")
        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, growth, listOf(guide)).error shouldBe null
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        resolveTrigger(driver, you, pay = true, sacrifice = guide)

        withClue("Goblin Guide was sacrificed, Centaur Courser stays") {
            driver.findPermanent(you, "Goblin Guide") shouldBe null
            driver.getCreatures(you).contains(courser) shouldBe true
        }
        withClue("X is the pumped toughness 4, not the printed 1 or the power 5") {
            wolves(driver, you) shouldBe 4
        }
    }

    test("declining the sacrifice creates nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Feed the Pack")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.passPriorityUntil(Step.END)
        resolveTrigger(driver, you, pay = false, sacrifice = null)

        driver.getCreatures(you) shouldBe listOf(courser)
        wolves(driver, you) shouldBe 0
    }

    test("tokens can't be sacrificed — with only Wolf tokens nothing is asked or made") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Feed the Pack")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        // First end step: make three Wolves from the Courser.
        driver.passPriorityUntil(Step.END)
        resolveTrigger(driver, you, pay = true, sacrifice = courser)
        wolves(driver, you) shouldBe 3

        // Next end step of yours: only tokens remain, so the gate is unaffordable — no prompt.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.activePlayer shouldBe you
        var guard = 0
        while (guard++ < 8 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            driver.pendingDecision shouldBe null
            driver.bothPass()
        }
        wolves(driver, you) shouldBe 3
    }
})
