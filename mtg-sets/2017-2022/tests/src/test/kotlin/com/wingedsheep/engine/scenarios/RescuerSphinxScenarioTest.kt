package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.RescuerSphinx
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rescuer Sphinx (WAR #65) — {2}{U}{U} 3/2 Creature — Sphinx.
 *
 * "Flying. As this creature enters, you may return a nonland permanent you control to its owner's
 *  hand. If you do, this creature enters with a +1/+1 counter on it."
 *
 * The pick is offered only among nonland permanents you control (never the Sphinx itself, nor an
 * opponent's permanent, nor a land), and the counter is gated on the return actually happening.
 */
class RescuerSphinxScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(RescuerSphinx))
        d.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castSphinx(): SelectCardsDecision {
        val sphinx = putCardInHand(player1, "Rescuer Sphinx")
        giveMana(player1, Color.BLUE, 4)
        submit(CastSpell(playerId = player1, cardId = sphinx))
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("returning a nonland permanent you control bounces it and the Sphinx enters with a +1/+1 counter") {
        val d = newDriver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val island = d.putLandOnBattlefield(d.player1, "Island")
        val theirBear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        val decision = d.castSphinx()
        withClue("only nonland permanents you control are offered") {
            decision.options shouldContainExactlyInAnyOrder listOf(bear)
            decision.options.contains(island) shouldBe false
            decision.options.contains(theirBear) shouldBe false
        }
        d.submitDecision(d.player1, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(bear)))
        d.isPaused shouldBe false

        val sphinx = d.findPermanent(d.player1, "Rescuer Sphinx").shouldNotBeNull()
        d.getHand(d.player1) shouldContain bear
        d.plusCounters(sphinx) shouldBe 1
        d.state.projectedState.getPower(sphinx) shouldBe 4
        d.state.projectedState.getToughness(sphinx) shouldBe 3
    }

    test("declining the return leaves the permanent in play and the Sphinx enters without a counter") {
        val d = newDriver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val decision = d.castSphinx()
        d.submitDecision(d.player1, CardsSelectedResponse(decisionId = decision.id, selectedCards = emptyList()))
        d.isPaused shouldBe false

        val sphinx = d.findPermanent(d.player1, "Rescuer Sphinx").shouldNotBeNull()
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe bear
        d.plusCounters(sphinx) shouldBe 0
        d.state.projectedState.getPower(sphinx) shouldBe 3
        d.state.projectedState.getToughness(sphinx) shouldBe 2
    }
})
