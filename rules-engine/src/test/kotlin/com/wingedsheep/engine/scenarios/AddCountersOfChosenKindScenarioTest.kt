package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect] —
 * "choose a counter on target permanent. Put an additional counter of that kind on that permanent"
 * (Ichormoon Gauntlet), driven through inline sorceries.
 *
 * Proves: with two or more kinds the controller picks among exactly the kinds present and only that
 * kind grows; with one kind it is placed without a prompt; with none nothing happens; `count`
 * scales the placement; and counters on a player (poison) are a legal kind too.
 *
 * The printed-list form (`kindFrom = LISTED_ABSENT`, Crystalline Giant) offers only the listed kinds
 * the recipient has none of, read at resolution; `atRandom` draws the kind through the game RNG
 * instead of asking, never a kind already present.
 */
class AddCountersOfChosenKindScenarioTest : ScenarioTestBase() {

    private val nudge = card("Test Nudge") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a counter on target permanent. Put an additional counter of that kind on that permanent."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCountersOfChosenKind(t)
        }
    }

    private val shove = card("Test Shove") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a counter on target permanent. Put two additional counters of that kind on that permanent."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCountersOfChosenKind(t, count = 2)
        }
    }

    private val infect = card("Test Player Nudge") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a counter on target player. That player gets an additional counter of that kind."
        spell {
            val t = target(Targets.Player)
            effect = Effects.AddCountersOfChosenKind(t)
        }
    }

    private val listed = listOf(CounterType.FLYING, CounterType.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE)

    private val pick = card("Test Pick") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a kind of counter that target permanent doesn't have on it from among flying, trample, and +1/+1. Put a counter of that kind on it."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCounterOfAbsentKind(listed, t)
        }
    }

    private val lottery = card("Test Lottery") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a kind of counter at random that target permanent doesn't have on it from among flying, trample, and +1/+1. Put a counter of that kind on it."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCounterOfAbsentKind(listed, t, atRandom = true)
        }
    }

    private val reroll = card("Test Reroll") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Choose a kind of counter on target permanent at random. Put an additional counter of that kind on it."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCountersOfChosenKind(t, atRandom = true)
        }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board(spell: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, spell)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(nudge)
        cardRegistry.register(shove)
        cardRegistry.register(infect)
        cardRegistry.register(pick)
        cardRegistry.register(lottery)
        cardRegistry.register(reroll)

        test("two kinds: the controller picks one and only that kind grows") {
            val game = board("Test Nudge")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)
            seed(game, bears, CounterType.STUN, 2)

            game.castSpell(1, "Test Nudge", targetId = bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            withClue("Exactly the two kinds on the permanent are offered") {
                decision.options.toSet() shouldBe setOf(CounterType.PLUS_ONE_PLUS_ONE.printed, CounterType.STUN.printed)
            }
            val stunIndex = decision.options.indexOf(CounterType.STUN.printed)
            game.submitDecision(OptionChosenResponse(decision.id, stunIndex)).error shouldBe null

            count(game, bears, CounterType.STUN) shouldBe 3
            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("one kind: placed with no prompt") {
            val game = board("Test Nudge")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Nudge", targetId = bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("no counters: nothing happens") {
            val game = board("Test Nudge")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Test Nudge", targetId = bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.getEntity(bears)?.get<CountersComponent>()?.counters.orEmpty().values.sum() shouldBe 0
        }

        test("count scales the placement of the chosen kind") {
            val game = board("Test Shove")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)
            seed(game, bears, CounterType.CHARGE, 1)

            game.castSpell(1, "Test Shove", targetId = bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf(CounterType.PLUS_ONE_PLUS_ONE.printed))
            ).error shouldBe null

            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            count(game, bears, CounterType.CHARGE) shouldBe 1
        }

        test("a player's counters are a kind too") {
            val game = board("Test Player Nudge")
            seed(game, game.player2Id, CounterType.POISON, 2)

            game.castSpellTargetingPlayer(1, "Test Player Nudge", 2).error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.POISON) shouldBe 3
        }
        test("listed kinds: only the ones the permanent has none of are offered") {
            val game = board("Test Pick")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.FLYING, 1)
            seed(game, bears, CounterType.STUN, 1)

            game.castSpell(1, "Test Pick", targetId = bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            withClue("Flying is already there and stun isn't listed") {
                decision.options.toSet() shouldBe setOf(CounterType.TRAMPLE.printed, CounterType.PLUS_ONE_PLUS_ONE.printed)
            }
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf(CounterType.TRAMPLE.printed))
            ).error shouldBe null

            count(game, bears, CounterType.TRAMPLE) shouldBe 1
            count(game, bears, CounterType.FLYING) shouldBe 1
            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        }

        test("listed kinds: one absent kind is placed with no prompt, none absent is a no-op") {
            val game = board("Test Pick")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.FLYING, 1)
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

            game.castSpell(1, "Test Pick", targetId = bears).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            count(game, bears, CounterType.TRAMPLE) shouldBe 1

            val full = board("Test Pick")
            val other = full.findPermanent("Grizzly Bears")!!
            listed.forEach { seed(full, other, it, 1) }
            full.castSpell(1, "Test Pick", targetId = other).error shouldBe null
            full.resolveStack()
            full.hasPendingDecision() shouldBe false
            listed.forEach { count(full, other, it) shouldBe 1 }
        }

        test("at random: no prompt, the draw follows the RNG and never repeats a present kind") {
            val drawn = (0L until 24L).map { s ->
                val game = board("Test Lottery")
                game.state = game.state.copy(rng = GameRng.seeded(s))
                val bears = game.findPermanent("Grizzly Bears")!!
                seed(game, bears, CounterType.FLYING, 1)

                game.castSpell(1, "Test Lottery", targetId = bears).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                count(game, bears, CounterType.FLYING) shouldBe 1
                val placed = listOf(CounterType.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE)
                    .filter { count(game, bears, it) == 1 }
                placed.size shouldBe 1
                placed.single()
            }
            withClue("Both absent kinds come up across seeds") {
                drawn.toSet() shouldBe setOf(CounterType.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE)
            }
        }

        test("at random among the recipient's own kinds: no prompt, one present kind grows") {
            val game = board("Test Reroll")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.STUN, 1)
            seed(game, bears, CounterType.CHARGE, 1)

            game.castSpell(1, "Test Reroll", targetId = bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            (count(game, bears, CounterType.STUN) + count(game, bears, CounterType.CHARGE)) shouldBe 3
        }

        test("the listed form needs its kinds, and an iterated kind can't be random") {
            shouldThrow<IllegalArgumentException> {
                AddCountersOfChosenKindEffect(kindFrom = CounterKindSource.LISTED_ABSENT)
            }
            shouldThrow<IllegalArgumentException> {
                AddCountersOfChosenKindEffect(kinds = listOf(CounterType.FLYING))
            }
            shouldThrow<IllegalArgumentException> {
                AddCountersOfChosenKindEffect(kindFrom = CounterKindSource.ITERATION, atRandom = true)
            }
        }
    }
}
