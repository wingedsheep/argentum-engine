package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AtKnifepoint
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * At Knifepoint — {1}{B}{R} Enchantment.
 *
 * "During your turn, outlaws you control have first strike."
 * "Whenever you commit a crime, create a 1/1 red Mercenary creature token ... This ability
 *  triggers only once each turn."
 *
 * Ragavan, Nimble Pilferer (a Monkey Pirate, hence an outlaw) is the test outlaw.
 */
class OtjAtKnifepointScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AtKnifepoint)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.commitCrime(caster: EntityId, opponent: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, targets = listOf(opponent))
        bothPass() // resolve Bolt -> commit-crime -> token trigger on stack
        bothPass() // resolve token trigger
    }

    test("outlaws you control have first strike during your turn, not an opponent's outlaw") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "At Knifepoint")

        val myPirate = driver.putCreatureOnBattlefield(me, "Ragavan, Nimble Pilferer")
        val oppPirate = driver.putCreatureOnBattlefield(opp, "Ragavan, Nimble Pilferer")

        // It's my turn: my outlaw gains first strike, the opponent's does not (not mine).
        driver.state.projectedState.hasKeyword(myPirate, Keyword.FIRST_STRIKE) shouldBe true
        driver.state.projectedState.hasKeyword(oppPirate, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("the grant turns off when it is no longer your turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "At Knifepoint")
        val myPirate = driver.putCreatureOnBattlefield(me, "Ragavan, Nimble Pilferer")

        driver.state.projectedState.hasKeyword(myPirate, Keyword.FIRST_STRIKE) shouldBe true

        // Pass to the opponent's turn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe driver.getOpponent(me)

        driver.state.projectedState.hasKeyword(myPirate, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("committing a crime creates a Mercenary token, but only once each turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "At Knifepoint")

        val before = driver.state.getZone(ZoneKey(me, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)).size

        driver.commitCrime(me, opp)
        val afterFirst = driver.state.getZone(ZoneKey(me, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)).size
        afterFirst shouldBe before + 1
        driver.findPermanent(me, "Mercenary Token")?.let { driver.getCardName(it) } shouldBe "Mercenary Token"

        // A second crime the same turn must NOT make another token (once per turn).
        driver.commitCrime(me, opp)
        driver.state.getZone(ZoneKey(me, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)).size shouldBe afterFirst
    }
})
