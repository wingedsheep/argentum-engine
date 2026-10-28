package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Redoubled Stormsinger — "Whenever this creature attacks, for each creature token you control
 * that entered this turn, create a tapped and attacking token that's a copy of that token. At the
 * beginning of the next end step, sacrifice those tokens."
 *
 * Covers the per-token copy (only tokens that entered this turn — two Soldiers from an earlier
 * turn stay uncopied — each copied once — the fresh
 * copies aren't re-iterated), the copies entering tapped and attacking, and the delayed sacrifice
 * removing exactly the copies.
 */
class RedoubledStormsingerScenarioTest : FunSpec({

    fun GameTestDriver.soldierTokens(playerId: EntityId): List<EntityId> =
        getPermanents(playerId).filter { id ->
            val entity = state.getEntity(id)
            entity?.has<TokenComponent>() == true && entity.get<CardComponent>()?.name == "Soldier Token"
        }

    test("copies each creature token that entered this turn, tapped and attacking, then sacrifices the copies") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val stormsinger = driver.putCreatureOnBattlefield(attacker, "Redoubled Stormsinger")
        driver.removeSummoningSickness(stormsinger)

        fun raiseTheAlarm() {
            val alarm = driver.putCardInHand(attacker, "Raise the Alarm")
            driver.giveMana(attacker, Color.WHITE, 2)
            driver.castSpell(attacker, alarm)
            driver.bothPass()
        }

        // Two Soldiers made on an earlier turn: still creature tokens, but they didn't enter this turn.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        raiseTheAlarm()
        val oldTokens = driver.soldierTokens(attacker)
        oldTokens.size shouldBe 2

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, attacker)
        raiseTheAlarm()

        val originals = driver.soldierTokens(attacker)
        originals.size shouldBe 4

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(stormsinger), defender)
        driver.bothPass() // resolve the attack trigger

        val copies = driver.soldierTokens(attacker) - originals.toSet()
        withClue("One copy per token that entered this turn (not the earlier-turn ones), and no copies of the copies") {
            copies.size shouldBe 2
        }
        copies.forEach { copy ->
            withClue("Each copy enters tapped and attacking") {
                driver.state.getEntity(copy)?.has<TappedComponent>() shouldBe true
                driver.state.getEntity(copy)?.get<AttackingComponent>()?.defenderId shouldBe defender
            }
        }
        originals.forEach { original ->
            withClue("The original tokens weren't declared as attackers") {
                driver.state.getEntity(original)?.has<AttackingComponent>() shouldBe false
            }
        }

        driver.passPriorityUntil(Step.END)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        withClue("Only the copies are sacrificed at the next end step") {
            driver.soldierTokens(attacker).toSet() shouldBe originals.toSet()
        }
    }
})
