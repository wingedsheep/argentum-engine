package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.SpitefulBanditry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spiteful Banditry (LTR) — "Whenever one or more creatures your opponents control die, you create
 * a Treasure token. This ability triggers only once each turn."
 *
 * Exercises the batched, opponent-scoped creature-death trigger
 * (`Triggers.OneOrMoreCreaturesAnOpponentControlsDie`): it fires at most once per death batch
 * (CR 603.3b) — so a single event that kills several of an opponent's creatures makes one
 * Treasure, not one per creature — and only creatures an opponent controls count. Both cases drive
 * the deaths through the real resolution pipeline via the card's own "deals X damage to each
 * creature" ETB, so the batched-death trigger detection actually runs.
 */
class SpitefulBanditryScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(SpitefulBanditry)
        )
        return d
    }

    fun GameTestDriver.treasures(playerId: EntityId): Int =
        state.getBattlefield().count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Treasure" &&
                state.projectedState.getController(id) == playerId
        }

    test("two opponent creatures dying at once make exactly ONE Treasure (batched, not per-creature)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val banditry = d.putCardInHand(active, "Spiteful Banditry")
        d.giveMana(active, Color.RED, 2)
        d.giveColorlessMana(active, 2) // X = 2: ETB deals 2 to each creature, killing both 2/2s at once
        d.castXSpell(active, banditry, xValue = 2).isSuccess shouldBe true
        repeat(8) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        d.treasures(active) shouldBe 1
    }

    test("creatures YOU control dying do not trigger it (opponent-scoped)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only the caster controls creatures; the ETB kills them, but the trigger watches the
        // *opponents'* creatures, so no Treasure is made.
        d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.putCreatureOnBattlefield(active, "Grizzly Bears")

        val banditry = d.putCardInHand(active, "Spiteful Banditry")
        d.giveMana(active, Color.RED, 2)
        d.giveColorlessMana(active, 2) // X = 2 kills the caster's own 2/2s
        d.castXSpell(active, banditry, xValue = 2).isSuccess shouldBe true
        repeat(8) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        d.treasures(active) shouldBe 0
    }
})
