package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.OakhameAdversary
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Oakhame Adversary (ELD #167) — {3}{G} Creature — Elf Warrior 2/3.
 *
 * "This spell costs {2} less to cast if an opponent controls a green permanent.
 *  Deathtouch
 *  Whenever this creature deals combat damage to a player, draw a card."
 */
class OakhameAdversaryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OakhameAdversary))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("costs {2} less while an opponent controls a green permanent") {
        val driver = createDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val adversary = driver.putCardInHand(me, "Oakhame Adversary")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, adversary).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        val permanent = driver.findPermanent(me, "Oakhame Adversary")
        permanent shouldNotBe null
        driver.state.projectedState.hasKeyword(permanent!!, Keyword.DEATHTOUCH) shouldBe true
    }

    test("no reduction when the opponent's permanents aren't green, or only you control green") {
        val driver = createDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Black Creature")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val adversary = driver.putCardInHand(me, "Oakhame Adversary")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, adversary).error shouldNotBe null
    }

    test("draws a card when it deals combat damage to a player") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val adversary = driver.putCreatureOnBattlefield(me, "Oakhame Adversary")
        driver.removeSummoningSickness(adversary)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(me)
        driver.declareAttackers(me, mapOf(adversary to opponent)).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getHandSize(me) shouldBe handBefore + 1
    }
})
