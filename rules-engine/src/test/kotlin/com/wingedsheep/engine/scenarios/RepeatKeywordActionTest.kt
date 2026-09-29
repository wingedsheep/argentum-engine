package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ProliferatedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RepeatKeywordAction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for [RepeatKeywordAction] over proliferate — "If you would proliferate, proliferate
 * twice instead" (Tekuthal, Inquiry Dominus; a replacement effect, CR 614.1a, on the proliferate
 * keyword action, CR 701.34).
 *
 * The rules pinned here:
 * - one proliferate becomes two, each a complete proliferate: its own choice of recipients (any
 *   number, including none) and its own "whenever you proliferate" trigger;
 * - with nothing on the board to choose, both proliferates still happen;
 * - two instances multiply — the second applies to each proliferate the first produced — so four;
 * - "you" is the replacement's controller: an opponent's copy doesn't touch your proliferate;
 * - the targeted "another counter of each kind on target …" form is not proliferating, so it is
 *   never repeated.
 *
 * Test cards are inline (and non-legendary, so two can share a battlefield).
 */
class RepeatKeywordActionTest : ScenarioTestBase() {

    private val engine = card("Test Proliferation Engine") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "If you would proliferate, proliferate twice instead."
        replacementEffect(RepeatKeywordAction(times = 2, appliesTo = EventPattern.ProliferatedEvent()))
    }

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private val brokerage = card("Test Brokerage") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "For each kind of counter on target permanent or player, give that permanent " +
            "or player another counter of that kind."
        spell {
            val recipient = target(Targets.PermanentOrPlayer)
            effect = Effects.Proliferate(recipient)
        }
    }

    private val watcher = card("Test Proliferate Watcher") {
        manaCost = "{1}"
        typeLine = "Creature — Advisor"
        power = 1
        toughness = 1
        oracleText = "Whenever you proliferate, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.proliferates()
            effect = Effects.GainLife(1)
        }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    /**
     * Resolve the stack, answering the n-th proliferate prompt with `picks[n]` (the last entry
     * repeats). Returns every result and the number of proliferate prompts answered.
     */
    private fun resolveAll(game: TestGame, vararg picks: List<EntityId>): Pair<List<ExecutionResult>, Int> {
        val results = mutableListOf<ExecutionResult>()
        var prompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 30) {
            if (game.hasPendingDecision()) {
                results += game.selectCards(picks.getOrElse(prompts) { picks.lastOrNull() ?: emptyList() })
                prompts++
            } else {
                results += game.resolveStack()
            }
        }
        return results to prompts
    }

    private fun proliferatedEvents(results: List<ExecutionResult>) =
        results.flatMap { it.events }.filterIsInstance<ProliferatedEvent>()

    private fun baseScenario(caster: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(caster, "Forest", 2)
        .withActivePlayer(caster)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        listOf(engine, spreading, brokerage, watcher).forEach(cardRegistry::register)

        test("one proliferate becomes two, each with its own choice and its own trigger") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val (results, prompts) = resolveAll(game, listOf(bears))

            prompts shouldBe 2
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            proliferatedEvents(results).size shouldBe 2
            game.getLifeTotal(1) shouldBe 22
        }

        test("the second proliferate is chosen afresh — choosing nothing the second time adds nothing more") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val (_, prompts) = resolveAll(game, listOf(bears), emptyList())

            prompts shouldBe 2
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            withClue("choosing nothing is still proliferating, so both proliferates trigger") {
                game.getLifeTotal(1) shouldBe 22
            }
        }

        test("with nothing to choose, both proliferates still happen") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardInHand(1, "Test Spreading")
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            val (results, prompts) = resolveAll(game, emptyList())

            prompts shouldBe 0
            proliferatedEvents(results).size shouldBe 2
            game.getLifeTotal(1) shouldBe 22
        }

        test("two instances multiply: proliferate four times") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val (results, prompts) = resolveAll(game, listOf(bears))

            prompts shouldBe 4
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
            proliferatedEvents(results).size shouldBe 4
        }

        test("an opponent's instance does not repeat your proliferate") {
            val game = baseScenario()
                .withCardOnBattlefield(2, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val (results, prompts) = resolveAll(game, listOf(bears))

            prompts shouldBe 1
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            proliferatedEvents(results).size shouldBe 1
        }

        test("the targeted one-object form is not proliferating and is not repeated") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferation Engine")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Brokerage")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Test Brokerage").first(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            val (results, prompts) = resolveAll(game, emptyList())

            prompts shouldBe 0
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            proliferatedEvents(results).size shouldBe 0
        }
    }
}
