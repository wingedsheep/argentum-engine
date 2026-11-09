package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TurnSkippedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.OptionalSkipTurnWith
import com.wingedsheep.sdk.scripting.SkipExtraTurns
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `SkipExtraTurns` — "If a player would begin an extra turn, that player skips that turn
 * instead" (Ugin's Nexus) and its opponent-scoped form (Trouble in Pairs).
 *
 * The rulings of both cards are the spec: the extra turn is created normally and skipped only when
 * it would *begin* (so a source that has left by then lets the turn happen); a skipped turn never
 * begins, so the riders bound to it ("lose the game at that turn's end step", Kang's power-up
 * lockout) never apply; the turn order carries on exactly as if the extra turn never existed.
 */
class ExtraTurnSkipReplacementTest : FunSpec({

    val eachPlayer = card("Skip Extra Turns (Each)") {
        typeLine = "Artifact"
        replacementEffect(SkipExtraTurns())
    }
    val opponentsOnly = card("Skip Extra Turns (Opponents)") {
        typeLine = "Artifact"
        replacementEffect(SkipExtraTurns(EventPattern.ExtraTurnEvent(Player.EachOpponent)))
    }
    val vault = card("Optional Turn Skipper") {
        typeLine = "Artifact"
        replacementEffect(OptionalSkipTurnWith(Effects.Untap(EffectTarget.Self),
            restrictions = listOf(Conditions.SourceIsTapped)))
    }

    fun driver(players: Int = 2): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(eachPlayer, opponentsOnly, vault))
        val ids = d.initMultiplayer(List(players) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe ids[0]
        return d to ids
    }

    fun GameTestDriver.takeExtraTurn(taker: EntityId, loseAtEndStep: Boolean = false, powerUp: Boolean = false) {
        val result = services.effectExecutorRegistry.execute(state,
            Effects.TakeExtraTurn(EffectTarget.Controller, loseAtEndStep, powerUp),
            EffectContext(sourceId = null, controllerId = taker))
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.endTurn(): List<com.wingedsheep.engine.core.GameEvent> {
        val result = services.turnManager.endTurn(state)
        result.error shouldBe null
        replaceState(result.state)
        return result.events
    }

    fun GameTestDriver.begun(player: EntityId) = state.getEntity(player)?.get<ExtraTurnsComponent>()?.begun ?: 0
    fun GameTestDriver.pending(player: EntityId) = state.getEntity(player)?.get<ExtraTurnsComponent>()?.pending?.size ?: 0

    test("a player-scoped source skips its own controller's extra turn too (Ugin's Nexus reading)") {
        val (d, p) = driver()
        d.putPermanentOnBattlefield(p[0], eachPlayer.name)
        d.takeExtraTurn(p[0])
        d.pending(p[0]) shouldBe 1
        d.endTurn()
        withClue("the extra turn is skipped, so the next regular turn is the opponent's") {
            d.activePlayer shouldBe p[1]
            d.begun(p[0]) shouldBe 0
        }
    }

    test("an opponent-scoped source skips opponents' extra turns, never its controller's") {
        val (d, p) = driver()
        d.putPermanentOnBattlefield(p[0], opponentsOnly.name)
        d.takeExtraTurn(p[0])
        d.endTurn()
        withClue("the controller's own extra turn begins") {
            d.activePlayer shouldBe p[0]
            d.begun(p[0]) shouldBe 1
        }
    }

    test("the source is read as the turn would begin, not as it was created") {
        val (d, p) = driver()
        d.takeExtraTurn(p[0])
        // Put the source out only after the extra turn already exists.
        d.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        val events = d.endTurn()
        events.filterIsInstance<TurnSkippedEvent>().single().playerId shouldBe p[0]
        d.activePlayer shouldBe p[1]
    }

    test("a source that left before the turn would begin lets the extra turn happen") {
        val (d, p) = driver()
        val source = d.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        d.takeExtraTurn(p[0])
        d.replaceState(d.state.removeFromZone(
            com.wingedsheep.engine.state.ZoneKey(p[1], com.wingedsheep.sdk.core.Zone.BATTLEFIELD), source))
        d.endTurn()
        d.activePlayer shouldBe p[0]
        d.begun(p[0]) shouldBe 1
    }

    test("a skipped extra turn leaves the turn order as if it never existed (3 players, active player's extra turn)") {
        val (d, p) = driver(3)
        d.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        d.takeExtraTurn(p[0])
        val order = (1..4).map { d.endTurn(); d.activePlayer!! }
        order shouldBe listOf(p[1], p[2], p[0], p[1])
        d.begun(p[0]) shouldBe 0
    }

    test("without the source the same extra turn begins first (control for the 3-player order)") {
        val (d, p) = driver(3)
        d.takeExtraTurn(p[0])
        val order = (1..4).map { d.endTurn(); d.activePlayer!! }
        order shouldBe listOf(p[0], p[1], p[2], p[0])
        d.begun(p[0]) shouldBe 1
    }

    test("a non-active player's skipped extra turn also leaves the order intact") {
        val (d, p) = driver(3)
        d.putPermanentOnBattlefield(p[0], opponentsOnly.name)
        d.takeExtraTurn(p[2]) // taken during p0's own turn, by p2
        val order = (1..3).map { d.endTurn(); d.activePlayer!! }
        order shouldBe listOf(p[1], p[2], p[0])
        d.begun(p[2]) shouldBe 0

        val (c, q) = driver(3)
        c.takeExtraTurn(q[2])
        withClue("control: p2's extra turn comes right after the turn it was taken in") {
            (1..4).map { c.endTurn(); c.activePlayer!! } shouldBe listOf(q[2], q[1], q[2], q[0])
        }
    }

    test("a skipped extra turn never applies its lose-the-game rider; a begun one does") {
        val (skipped, p) = driver()
        skipped.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        skipped.takeExtraTurn(p[0], loseAtEndStep = true)
        skipped.endTurn()
        skipped.activePlayer shouldBe p[1]
        skipped.state.getEntity(p[0])?.has<LoseAtEndStepComponent>() shouldBe false
        skipped.endTurn()
        skipped.activePlayer shouldBe p[0]
        skipped.passPriorityUntil(Step.END)
        withClue("p0's regular turn ends without losing") {
            skipped.state.getEntity(p[0])?.has<PlayerLostComponent>() shouldBe false
        }

        val (begun, q) = driver()
        begun.takeExtraTurn(q[0], loseAtEndStep = true)
        withClue("nothing is applied until the extra turn begins") {
            begun.state.getEntity(q[0])?.has<LoseAtEndStepComponent>() shouldBe false
        }
        begun.endTurn()
        begun.activePlayer shouldBe q[0]
        begun.state.getEntity(q[0])?.get<LoseAtEndStepComponent>()?.turnsUntilLoss shouldBe 0
        begun.passPriorityUntil(Step.END)
        begun.state.getEntity(q[0])?.has<PlayerLostComponent>() shouldBe true
    }

    test("an extra turn created on an opponent's turn still loses at its own end step") {
        val (d, p) = driver()
        d.endTurn() // p1's turn
        d.activePlayer shouldBe p[1]
        d.takeExtraTurn(p[0], loseAtEndStep = true) // e.g. Final Fortune at instant speed
        d.endTurn() // p0's extra turn
        d.activePlayer shouldBe p[0]
        d.passPriorityUntil(Step.END)
        d.state.getEntity(p[0])?.has<PlayerLostComponent>() shouldBe true
    }

    test("the power-up lockout is stamped on the extra turn that begins, and not at all when it is skipped") {
        val (skipped, p) = driver()
        skipped.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        skipped.takeExtraTurn(p[0], powerUp = true)
        skipped.endTurn()
        skipped.endTurn()
        skipped.state.powerUpRestrictedTurns shouldBe emptySet()

        val (begun, q) = driver()
        begun.takeExtraTurn(q[0], powerUp = true)
        begun.state.powerUpRestrictedTurns shouldBe emptySet()
        begun.endTurn()
        begun.activePlayer shouldBe q[0]
        begun.state.powerUpRestrictedTurns shouldBe setOf(begun.state.turnNumber)
    }

    test("the extra turn created last begins first and carries its own riders (CR 500.7)") {
        val (d, p) = driver()
        d.takeExtraTurn(p[0], loseAtEndStep = true) // created first: goes second
        d.takeExtraTurn(p[0])                        // created last: goes first
        d.pending(p[0]) shouldBe 2
        d.endTurn()
        withClue("the plain extra turn is first") {
            d.activePlayer shouldBe p[0]
            d.state.getEntity(p[0])?.has<LoseAtEndStepComponent>() shouldBe false
        }
        d.endTurn()
        withClue("then the one with the rider") {
            d.activePlayer shouldBe p[0]
            d.state.getEntity(p[0])?.has<LoseAtEndStepComponent>() shouldBe true
        }
    }

    test("a skip replacement that applies leaves no 'begin this turn' answer beside an optional one") {
        val (d, p) = driver()
        val tappedVault = d.putPermanentOnBattlefield(p[0], vault.name)
        d.replaceState(d.state.updateEntity(tappedVault) { it.with(TappedComponent) })
        d.putPermanentOnBattlefield(p[1], opponentsOnly.name)
        d.takeExtraTurn(p[0])
        d.endTurn()

        val decision = d.pendingDecision as ChooseOptionDecision
        decision.playerId shouldBe p[0]
        withClue("the optional skip and the mandatory one — beginning the turn isn't offered") {
            decision.options.size shouldBe 2
        }
        withClue("the slot past the options (\"begin\") is rejected") {
            d.submitDecision(p[0], OptionChosenResponse(decision.id, 2)).error shouldNotBe null
        }

        d.submitDecision(p[0], OptionChosenResponse(decision.id, 1)).error shouldBe null
        withClue("choosing the mandatory skip skips the extra turn; the optional one is untouched") {
            d.activePlayer shouldBe p[1]
            d.begun(p[0]) shouldBe 0
            d.state.getEntity(tappedVault)!!.has<TappedComponent>() shouldBe true
        }
    }
})
