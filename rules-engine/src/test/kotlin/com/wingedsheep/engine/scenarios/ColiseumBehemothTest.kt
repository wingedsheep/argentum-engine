package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ColiseumBehemoth
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Coliseum Behemoth — {5}{G}{G} 7/7 Creature — Beast
 *
 * "Trample
 *  When this creature enters, choose one —
 *  • Destroy target artifact or enchantment.
 *  • Draw a card."
 */
class ColiseumBehemothTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ColiseumBehemoth)
        return driver
    }

    test("has trample") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val behemoth = driver.putCreatureOnBattlefield(me, "Coliseum Behemoth")
        projector.project(driver.state).hasKeyword(behemoth, Keyword.TRAMPLE) shouldBe true
    }

    test("mode 1 — destroys a target artifact or enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // An artifact for the opponent to lose. Bonesplitter is a vanilla Equipment artifact.
        val artifact = driver.putPermanentOnBattlefield(opp, "Bonesplitter")

        val behemoth = driver.putCardInHand(me, "Coliseum Behemoth")
        driver.giveColorlessMana(me, 5)
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 2)
        driver.castSpell(me, behemoth)
        driver.bothPass() // resolve creature -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> mode choice

        val mode = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val destroyIndex = mode.options.indexOfFirst { it.contains("Destroy") }
        driver.submitDecision(me, OptionChosenResponse(mode.id, destroyIndex))

        // The chosen mode targets the artifact.
        driver.submitTargetSelection(me, listOf(artifact))
        driver.bothPass()

        driver.findPermanent(opp, "Bonesplitter") shouldBe null
        driver.getGraveyard(opp).contains(artifact) shouldBe true
    }

    test("mode 2 — draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!

        val behemoth = driver.putCardInHand(me, "Coliseum Behemoth")
        val handBefore = driver.state.getHand(me).size

        driver.giveColorlessMana(me, 5)
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 2)
        driver.castSpell(me, behemoth)
        driver.bothPass() // resolve creature -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> mode choice

        val mode = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val drawIndex = mode.options.indexOfFirst { it.contains("Draw") }
        driver.submitDecision(me, OptionChosenResponse(mode.id, drawIndex))
        driver.bothPass()

        // Cast removed Behemoth from hand (-1), draw mode added one card (+1) => net same as before cast.
        // Before cast hand had the Behemoth; after cast+draw hand has one drawn card in its place.
        driver.state.getHand(me).size shouldBe handBefore
    }
})
