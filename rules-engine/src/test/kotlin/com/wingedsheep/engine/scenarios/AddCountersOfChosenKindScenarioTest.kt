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
    }
}
