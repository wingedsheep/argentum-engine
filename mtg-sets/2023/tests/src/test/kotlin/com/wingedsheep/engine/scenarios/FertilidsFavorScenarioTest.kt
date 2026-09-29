package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.FertilidsFavor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fertilid's Favor (MOM #186): target player searches their library for a basic land, puts it onto
 * the battlefield tapped, then shuffles; two +1/+1 counters on up to one target artifact or creature.
 */
class FertilidsFavorScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(FertilidsFavor))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun plusCounters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun lands(d: GameTestDriver, player: EntityId): List<EntityId> =
        d.state.getZone(player, Zone.BATTLEFIELD)
            .filter { id -> d.state.getEntity(id)?.get<CardComponent>()?.name == "Forest" }

    test("targeted opponent searches their own library; land enters tapped under their control; counters land") {
        val d = newDriver()
        val me = d.player1
        val opp = d.getOpponent(me)
        val bear = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val oppLandsBefore = lands(d, opp).size
        val oppLibraryBefore = d.state.getZone(opp, Zone.LIBRARY).size

        val spell = d.putCardInHand(me, "Fertilid's Favor")
        d.giveMana(me, Color.GREEN, 1)
        d.giveColorlessMana(me, 3)
        d.castSpell(me, spell, listOf(opp, bear)).outcome shouldBe Outcome.Done
        d.bothPass()

        // The targeted player — not the caster — makes the search choice.
        val decision = d.pendingDecision!!
        decision.playerId shouldBe opp
        val pick = d.state.getZone(opp, Zone.LIBRARY).first()
        d.submitDecision(opp, CardsSelectedResponse(decision.id, listOf(pick)))

        var guard = 0
        while (guard++ < 10 && d.isPaused) d.autoResolveDecision()

        d.state.getZone(opp, Zone.BATTLEFIELD).contains(pick) shouldBe true
        d.isTapped(pick) shouldBe true
        lands(d, opp).size shouldBe oppLandsBefore + 1
        d.state.getZone(opp, Zone.LIBRARY).size shouldBe oppLibraryBefore - 1
        plusCounters(d, bear) shouldBe 2
    }

    test("no artifact or creature target — the search still happens for yourself") {
        val d = newDriver()
        val me = d.player1
        val myLibraryBefore = d.state.getZone(me, Zone.LIBRARY).size

        val spell = d.putCardInHand(me, "Fertilid's Favor")
        d.giveMana(me, Color.GREEN, 1)
        d.giveColorlessMana(me, 3)
        d.castSpell(me, spell, listOf(me)).outcome shouldBe Outcome.Done
        d.bothPass()

        val decision = d.pendingDecision!!
        decision.playerId shouldBe me
        val pick = d.state.getZone(me, Zone.LIBRARY).first()
        d.submitDecision(me, CardsSelectedResponse(decision.id, listOf(pick)))
        var guard = 0
        while (guard++ < 10 && d.isPaused) d.autoResolveDecision()

        d.state.getZone(me, Zone.BATTLEFIELD).contains(pick) shouldBe true
        d.isTapped(pick) shouldBe true
        d.state.getZone(me, Zone.LIBRARY).size shouldBe myLibraryBefore - 1
    }
})
