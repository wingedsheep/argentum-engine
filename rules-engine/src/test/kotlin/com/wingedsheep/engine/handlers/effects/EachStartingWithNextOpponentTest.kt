package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Player.EachStartingWithNextOpponent] — "starting with the next opponent in turn order, each
 * player …": the first opponent after the controller, round the table, the controller last.
 */
class EachStartingWithNextOpponentTest : FunSpec({
    val a = EntityId.generate()
    val b = EntityId.generate()
    val c = EntityId.generate()
    val d = EntityId.generate()

    fun board(active: EntityId): GameState = listOf(a, b, c, d).fold(
        GameState(activePlayerId = active, turnOrder = listOf(a, b, c, d), turnNumber = 3)
    ) { s, p -> s.withEntity(p, ComponentContainer.EMPTY) }

    fun resolve(state: GameState, controller: EntityId): List<EntityId> =
        TargetResolutionUtils.resolvePlayerTargets(
            EffectTarget.PlayerRef(Player.EachStartingWithNextOpponent),
            state,
            EffectContext(sourceId = null, controllerId = controller)
        )

    test("starts after the controller and ends with them, whoever is active") {
        resolve(board(active = a), controller = b) shouldBe listOf(c, d, a, b)
        resolve(board(active = c), controller = d) shouldBe listOf(a, b, c, d)
    }

    test("players who have left the game are skipped") {
        val state = board(active = b).withEntity(c, ComponentContainer.of(PlayerLostComponent(LossReason.LIFE_ZERO)))
        resolve(state, controller = b) shouldBe listOf(d, a, b)
    }

    test("in a team game a teammate seated next is passed over until after the controller") {
        // Teams {a, b} and {c, d}: from a, b is a teammate, so the next opponent is c.
        val state = listOf(a to 0, b to 0, c to 1, d to 1).fold(board(active = a)) { s, (p, team) ->
            s.withEntity(p, ComponentContainer.of(TeamComponent(team)))
        }
        resolve(state, controller = a) shouldBe listOf(c, d, a, b)
    }
})
