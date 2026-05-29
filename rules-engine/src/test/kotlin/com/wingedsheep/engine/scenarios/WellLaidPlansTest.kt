package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.WellLaidPlans
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Well-Laid Plans (INV #88) — Invasion engine gap #7, the relational
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.SharesColorWithRecipient] source predicate.
 *
 * "Prevent all damage that would be dealt to a creature by another creature if they share a color."
 */
class WellLaidPlansTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WellLaidPlans))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return Triple(driver, you, driver.getOpponent(you))
    }

    test("prevents combat damage between two creatures sharing a color") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(you, "Well-Laid Plans")

        // Two red creatures: Goblin Guide (attacker) vs Goblin Guide (blocker).
        val attacker = driver.putCreatureOnBattlefield(you, "Goblin Guide")
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(attacker), opponent)
        driver.bothPass()
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker)))
        driver.bothPass()
        driver.bothPass() // first strike step
        driver.bothPass() // combat damage

        // Both red → all combat damage between them prevented; both survive unscathed.
        (driver.state.getEntity(attacker)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (driver.state.getEntity(blocker)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        driver.getGraveyardCardNames(opponent).contains("Goblin Guide") shouldBe false
    }

    test("does not prevent damage between creatures of different colors") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(you, "Well-Laid Plans")

        // Red attacker vs white blocker — no shared color.
        val attacker = driver.putCreatureOnBattlefield(you, "Goblin Guide")   // R 2/1
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // W 1/1

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(attacker), opponent)
        driver.bothPass()
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker)))
        driver.bothPass()
        driver.bothPass() // first strike step
        driver.bothPass() // combat damage

        // 2 damage to the 1/1 Lions is not prevented — it dies.
        driver.getGraveyardCardNames(opponent).contains("Savannah Lions") shouldBe true
    }
})
