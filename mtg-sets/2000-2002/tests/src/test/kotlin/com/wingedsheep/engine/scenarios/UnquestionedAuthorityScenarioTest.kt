package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jud.cards.UnquestionedAuthority
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unquestioned Authority — {2}{W} Enchantment — Aura
 *
 * "Enchant creature
 *  When this Aura enters, draw a card.
 *  Enchanted creature has protection from creatures."
 *
 * Protection is checked through real combat: creatures can't block the enchanted creature, and
 * damage a creature would deal to it is prevented.
 */
class UnquestionedAuthorityScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UnquestionedAuthority))
        return driver
    }

    fun GameTestDriver.castAuthorityOn(playerId: EntityId, creature: EntityId) {
        val aura = putCardInHand(playerId, "Unquestioned Authority")
        giveMana(playerId, Color.WHITE, 3)
        castSpell(playerId, aura, listOf(creature)).outcome shouldBe Outcome.Done
        bothPass() // resolve the Aura
        bothPass() // resolve the enters trigger
    }

    test("entering draws a card and attaches to the target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val handBefore = driver.getHandSize(me)
        driver.castAuthorityOn(me, lions)

        withClue("the Aura was added to and cast from hand, then the trigger drew one card") {
            driver.getHandSize(me) shouldBe handBefore + 1
        }
        driver.findPermanent(me, "Unquestioned Authority") shouldNotBe null
        driver.state.projectedState.hasKeyword(lions, "PROTECTION_FROM_CARDTYPE_CREATURE") shouldBe true
    }

    test("the enchanted creature can't be blocked by creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.removeSummoningSickness(attacker)
        val other = driver.putCreatureOnBattlefield(me, "Goblin Guide")
        driver.removeSummoningSickness(other)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.castAuthorityOn(me, attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker, other), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("protection from creatures: no creature can block it") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).outcome shouldNotBe Outcome.Done
        }
        withClue("an unenchanted attacker can still be blocked") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(other))).outcome shouldBe Outcome.Done
        }
    }

    test("damage dealt to the enchanted creature by a creature is prevented") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // I enchant the opponent's 1/1 (any creature is a legal host), which then blocks my 2/1.
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val attacker = driver.putCreatureOnBattlefield(me, "Goblin Guide")
        driver.removeSummoningSickness(attacker)
        driver.castAuthorityOn(me, lions)
        driver.findPermanent(me, "Unquestioned Authority") shouldNotBe null

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("protection doesn't stop the protected creature from blocking") {
            driver.declareBlockers(opponent, mapOf(lions to listOf(attacker))).outcome shouldBe Outcome.Done
        }
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        withClue("the 2 damage from Goblin Guide was prevented, so the 1/1 survives") {
            driver.findPermanent(opponent, "Savannah Lions") shouldBe lions
        }
        withClue("the Lions' 1 damage still kills the 2/1 Goblin Guide") {
            driver.findPermanent(me, "Goblin Guide") shouldBe null
        }
    }
})
