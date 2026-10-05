package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.MirrorImage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mirror Image — {2}{U} 0/0 Creature — Shapeshifter.
 *
 * "You may have this creature enter as a copy of a creature you control."
 *
 * Only creatures you control are offered; the copy takes the chosen creature's name and P/T.
 * Declining leaves a 0/0 that dies to state-based actions.
 */
class MirrorImageScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MirrorImage)
        return driver
    }

    test("enters as a copy of a creature you control, never an opponent's") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(me, "Hill Giant")
        val theirGiant = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        val image = driver.putCardInHand(me, "Mirror Image")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, image)
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain giant
        decision.options shouldNotContain theirGiant

        driver.submitCardSelection(me, listOf(giant))

        driver.state.getBattlefield() shouldContain image
        projector.getProjectedPower(driver.state, image) shouldBe 3
        projector.getProjectedToughness(driver.state, image) shouldBe 3
        driver.state.getEntity(image)?.get<CardComponent>()!!.name shouldBe "Hill Giant"
    }

    test("declining the copy leaves a 0/0 that dies to state-based actions") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Hill Giant")

        val image = driver.putCardInHand(me, "Mirror Image")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, image)
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, emptyList())

        driver.state.getBattlefield() shouldNotContain image
        driver.state.getGraveyard(me) shouldContain image
    }
})
