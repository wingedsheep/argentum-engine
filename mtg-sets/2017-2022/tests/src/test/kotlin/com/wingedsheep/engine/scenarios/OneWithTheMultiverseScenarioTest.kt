package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.OneWithTheMultiverse
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * One with the Multiverse — "Once during each of your turns, you may cast a spell from your hand
 * or the top of your library without paying its mana cost." The free cast is offered on the
 * top-of-library path as well as from hand, and one use covers both zones.
 */
class OneWithTheMultiverseScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(OneWithTheMultiverse)
        d.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true)
        return d
    }

    fun GameTestDriver.freeCastOf(player: com.wingedsheep.sdk.model.EntityId, cardId: com.wingedsheep.sdk.model.EntityId) =
        legalActions(player).firstOrNull { la ->
            val cs = la.action as? CastSpell
            cs != null && cs.cardId == cardId && cs.useWithoutPayingManaCost
        }

    test("the top card of the library can be cast for free, once per turn") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val enchantment = d.putPermanentOnBattlefield(p1, OneWithTheMultiverse.name)

        val top = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        val offer = d.freeCastOf(p1, top)
        offer shouldNotBe null
        offer!!.sourceZone shouldBe "LIBRARY"

        val creaturesBefore = d.getCreatures(p1).size
        d.submitSuccess(CastSpell(p1, top, useWithoutPayingManaCost = true))
        d.bothPass()
        d.getCreatures(p1).size shouldBe creaturesBefore + 1
        d.state.getEntity(enchantment)?.get<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldNotBe null

        // The use is spent: the next top card is no longer free, from either zone.
        val nextTop = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        d.freeCastOf(p1, nextTop) shouldBe null
        val inHand = d.putCardInHand(p1, "Grizzly Bears")
        d.freeCastOf(p1, inHand) shouldBe null
        d.submit(CastSpell(p1, nextTop, useWithoutPayingManaCost = true)).outcome shouldNotBe Outcome.Done
    }

    test("a free cast from hand spends the same use as the library") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, OneWithTheMultiverse.name)

        val inHand = d.putCardInHand(p1, "Grizzly Bears")
        d.freeCastOf(p1, inHand) shouldNotBe null
        d.submitSuccess(CastSpell(p1, inHand, useWithoutPayingManaCost = true))
        d.bothPass()

        val top = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        d.freeCastOf(p1, top) shouldBe null
    }

    test("no free cast during an opponent's turn") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(p1, OneWithTheMultiverse.name)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP) // opponent's turn
        d.activePlayer shouldNotBe p1
        d.passPriority(d.getOpponent(p1))
        d.priorityPlayer shouldBe p1

        // The paid cast from the top is still there at instant speed; the free one isn't.
        val bolt = d.putCardOnTopOfLibrary(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.legalActions(p1).any { (it.action as? CastSpell)?.cardId == bolt } shouldBe true
        d.freeCastOf(p1, bolt) shouldBe null
    }
})
