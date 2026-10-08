package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

/**
 * A secret bid's per-bidder branch runs as ordinary effect resolution: when it pauses for a
 * decision the remaining bidders' branches must still run once that decision is answered, and
 * each branch must see the resolving ability's targets.
 */
class SecretBidBranchPauseTest : FunSpec({

    // Every highest bidder sacrifices a creature (a choice when they control two) and loses 1 life.
    val sacrificeBidder = card("Test Secret Bid Sacrificer") {
        typeLine = "Creature — Ogre"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.SecretBid(
                highestBidderEffect = CompositeEffect(listOf(
                    Effects.Sacrifice(GameObjectFilter.Creature, count = 1, target = EffectTarget.Controller),
                    Effects.LoseLife(1, EffectTarget.Controller)
                ))
            )
        }
    }

    // Every highest bidder puts a +1/+1 counter on the trigger's target.
    val targetingBidder = card("Test Secret Bid Targeter") {
        typeLine = "Creature — Ogre"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            val t = target(TargetFilter.Creature)
            effect = Effects.SecretBid(
                highestBidderEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
            )
        }
    }

    val json = Json { serializersModule = com.wingedsheep.engine.core.engineSerializersModule; allowStructuredMapKeys = true }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(sacrificeBidder, targetingBidder))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun roundTrip(d: GameTestDriver) {
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
    }

    fun nextOwnUpkeep(d: GameTestDriver) {
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
    }

    /** Both players bid [bid], so both are highest bidders (player 1's branch runs first). */
    fun bothBid(d: GameTestDriver, bid: Int) {
        repeat(2) {
            roundTrip(d)
            val decision = d.pendingDecision!!
            d.submitDecision(decision.playerId, NumberChosenResponse(decision.id, bid)).error shouldBe null
        }
    }

    test("a bidder branch that pauses for a sacrifice choice still runs the remaining bidders' branches") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, sacrificeBidder.name)
        val bear1 = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val bear2a = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        d.bothPass().error shouldBe null
        bothBid(d, 2)

        // Player 1's branch pauses on the sacrifice choice.
        roundTrip(d)
        val first = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        first.playerId shouldBe d.player1
        d.submitCardSelection(d.player1, listOf(bear1)).error shouldBe null
        d.getLifeTotal(d.player1) shouldBe 19

        // Player 2's branch runs after player 1's decision was answered.
        roundTrip(d)
        val second = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        second.playerId shouldBe d.player2
        d.submitCardSelection(d.player2, listOf(bear2a)).error shouldBe null

        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 19
        d.getLifeTotal(d.player2) shouldBe 19
        d.getCreatures(d.player1).size shouldBe 1
        d.getCreatures(d.player2).size shouldBe 1
        d.state.stack.size shouldBe 0
    }

    test("each bidder branch sees the resolving ability's original target") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, targetingBidder.name)
        val bear = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        nextOwnUpkeep(d)
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass().error shouldBe null
        bothBid(d, 1)

        d.pendingDecision shouldBe null
        d.state.getEntity(bear)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
    }
})
