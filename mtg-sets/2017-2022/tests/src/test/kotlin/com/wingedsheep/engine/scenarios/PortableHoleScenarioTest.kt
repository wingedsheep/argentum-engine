package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PortableHole
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Portable Hole (AFR #33) — {W} Artifact.
 *
 * "When this artifact enters, exile target nonland permanent an opponent controls with mana value 2
 *  or less until this artifact leaves the battlefield."
 */
class PortableHoleScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PortableHole)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isInExile(playerId: com.wingedsheep.sdk.model.EntityId, name: String): Boolean =
        state.getExile(playerId).any { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("exiles an opponent's mana value 2 permanent until Portable Hole leaves the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(opponent, "Glory Seeker") // {1}{W}, mana value 2
        val seeker = driver.findPermanent(opponent, "Glory Seeker")!!

        val hole = driver.putCardInHand(me, "Portable Hole")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, hole)
        driver.bothPass() // resolve the spell -> it enters -> ETB trigger
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(me, listOf(seeker))
        driver.bothPass() // resolve the ETB trigger

        driver.findPermanent(opponent, "Glory Seeker") shouldBe null
        driver.isInExile(opponent, "Glory Seeker") shouldBe true

        // Shatter the Hole: the exiled card returns under its owner's control.
        val holeId = driver.findPermanent(me, "Portable Hole")!!
        val shatter = driver.putCardInHand(me, "Shatter")
        driver.giveMana(me, Color.RED, 2)
        driver.castSpellWithTargets(me, shatter, listOf(ChosenTarget.Permanent(holeId)))
        driver.bothPass() // resolve Shatter
        driver.bothPass() // resolve the leaves-the-battlefield trigger

        driver.findPermanent(me, "Portable Hole") shouldBe null
        driver.findPermanent(opponent, "Glory Seeker") shouldNotBe null
    }

    test("an opponent's mana value 4 permanent is not a legal target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(opponent, "Hill Giant") // {3}{R}, mana value 4

        val hole = driver.putCardInHand(me, "Portable Hole")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, hole)
        driver.bothPass() // resolve the spell -> it enters; the ETB trigger has no legal target

        driver.pendingDecision shouldBe null
        driver.findPermanent(me, "Portable Hole") shouldNotBe null
        driver.findPermanent(opponent, "Hill Giant") shouldNotBe null
    }
})
