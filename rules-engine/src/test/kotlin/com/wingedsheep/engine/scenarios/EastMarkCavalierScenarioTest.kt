package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.EastMarkCavalier
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * East-Mark Cavalier (LTR #9) — "Whenever this creature deals damage to a Goblin or Orc, destroy
 * that creature." Regression coverage for the shipped card that shares Spider-Slayer's shape: the
 * `RecipientFilter.Matching` deals-damage trigger repaired in `TriggerMatcher.matchesDealsDamageTrigger`.
 * Before that fix this ability silently never fired.
 *
 * The Goblin blocker is a 0/4 so the Cavalier survives and the Goblin survives the raw 2 combat
 * damage — proving it is the *trigger* (not lethal combat damage) that destroys it, and that a
 * non-Goblin/Orc is left alone.
 */
class EastMarkCavalierScenarioTest : FunSpec({

    val testGoblin = card("Test Goblin Blocker") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 0
        toughness = 4
    }
    val testBear = card("Test Bear Blocker") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 0
        toughness = 4
    }

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EastMarkCavalier, testGoblin, testBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("destroys a Goblin it deals combat damage to (even though the Goblin survives the damage)") {
        val (driver, you, opponent) = newGame()
        val cavalier = driver.putCreatureOnBattlefield(you, "East-Mark Cavalier") // 2/2
        driver.removeSummoningSickness(cavalier)
        val goblin = driver.putCreatureOnBattlefield(opponent, "Test Goblin Blocker") // 0/4

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(cavalier), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(goblin to listOf(cavalier)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // The Goblin took only 2 (survives a 4-toughness body on its own), but the trigger destroys it.
        driver.state.getBattlefield().contains(goblin) shouldBe false
        // The Cavalier took 0 and survives.
        driver.state.getBattlefield().contains(cavalier) shouldBe true
    }

    test("does not destroy a non-Goblin/Orc it deals combat damage to") {
        val (driver, you, opponent) = newGame()
        val cavalier = driver.putCreatureOnBattlefield(you, "East-Mark Cavalier") // 2/2
        driver.removeSummoningSickness(cavalier)
        val bear = driver.putCreatureOnBattlefield(opponent, "Test Bear Blocker") // 0/4, not a Goblin/Orc

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(cavalier), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(bear to listOf(cavalier)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // Non-Goblin/Orc: no destroy trigger; the 0/4 survives the 2 combat damage.
        driver.state.getBattlefield().contains(bear) shouldBe true
    }
})
