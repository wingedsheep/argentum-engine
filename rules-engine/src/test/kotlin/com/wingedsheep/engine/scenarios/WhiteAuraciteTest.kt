package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.WhiteAuracite
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * White Auracite — {2}{W}{W} Artifact
 *
 * "When this artifact enters, exile target nonland permanent an opponent controls
 *  until this artifact leaves the battlefield.
 *  {T}: Add {W}."
 */
class WhiteAuraciteTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WhiteAuracite)
        return driver
    }

    fun GameTestDriver.isInExile(playerId: EntityId, cardName: String): Boolean =
        state.getExile(playerId).any { state.getEntity(it)?.get<CardComponent>()?.name == cardName }

    test("ETB exiles an opponent permanent; it returns when White Auracite leaves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val victim = driver.putCreatureOnBattlefield(opp, "Glory Seeker")

        // Cast White Auracite; its ETB exiles a target nonland opponent permanent.
        val auracite = driver.putCardInHand(me, "White Auracite")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, auracite)
        driver.bothPass() // resolve artifact -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> pause for target selection

        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        // Victim is exiled and the auracite holds the link.
        driver.findPermanent(opp, "Glory Seeker") shouldBe null
        driver.isInExile(opp, "Glory Seeker") shouldBe true

        val auraciteId = driver.findPermanent(me, "White Auracite")
        auraciteId shouldNotBe null
        driver.state.getEntity(auraciteId!!)?.get<LinkedExileComponent>()?.exiledIds?.contains(victim) shouldBe true

        // Destroy White Auracite (Naturalize destroys target artifact) -> LTB trigger returns the exiled permanent.
        val naturalize = driver.putCardInHand(me, "Naturalize")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpellWithTargets(me, naturalize, listOf(ChosenTarget.Permanent(auraciteId)))
        driver.bothPass() // resolve Naturalize
        driver.bothPass() // resolve LTB trigger

        driver.findPermanent(me, "White Auracite") shouldBe null
        driver.findPermanent(opp, "Glory Seeker") shouldNotBe null
        driver.isInExile(opp, "Glory Seeker") shouldBe false
    }
})
