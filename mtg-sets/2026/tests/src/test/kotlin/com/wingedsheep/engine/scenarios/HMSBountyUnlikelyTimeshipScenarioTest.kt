package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.HMSBountyUnlikelyTimeship
import com.wingedsheep.mtg.sets.definitions.trk.cards.HumpbackWhales
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * H.M.S. Bounty, Unlikely Timeship ({4}{U} Legendary Artifact — Spacecraft, 3/4):
 * "Station. 8+ | Flying. Whenever H.M.S. Bounty deals combat damage to a player, draw a card. If you
 * control a Whale and haven't begun an extra turn this game, take an extra turn after this one."
 */
class HMSBountyUnlikelyTimeshipScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(HMSBountyUnlikelyTimeship, HumpbackWhales))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** A fully stationed Bounty (8 charge counters) that can attack this turn. */
    fun GameTestDriver.stationedBounty(me: EntityId): EntityId {
        val bounty = putPermanentOnBattlefield(me, "H.M.S. Bounty, Unlikely Timeship")
        replaceState(state.updateEntity(bounty) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, 8))
        })
        removeSummoningSickness(bounty)
        return bounty
    }

    /** Attack with the Bounty unblocked and let its combat-damage trigger resolve. */
    fun GameTestDriver.connect(me: EntityId, opp: EntityId, bounty: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS, me)
        declareAttackers(me, listOf(bounty), opp).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareNoBlockers(opp)
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    fun GameTestDriver.extraTurnsBegun(player: EntityId): Int =
        state.getEntity(player)?.get<ExtraTurnsComponent>()?.begun ?: 0

    test("connecting while you control a Whale draws a card and takes one extra turn") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Humpback Whales")
        val bounty = d.stationedBounty(me)
        d.state.projectedState.hasKeyword(bounty, Keyword.FLYING) shouldBe true

        val hand = d.getHandSize(me)
        d.connect(me, opp, bounty)
        withClue("the trigger draws a card") { d.getHandSize(me) shouldBe hand + 1 }
        d.getLifeTotal(opp) shouldBe 17

        d.passPriorityUntil(Step.UPKEEP)
        withClue("the next turn is an extra turn for the Bounty's controller") {
            d.activePlayer shouldBe me
            d.extraTurnsBegun(me) shouldBe 1
            d.extraTurnsBegun(opp) shouldBe 0
        }

        // Having begun an extra turn this game, connecting again only draws.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handInExtraTurn = d.getHandSize(me)
        d.connect(me, opp, bounty)
        withClue("still draws during the extra turn") { d.getHandSize(me) shouldBe handInExtraTurn + 1 }
        d.passPriorityUntil(Step.UPKEEP)
        withClue("you've begun an extra turn this game, so no second one") {
            d.activePlayer shouldBe opp
            d.extraTurnsBegun(me) shouldBe 1
        }
    }

    test("without a Whale it only draws a card") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bounty = d.stationedBounty(me)

        val hand = d.getHandSize(me)
        d.connect(me, opp, bounty)
        d.getHandSize(me) shouldBe hand + 1

        d.passPriorityUntil(Step.UPKEEP)
        withClue("no Whale, no extra turn") {
            d.activePlayer shouldBe opp
            d.extraTurnsBegun(me) shouldBe 0
        }
    }
})
