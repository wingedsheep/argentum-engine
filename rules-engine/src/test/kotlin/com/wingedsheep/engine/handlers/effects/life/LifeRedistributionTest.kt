package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Redistributing life totals (Reverse the Sands). The walk is driven directly through
 * [LifeRedistribution] so multiplayer and shared-life formats can be exercised:
 *
 * - totals move whole — each player gets exactly one of the starting totals (Reverse the Sands ruling);
 * - a player who can't gain life can't be handed a higher total, and one who can't lose life can't
 *   be handed a lower one (CR 119.7–8), and the walk never offers a total that would strand a later
 *   player without a legal one;
 * - in Two-Headed Giant the team's shared total is the unit, so at most one member of each team is
 *   affected (CR 810.9f);
 * - the changes are ordinary life gain and loss.
 */
class LifeRedistributionTest : FunSpec({

    val predicateEvaluator = PredicateEvaluator(cardRegistry = null)

    fun boot(playerCount: Int, format: Format = Format.Standard): Pair<GameState, List<EntityId>> {
        val registry = CardRegistry().also { it.register(TestCards.all) }
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                format = format,
                players = (1..playerCount).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = if (format is Format.TwoHeadedGiant) listOf(listOf(0, 1), listOf(2, 3)) else null,
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    fun GameState.withLives(players: List<EntityId>, vararg lives: Int): GameState =
        players.zip(lives.toList()).fold(this) { s, (p, life) -> s.withLifeTotal(p, life) }

    /** Answer every prompt with [pick] (the prompted player id, the offered totals → chosen total). */
    fun run(
        state: GameState,
        controller: EntityId,
        pick: (EntityId, List<Int>) -> Int,
    ): Pair<LifeRedistribution.Step.Done, List<List<Int>>> {
        val offered = mutableListOf<List<Int>>()
        var step = LifeRedistribution.start(state, EffectContext(sourceId = null, controllerId = controller), predicateEvaluator)
        while (step is LifeRedistribution.Step.Ask) {
            val progress = step.continuation
            offered += progress.optionValues
            val choice = pick(progress.players[progress.assigned.size], progress.optionValues)
            step = LifeRedistribution.answer(state, progress, choice, predicateEvaluator)
        }
        return (step as LifeRedistribution.Step.Done) to offered
    }

    test("two players: the only choice is keep or swap, and a swap is gain and loss") {
        val (base, p) = boot(2)
        val state = base.withLives(p, 5, 15)

        val (done, offered) = run(state, p[0]) { _, _ -> 15 }

        withClue("one question, offering the two whole totals — never a split") {
            offered shouldContainExactly listOf(listOf(15, 5))
        }
        done.state.lifeTotal(p[0]) shouldBe 15
        done.state.lifeTotal(p[1]) shouldBe 5
        done.events.filterIsInstance<LifeChangedEvent>().map { it.playerId to it.reason } shouldContainExactlyInAnyOrder
            listOf(p[0] to LifeChangeReason.LIFE_GAIN, p[1] to LifeChangeReason.LIFE_LOSS)
    }

    test("keeping every total changes nothing and emits no life events") {
        val (base, p) = boot(2)
        val state = base.withLives(p, 5, 15)

        val (done, _) = run(state, p[0]) { _, _ -> 5 }

        done.state.lifeTotal(p[0]) shouldBe 5
        done.state.lifeTotal(p[1]) shouldBe 15
        done.events shouldBe emptyList()
    }

    test("equal totals ask nothing") {
        val (base, p) = boot(2)
        val (done, offered) = run(base, p[0]) { _, _ -> error("no prompt expected") }
        offered shouldBe emptyList()
        done.state.lifeTotal(p[0]) shouldBe 20
    }

    test("three players: any permutation, and the last player's total is forced") {
        val (base, p) = boot(3)
        val state = base.withLives(p, 3, 10, 30)

        // Rotate: p0 takes 30, p1 takes 3, p2 is left 10.
        val (done, offered) = run(state, p[0]) { player, _ -> if (player == p[0]) 30 else 3 }

        offered shouldContainExactly listOf(listOf(30, 10, 3), listOf(10, 3))
        done.state.lifeTotal(p[0]) shouldBe 30
        done.state.lifeTotal(p[1]) shouldBe 3
        done.state.lifeTotal(p[2]) shouldBe 10
    }

    test("any number of players: handing two their own totals affects only the other two") {
        val (base, p) = boot(3)
        val state = base.withLives(p, 3, 10, 30)

        val (done, _) = run(state, p[0]) { player, _ -> if (player == p[0]) 30 else 10 }

        done.state.lifeTotal(p[0]) shouldBe 30
        done.state.lifeTotal(p[1]) shouldBe 10
        done.state.lifeTotal(p[2]) shouldBe 3
    }

    test("a player who can't gain life is never offered a higher total (CR 119.7)") {
        val (base, p) = boot(2)
        val state = base.withLives(p, 5, 15).updateEntity(p[0]) { it.with(CantGainLifeComponent()) }

        val (done, offered) = run(state, p[0]) { _, _ -> error("no prompt expected") }

        withClue("p0 can only keep 5, which forces p1 to keep 15") { offered shouldBe emptyList() }
        done.state.lifeTotal(p[0]) shouldBe 5
        done.state.lifeTotal(p[1]) shouldBe 15
    }

    test("a player who can't lose life is never handed a lower total (CR 119.8)") {
        val (base, p) = boot(3)
        val guard = EntityId.of("life-loss-guard")
        val state = base.withLives(p, 5, 10, 20)
            .withEntity(
                guard,
                ComponentContainer.of(
                    ControllerComponent(p[2]),
                    ReplacementEffectSourceComponent(
                        listOf(ModifyLifeLoss(multiplier = 0, appliesTo = EventPattern.LifeLossEvent(Player.You)))
                    )
                )
            )
            .addToZone(ZoneKey(p[2], Zone.BATTLEFIELD), guard)

        // p0 takes 10: then 20 must stay with p2 (it can't go lower), so p1 is forced to 5.
        val (done, offered) = run(state, p[0]) { _, _ -> 10 }

        withClue("20 isn't offered to p0 — that would leave p2, who can't lose life, below 20") {
            offered shouldContainExactly listOf(listOf(10, 5))
        }
        done.state.lifeTotal(p[0]) shouldBe 10
        done.state.lifeTotal(p[1]) shouldBe 5
        done.state.lifeTotal(p[2]) shouldBe 20
    }

    test("Two-Headed Giant: the team's shared total is one unit (CR 810.9f)") {
        val (base, p) = boot(4, Format.TwoHeadedGiant())
        val state = base.withLifeTotal(p[0], 12).withLifeTotal(p[2], 30)

        val (done, offered) = run(state, p[0]) { _, _ -> 30 }

        withClue("one question for two totals, not four players") { offered shouldContainExactly listOf(listOf(30, 12)) }
        done.state.lifeTotal(p[0]) shouldBe 30
        done.state.lifeTotal(p[1]) shouldBe 30
        done.state.lifeTotal(p[2]) shouldBe 12
        done.state.lifeTotal(p[3]) shouldBe 12
    }
})
