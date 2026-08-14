package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit coverage for the tap/untap atoms ([tap] / [untapOrConsumeStun]) — the single chokepoint
 * every battlefield tap and untap routes through (enforced corpus-wide by
 * [com.wingedsheep.engine.hygiene.TapEventEnforcementTest]).
 *
 * The atoms exist so the state change and its [TappedEvent] / [UntappedEvent] can never drift
 * apart — the bug that silently dropped station and declare-attackers taps. These tests pin that
 * contract: every real transition emits exactly one event, and every non-transition emits none
 * (CR 603.2f / 603.6e), including the stun-counter replacement (CR 122.1d).
 */
class TapAtomTest : ScenarioTestBase() {

    init {
        test("tap() taps an untapped permanent and emits exactly one TappedEvent") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spined Wurm", tapped = false)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (newState, event) = tap(game.state, wurm)

            newState.getEntity(wurm)?.has<TappedComponent>() shouldBe true
            event.shouldBeInstanceOf<TappedEvent>()
            event.entityId shouldBe wurm
        }

        test("tap() attributes the tap to the permanent's controller by default") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Spined Wurm", tapped = false)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (_, event) = tap(game.state, wurm)

            // Every tap a permanent's own controller performs on it — a cost payment, a mana
            // ability, crew/saddle, declaring it as an attacker — is theirs, so the default has to
            // be the controller rather than "unattributed". This is what keeps "whenever you tap a
            // creature an opponent controls" from firing on an opponent's own taps.
            event?.tappedById shouldBe game.player2Id
        }

        test("tap() attributes a control-changed permanent to the player now wielding it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Spined Wurm", tapped = false)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!
            game.state = game.state.addFloatingEffect(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(game.player1Id),
                affectedEntities = setOf(wurm),
                duration = Duration.EndOfTurn,
                context = EffectContext(sourceId = wurm, controllerId = game.player1Id),
            )
            // Sanity: only the projection flipped; the base component still says player 2.
            game.state.getEntity(wurm)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id

            val (_, event) = tap(game.state, wurm)

            // A stolen creature tapped for a cost or to attack is tapped by the player who stole it,
            // not by its owner — so the default reads projected control, not ControllerComponent.
            event?.tappedById shouldBe game.player1Id
        }

        test("tap() records an explicit tapper, so an effect's controller owns the tap") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Spined Wurm", tapped = false)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (_, event) = tap(game.state, wurm, tappedById = game.player1Id)

            event?.tappedById shouldBe game.player1Id
        }

        test("tap() is a no-op with no event on an already-tapped permanent (CR 603.2f)") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spined Wurm", tapped = true)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (newState, event) = tap(game.state, wurm)

            event shouldBe null
            newState shouldBe game.state
        }

        test("untapOrConsumeStun() untaps a tapped permanent and emits exactly one UntappedEvent") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spined Wurm", tapped = true)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (newState, events) = untapOrConsumeStun(game.state, wurm)

            newState.getEntity(wurm)?.has<TappedComponent>() shouldBe false
            events.size shouldBe 1
            events.single().shouldBeInstanceOf<UntappedEvent>()
        }

        test("untapOrConsumeStun() consumes a stun counter instead of untapping, emitting no event (CR 122.1d)") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spined Wurm", tapped = true)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!
            game.state = game.state.updateEntity(wurm) {
                it.with(CountersComponent(mapOf(CounterType.STUN to 2)))
            }

            val (newState, events) = untapOrConsumeStun(game.state, wurm)

            // Stays tapped; one stun counter removed; no UntappedEvent (it never became untapped).
            newState.getEntity(wurm)?.has<TappedComponent>() shouldBe true
            newState.getEntity(wurm)?.get<CountersComponent>()?.getCount(CounterType.STUN) shouldBe 1
            events.shouldBeEmpty()
        }

        test("untapOrConsumeStun() is a no-op with no event on an already-untapped permanent") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Spined Wurm", tapped = false)
                .build()
            val wurm = game.findPermanent("Spined Wurm")!!

            val (newState, events) = untapOrConsumeStun(game.state, wurm)

            events.shouldBeEmpty()
            newState shouldBe game.state
        }
    }
}
