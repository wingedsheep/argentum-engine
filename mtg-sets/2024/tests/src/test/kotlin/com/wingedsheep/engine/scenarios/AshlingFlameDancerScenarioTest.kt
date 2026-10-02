package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

/**
 * Ashling, Flame Dancer (MH3) — magecraft fires on casts *and* copies, and the per-turn tally
 * counts both: a twice-replicated Reiterating Bolt is one cast plus two copies, so the ability
 * resolves three times — loot, loot + 2 damage to each opponent and their creatures, loot + {R}{R}{R}{R}.
 */
class AshlingFlameDancerScenarioTest : ScenarioTestBase() {

    private fun TestGame.giveEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun TestGame.redInPool(): Int =
        state.getEntity(player1Id)?.get<ManaPoolComponent>()?.red ?: 0

    private fun TestGame.castBolt(target: EntityId, times: Int?) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Reiterating Bolt" },
            targets = listOf(ChosenTarget.Permanent(target)),
            declaredCostSlot = times?.let { ChoiceSlot.REPLICATED },
            declaredCostTimes = times ?: 1,
        )
    )

    /** Resolve everything; copies keep the Wurm as their target, each loot discards the one card in hand. */
    private fun TestGame.drain(wurm: EntityId) {
        var guard = 0
        while ((state.stack.isNotEmpty() || getPendingDecision() != null) && guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(wurm)).error shouldBe null
                is SelectCardsDecision -> selectCards(decision.options.take(decision.minSelections.coerceAtLeast(1))).error shouldBe null
                null -> resolveStack()
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ashling, Flame Dancer")
        .withCardInHand(1, "Reiterating Bolt")
        .withCardInHand(1, "Forest")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardOnBattlefield(2, "Craw Wurm") // 6/4
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a lone cast resolves once: loot only") {
            val game = board()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castBolt(wurm, times = null).error shouldBe null
            game.drain(wurm)

            game.isInGraveyard(1, "Forest") shouldBe true
            game.handSize(1) shouldBe 1
            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.redInPool() shouldBe 0
        }

        test("each copy triggers magecraft: the second resolution burns, the third adds {R}{R}{R}{R}") {
            val game = board()
            game.giveEnergy(6)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castBolt(wurm, times = 2).error shouldBe null
            game.drain(wurm)

            game.handSize(1) shouldBe 1
            game.getLifeTotal(2) shouldBe 18
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.getLifeTotal(1) shouldBe 20 // only opponents and their creatures are dealt damage
            game.isOnBattlefield("Ashling, Flame Dancer") shouldBe true
            game.redInPool() shouldBe 4
        }

        test("unspent red mana is kept as steps end") {
            val game = board()
            game.giveEnergy(6)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castBolt(wurm, times = 2).error shouldBe null
            game.drain(wurm)
            game.redInPool() shouldBe 4

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.redInPool() shouldBe 4
        }
    }
}
