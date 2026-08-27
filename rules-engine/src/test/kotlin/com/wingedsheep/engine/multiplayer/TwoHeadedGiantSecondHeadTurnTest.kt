package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — the *second* head's turn-keyed state (CR 805.4 / 805.8).
 *
 * `GameState.getNextTeam` always hands the turn to a team's first still-in member, so in a shared
 * team turn `activePlayerId` is permanently seat p0 and seat p1 is never "the active player" even
 * though the turn is theirs too. Every site that used to compare against `activePlayerId` — "at the
 * beginning of the next end step you lose the game", step-keyed delayed triggers, "until your next
 * turn" durations, Saga lore counters, "skip your combat phase" — silently skipped p1. These tests
 * pin the team-wide reading for the head that isn't the representative.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order: p0,p1 = team 0 (starting);
 * p2,p3 = team 1.
 */
class TwoHeadedGiantSecondHeadTurnTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().also { it.register(TestCards.all) }

    fun boot(): Triple<GameState, List<EntityId>, ActionProcessor> {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return Triple(result.state, result.playerIds, ActionProcessor(registry()))
    }

    fun handSize(state: GameState, p: EntityId) = state.getZone(ZoneKey(p, Zone.HAND)).size

    /** Pass priority (answering any discard decision) until [predicate] holds or the cap is hit. */
    fun drive(start: GameState, proc: ActionProcessor, cap: Int = 400, predicate: (GameState) -> Boolean): GameState {
        var state = start
        var n = 0
        while (!predicate(state)) {
            check(++n < cap) { "drive never reached target (step=${state.step}, active=${state.activePlayerId}, turn=${state.turnNumber})" }
            if (state.gameOver) break
            val pending = state.pendingDecision
            if (pending is com.wingedsheep.engine.core.SelectCardsDecision) {
                val resp = com.wingedsheep.engine.core.CardsSelectedResponse(pending.id, pending.options.take(pending.minSelections))
                state = proc.process(state, com.wingedsheep.engine.core.SubmitDecision(pending.playerId, resp)).result.newState
                continue
            }
            val prio = state.priorityPlayerId ?: break
            // An attacking player who hasn't declared yet must declare (here: nothing) before passing.
            if (state.step == Step.DECLARE_ATTACKERS && state.isActiveTurnFor(prio) &&
                state.getEntity(prio)?.has<AttackersDeclaredThisCombatComponent>() != true
            ) {
                state = proc.process(state, DeclareAttackers(prio, emptyMap())).result.newState
                continue
            }
            val result = proc.process(state, PassPriority(prio)).result
            check(result.isSuccess || result.isPaused) { "pass by $prio at ${state.step} failed: ${result.error}" }
            state = result.newState
        }
        return state
    }

    /** A vanilla permanent on [owner]'s battlefield with the given type line. */
    fun GameState.withPermanent(owner: EntityId, name: String, typeLine: String, extra: ComponentContainer.() -> ComponentContainer = { this }): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.parse("{1}"),
                typeLine = TypeLine.parse(typeLine),
                ownerId = owner
            ),
            OwnerComponent(owner),
            ControllerComponent(owner)
        ).extra()
        return withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id) to id
    }

    test("the second head's 'lose at the next end step' fires on the team's end step (Final Fortune, CR 805.8)") {
        val (base, p, proc) = boot()
        base.activePlayerId shouldBe p[0]
        val armed = base.updateEntity(p[1]) { it.with(LoseAtEndStepComponent(turnsUntilLoss = 0)) }

        val atEnd = drive(armed, proc) { it.gameOver || (it.step == Step.END && it.activePlayerId != p[0]) }

        // p1 lost at team 0's end step, which takes the whole team down (CR 810.8a).
        atEnd.getEntity(p[1])?.has<PlayerLostComponent>() shouldBe true
        atEnd.getEntity(p[0])?.has<PlayerLostComponent>() shouldBe true
        atEnd.gameOver.shouldBeTrue()
    }

    test("'lose at the next end step' is stopped by a can't-lose grant, including a teammate's (CR 104.3 / 810.8a)") {
        val (base, p, proc) = boot()
        // Platinum Angel-style grant under p0; Final Fortune's marker on the teammate p1.
        val (withAngel, _) = base.withPermanent(p[0], "Team Angel", "Artifact") {
            with(com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent())
        }
        val armed = withAngel.updateEntity(p[1]) { it.with(LoseAtEndStepComponent(turnsUntilLoss = 0)) }

        val atEnd = drive(armed, proc) { it.gameOver || (it.step == Step.END && it.activePlayerId == p[0]) }

        io.kotest.assertions.withClue(
            "lost: " + p.map { it to atEnd.getEntity(it)?.get<PlayerLostComponent>()?.reason } +
                " step=${atEnd.step} active=${atEnd.activePlayerId} turn=${atEnd.turnNumber}"
        ) {
            atEnd.gameOver.shouldBeFalse()
        }
        atEnd.getEntity(p[1])?.has<PlayerLostComponent>() shouldBe false
        // The delayed loss resolved and did nothing — it doesn't lie in wait for the next end step.
        atEnd.getEntity(p[1])?.has<LoseAtEndStepComponent>() shouldBe false
    }

    test("hijacking one head controls the whole team on its next turn (CR 805.8)") {
        val (base, p, proc) = boot()
        // p2 resolves a Mindslaver aimed at p1 (the second head). The executor schedules the
        // hijack on p1's whole shared-turn team.
        val scheduled = com.wingedsheep.engine.handlers.effects.player.HijackNextTurnExecutor().execute(
            base,
            com.wingedsheep.sdk.scripting.effects.HijackNextTurnEffect(
                target = com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.You)
            ),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = p[1])
        ).newState
        // (Player.You resolves to p1 here; the controller field is what the hijack records.)
        p.take(2).forEach { head ->
            scheduled.getEntity(head)?.get<com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent>()
                ?.state shouldBe com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent.HijackState.SCHEDULED
        }
        // Re-point the recorded controller at the opposing head so the engagement below is a real
        // "opponent drives your team" case.
        val armed = p.take(2).fold(scheduled) { s, head ->
            s.updateEntity(head) { c ->
                val h = c.get<com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent>()!!
                c.with(h.copy(controllerId = p[2]))
            }
        }

        // Team 0's *next* turn: drive through team 1's turn and back.
        val onTeam1 = drive(armed, proc) { it.activePlayerId == p[2] && it.step == Step.PRECOMBAT_MAIN }
        onTeam1.actorFor(p[0]) shouldBe p[0]
        val backOnTeam0 = drive(onTeam1, proc) { it.activePlayerId == p[0] && it.step == Step.UPKEEP }
        backOnTeam0.actorFor(p[0]) shouldBe p[2]
        backOnTeam0.actorFor(p[1]) shouldBe p[2]
    }

    test("a step-keyed delayed trigger owned by the second head fires at the team's end step") {
        val (base, p, proc) = boot()
        val delayed = DelayedTriggeredAbility(
            id = "second-head-delayed",
            effect = Effects.DrawCards(1),
            fireAtStep = Step.END,
            sourceId = p[1],
            sourceName = "Second Head Rider",
            controllerId = p[1],
            fireOnPlayerId = p[1]
        )
        val armed = base.copy(delayedTriggers = base.delayedTriggers + delayed)
        val mainHand = handSize(drive(armed, proc) { it.step == Step.PRECOMBAT_MAIN }, p[1])

        // Reach the end step with the trigger consumed and its draw resolved.
        val atEnd = drive(armed, proc) {
            it.step == Step.END && it.delayedTriggers.none { d -> d.id == "second-head-delayed" } && it.stack.isEmpty()
        }
        atEnd.activePlayerId shouldBe p[0]
        handSize(atEnd, p[1]) shouldBe mainHand + 1
    }

    test("the second head's 'until your next turn' effect expires when the team's next turn begins") {
        val (base, p, proc) = boot()
        val (withBear, bear) = base.withPermanent(p[1], "Team Bear", "Creature — Bear")
        val fx = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(3, 3),
                affectedEntities = setOf(bear)
            ),
            duration = Duration.UntilYourNextTurn,
            sourceId = null,
            controllerId = p[1],
            timestamp = withBear.timestamp
        )
        val armed = withBear.copy(floatingEffects = withBear.floatingEffects + fx)

        // It survives the rest of team 0's turn and all of team 1's turn …
        val onTeam1Turn = drive(armed, proc) { it.activePlayerId == p[2] && it.step == Step.PRECOMBAT_MAIN }
        onTeam1Turn.floatingEffects.any { it.id == fx.id } shouldBe true
        // … and wears off once team 0's next turn has untapped.
        val backOnTeam0 = drive(onTeam1Turn, proc) { it.activePlayerId == p[0] && it.step == Step.UPKEEP }
        backOnTeam0.floatingEffects.any { it.id == fx.id }.shouldBeFalse()
    }

    test("the second head's Saga gets a lore counter at the team's precombat main phase (CR 714.3c)") {
        val (base, p, proc) = boot()
        val (withSaga, saga) = base.withPermanent(p[1], "Team Saga", "Enchantment — Saga") { with(SagaComponent()) }
        withSaga.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe null

        // The game boots at the top of team 0's first turn: its precombat main adds the first
        // counter, and team 0's next turn adds the second — never team 1's turn in between.
        val turn1Main = drive(withSaga, proc) { it.step == Step.PRECOMBAT_MAIN }
        turn1Main.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 1
        val team1Main = drive(turn1Main, proc) { it.activePlayerId == p[2] && it.step == Step.PRECOMBAT_MAIN }
        team1Main.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 1
        val nextTeam0Main = drive(team1Main, proc) { it.activePlayerId == p[0] && it.step == Step.PRECOMBAT_MAIN }
        nextTeam0Main.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 2
    }

    test("'skip your combat phase' aimed at the second head skips the team's combat (CR 805.8)") {
        val (base, p, proc) = boot()
        val armed = base.updateEntity(p[1]) { it.with(SkippedTurnPartsComponent(setOf(TurnPart.COMBAT_PHASE))) }

        val visited = mutableListOf<Step>()
        drive(armed, proc) { visited += it.step; it.activePlayerId == p[2] }
        visited.filter { it == Step.DECLARE_ATTACKERS || it == Step.BEGIN_COMBAT }.shouldBeEmpty()
    }
})
