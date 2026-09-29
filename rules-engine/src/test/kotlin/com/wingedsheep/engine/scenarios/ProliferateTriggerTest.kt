package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ProliferatedEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for "whenever [player] proliferates" — `EventPattern.ProliferatedEvent` (CR 701.34).
 *
 * The rules pinned here, from CR 701.34a and the Phyrexia: All Will Be One rulings:
 * - proliferating fires the trigger once per proliferate, after the counters are placed;
 * - "any number" includes zero, and the trigger fires "even if you chose no permanents or
 *   players" — including when nothing had a counter to choose;
 * - "proliferate twice" is two proliferates, so it fires twice;
 * - the player filter is honoured (an opponent's proliferate doesn't fire "whenever you");
 * - the targeted "give it another counter of each kind" form (Powerful Broker) is not the keyword
 *   action and never fires it;
 * - an ability that functions from the graveyard (Voidwing Hybrid) sees it too.
 *
 * Test cards are inline so the tests pin the primitive, not a printed card.
 */
class ProliferateTriggerTest : ScenarioTestBase() {

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private val doubleSpreading = card("Test Double Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate twice."
        spell { effect = Effects.Proliferate() then Effects.Proliferate() }
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

    private val graveWatcher = card("Test Graveyard Proliferate Watcher") {
        manaCost = "{1}"
        typeLine = "Creature — Bat"
        power = 1
        toughness = 1
        oracleText = "When you proliferate, return this card from your graveyard to your hand."
        triggeredAbility {
            trigger = Triggers.you.proliferates()
            triggerZone = Zone.GRAVEYARD
            effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    /** Resolve the stack, answering each proliferate prompt with [pick] (empty = choose nothing). */
    private fun resolveAll(game: TestGame, pick: List<EntityId>): List<ExecutionResult> {
        val results = mutableListOf<ExecutionResult>()
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) results += game.selectCards(pick)
            else results += game.resolveStack()
        }
        return results
    }

    private fun proliferatedEvents(results: List<ExecutionResult>) =
        results.flatMap { it.events }.filterIsInstance<ProliferatedEvent>()

    private fun baseScenario(caster: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(caster, "Forest", 2)
        .withActivePlayer(caster)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        listOf(spreading, doubleSpreading, brokerage, watcher, graveWatcher).forEach(cardRegistry::register)

        test("proliferating fires once, after the chosen counters are placed") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            val results = resolveAll(game, listOf(bears))

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            proliferatedEvents(results).size shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("choosing nothing is still proliferating") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            resolveAll(game, emptyList())

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            withClue("the trigger fires even though nothing was chosen") {
                game.getLifeTotal(1) shouldBe 21
            }
        }

        test("with nothing on the board to choose, the trigger still fires") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardInHand(1, "Test Spreading")
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            val results = resolveAll(game, emptyList())

            proliferatedEvents(results).size shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("proliferate twice fires the trigger twice") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Double Spreading")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Double Spreading").error shouldBe null
            resolveAll(game, listOf(bears))

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            game.getLifeTotal(1) shouldBe 22
        }

        test("an opponent proliferating does not fire \"whenever you proliferate\"") {
            val game = baseScenario(caster = 2)
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
                .withCardInHand(2, "Test Spreading")
                .build()

            game.castSpell(2, "Test Spreading").error shouldBe null
            val results = resolveAll(game, emptyList())

            proliferatedEvents(results).size shouldBe 1
            game.getLifeTotal(1) shouldBe 20
        }

        test("the targeted one-object form is not proliferating") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Watcher")
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
            val results = resolveAll(game, emptyList())

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            proliferatedEvents(results).size shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }

        test("a graveyard-scoped proliferate trigger returns the card to hand") {
            val game = baseScenario()
                .withCardInGraveyard(1, "Test Graveyard Proliferate Watcher")
                .withCardInHand(1, "Test Spreading")
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            resolveAll(game, emptyList())

            game.isInHand(1, "Test Graveyard Proliferate Watcher") shouldBe true
            game.isInGraveyard(1, "Test Graveyard Proliferate Watcher") shouldBe false
        }
    }
}
