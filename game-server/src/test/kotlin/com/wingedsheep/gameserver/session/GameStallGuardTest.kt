package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * What [GameStallGuard] must and must not call a stall.
 *
 * The direction of the failures matters more here than the numbers do. Missing a wedge costs a
 * session that never ends, a replay that grows forever and a tournament round that blocks; calling a
 * *healthy* game stalled ends a game somebody is playing. So these tests pin both sides: a long
 * game with turns passing is never touched however many actions it takes, and a game that stops
 * handing the turn over is stopped.
 */
class GameStallGuardTest : FunSpec({

    val alice = EntityId.of("alice")
    val bob = EntityId.of("bob")

    fun state(turn: Int, active: EntityId) = GameState(turnNumber = turn, activePlayerId = active)

    test("a game whose turns keep changing hands is never called stalled") {
        val guard = GameStallGuard(maxActionsPerTurn = 10, maxPlayerTurns = 1_000, maxActions = 1_000)

        // 20 turns of 9 actions each — comfortably more actions than the per-turn budget, but the
        // budget is per turn and every turn ends.
        var turn = 0
        repeat(20) {
            turn++
            val seat = if (turn % 2 == 0) bob else alice
            repeat(9) { guard.onActionApplied(state(turn, seat)) shouldBe null }
        }
        guard.stall shouldBe null
        guard.actionCount() shouldBe 180
    }

    test("actions that never hand the turn over are a loop, and the game becomes a draw") {
        val guard = GameStallGuard(maxActionsPerTurn = 10)

        repeat(11) { guard.onActionApplied(state(turn = 3, active = alice)) shouldBe null }
        val stall = guard.onActionApplied(state(turn = 3, active = alice)).shouldNotBeNull()

        stall.code shouldContain "wedged(turn=3"
        // The overlay text has to explain a draw nobody asked for — and cite the rule that says a
        // loop with no way out *is* one, so it doesn't read as the server giving up arbitrarily.
        stall.playerMessage shouldContain "CR 104.4b"
        stall.playerMessage shouldContain "10 actions in a single turn"
    }

    test("the verdict is sticky and reported exactly once") {
        val guard = GameStallGuard(maxActionsPerTurn = 1)

        repeat(3) { guard.onActionApplied(state(turn = 1, active = alice)) }
        val stall = guard.stall.shouldNotBeNull()

        // The session ends the game on the first verdict; anything still in flight afterwards must
        // not produce a second game-over, and must not overwrite the reason the first one gave.
        guard.onActionApplied(state(turn = 1, active = alice)) shouldBe null
        guard.stall shouldBe stall
    }

    test("a game nobody can close out is a draw on the turn limit") {
        val guard = GameStallGuard(maxActionsPerTurn = 100, maxPlayerTurns = 4)

        // Turns pass — so the per-turn budget resets every time and can never fire — but the game
        // is going nowhere all the same. This is the soft-lock shape the per-turn clock cannot see.
        for (turn in 1..4) guard.onActionApplied(state(turn, if (turn % 2 == 0) bob else alice)) shouldBe null
        val stall = guard.onActionApplied(state(turn = 5, active = alice)).shouldNotBeNull()

        stall.code shouldContain "turnLimit(turn=5"
    }

    test("the total-action backstop catches a game that satisfies both other clocks") {
        val guard = GameStallGuard(maxActionsPerTurn = 5, maxPlayerTurns = 1_000, maxActions = 12)

        // Four actions per turn, turns always advancing: under the per-turn budget, under the turn
        // limit, and still a loop that would run until the process died.
        var stall: GameStallGuard.Stall? = null
        var turn = 0
        while (stall == null && turn < 100) {
            turn++
            repeat(4) { stall = stall ?: guard.onActionApplied(state(turn, if (turn % 2 == 0) bob else alice)) }
        }
        stall.shouldNotBeNull().code shouldContain "actionLimit"
        guard.actionCount() shouldBe 13 // the 13th action is the one past the budget of 12
    }

    test("a seat whose actions are all rejected is given up on, and progress forgives it") {
        val guard = GameStallGuard(maxConsecutiveRejections = 3)

        guard.onActionRejected(alice) shouldBe false
        guard.onActionRejected(alice) shouldBe false
        // A seat that manages to act is plainly not out of moves — one rejected block followed by a
        // legal one is ordinary play, and must not accumulate toward a concession.
        guard.onActionApplied(state(turn = 1, active = alice))
        guard.onActionRejected(alice) shouldBe false
        guard.onActionRejected(alice) shouldBe false
        guard.onActionRejected(alice) shouldBe true
    }

    test("rejections are counted per seat, so one stuck AI doesn't concede the other") {
        val guard = GameStallGuard(maxConsecutiveRejections = 2)

        guard.onActionRejected(alice) shouldBe false
        guard.onActionRejected(bob) shouldBe false
        guard.onActionRejected(alice) shouldBe true
    }

    test("the shipped policy lets the record give up before the game does") {
        // Both KDocs claim this ordering, and it is the whole reason a pathological game degrades
        // to a partial replay instead of being ended early: the recording stops first.
        val cap = com.wingedsheep.gameserver.replay.ReplayRecordingPolicy.MAX_RECORDED_ACTIONS
        (cap < GameStallGuard.MAX_ACTIONS) shouldBe true
    }
})
