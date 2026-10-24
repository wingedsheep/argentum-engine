package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.legalTargetsFor
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.QuandrixCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Quandrix Command (STX #217) — {1}{G}{U} Instant. Choose two — bounce a creature or planeswalker;
 * counter an artifact or enchantment spell; two +1/+1 counters on a creature; target player shuffles
 * up to three target cards from their graveyard into their library.
 *
 * The fourth mode's card slot is offered per chosen player — only that player's graveyard — and the
 * library still shuffles when every card target is gone by resolution (ruling 2021-04-16).
 */
class QuandrixCommandScenarioTest : FunSpec({

    val counters = 2
    val shuffle = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(QuandrixCommand))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(caster: EntityId) {
        giveMana(caster, Color.GREEN, 2)
        giveMana(caster, Color.BLUE, 1)
        val spell = putCardInHand(caster, "Quandrix Command")
        submit(CastSpell(playerId = caster, cardId = spell, chosenModes = listOf(counters, shuffle))).error shouldBe null
    }

    test("counters on a creature; the opponent shuffles two chosen cards from their graveyard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val myCard = driver.putCardInGraveyard(me, "Savannah Lions")
        val oppCards = listOf(
            driver.putCardInGraveyard(opp, "Centaur Courser"),
            driver.putCardInGraveyard(opp, "Grizzly Bears"),
            driver.putCardInGraveyard(opp, "Mountain"),
        )
        val oppLibrary = driver.state.getLibrary(opp).size

        driver.castCommand(me)
        driver.submitTargetSelection(me, listOf(bears)).error shouldBe null

        val shuffleTargets = driver.pendingDecision
        shuffleTargets.shouldBeInstanceOf<ChooseTargetsDecision>()
        shuffleTargets.legalTargetsFor(1, mapOf(0 to listOf(opp))) shouldContainExactlyInAnyOrder oppCards
        shuffleTargets.legalTargetsFor(1, mapOf(0 to listOf(me))) shouldBe listOf(myCard)
        driver.submitMultiTargetSelection(me, mapOf(0 to listOf(opp), 1 to oppCards.take(2))).error shouldBe null
        driver.bothPass()

        driver.state.getEntity(bears)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        driver.getGraveyard(opp) shouldBe listOf(oppCards[2])
        driver.state.getLibrary(opp).size shouldBe oppLibrary + 2
        driver.state.getLibrary(opp).let { library -> oppCards.take(2).forEach { library shouldContain it } }
        driver.getGraveyard(me) shouldContain myCard
    }

    test("a card from another player's graveyard is not a legal pick for the targeted player") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val myCard = driver.putCardInGraveyard(me, "Savannah Lions")
        driver.putCardInGraveyard(opp, "Centaur Courser")

        driver.castCommand(me)
        driver.submitTargetSelection(me, listOf(bears)).error shouldBe null
        driver.submitMultiTargetSelection(me, mapOf(0 to listOf(opp), 1 to listOf(myCard))).error shouldNotBe null
    }

    test("the targeted player still shuffles when every card target left the graveyard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppCard = driver.putCardInGraveyard(opp, "Centaur Courser")

        driver.castCommand(me)
        driver.submitTargetSelection(me, listOf(bears)).error shouldBe null
        driver.submitMultiTargetSelection(me, mapOf(0 to listOf(opp), 1 to listOf(oppCard))).error shouldBe null
        // The card is exiled in response, so it is an illegal target as the spell resolves.
        driver.replaceState(driver.state.moveToZone(oppCard, ZoneKey(opp, Zone.GRAVEYARD), ZoneKey(opp, Zone.EXILE)))
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == opp }
        driver.bothPass()

        driver.state.getLibrary(opp) shouldNotContain oppCard
        driver.state.getExile(opp) shouldContain oppCard
        driver.events.count { it is LibraryShuffledEvent && it.playerId == opp } shouldBe shufflesBefore + 1
        driver.state.getEntity(bears)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
    }
})
