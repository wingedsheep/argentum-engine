package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.MoggWarMarshal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mogg War Marshal (TSP #170) — "Echo {1}{R}. When this creature enters or dies, create a 1/1 red
 * Goblin creature token."
 *
 * Echo itself is proved in `EchoTest`; this pins the card's wiring: a Goblin on entering, the echo
 * owed at the next upkeep, and — the reason Krenko decks run it — a second Goblin when the echo
 * goes unpaid and the Marshal is sacrificed.
 */
class MoggWarMarshalScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MoggWarMarshal))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.goblinTokens(player: EntityId): Int = state.getBattlefield().count { id ->
        getController(id) == player && state.getEntity(id)?.has<TokenComponent>() == true &&
            state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Goblin" } == true
    }

    test("enters with a Goblin; skipping the echo sacrifices it for a second Goblin") {
        val d = driver()
        val me = d.activePlayer!!
        val marshal = d.putCardInHand(me, "Mogg War Marshal")
        d.giveMana(me, Color.RED, 2)
        d.castSpell(me, marshal)
        d.bothPass() // the spell
        d.bothPass() // its enters trigger
        d.goblinTokens(me) shouldBe 1

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.passPriorityUntil(Step.UPKEEP, me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, false)
        (marshal in d.state.getBattlefield()) shouldBe false
        d.bothPass() // its dies trigger
        d.goblinTokens(me) shouldBe 2
    }

    test("paying the echo keeps it, and it isn't owed again") {
        val d = driver()
        val me = d.activePlayer!!
        val marshal = d.putCardInHand(me, "Mogg War Marshal")
        d.giveMana(me, Color.RED, 2)
        d.castSpell(me, marshal)
        d.bothPass()
        d.bothPass()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.passPriorityUntil(Step.UPKEEP, me)
        d.giveMana(me, Color.RED, 2)
        d.bothPass()
        d.submitYesNo(me, true)
        (marshal in d.state.getBattlefield()) shouldBe true

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.passPriorityUntil(Step.UPKEEP, me)
        d.stackSize shouldBe 0
    }
})
