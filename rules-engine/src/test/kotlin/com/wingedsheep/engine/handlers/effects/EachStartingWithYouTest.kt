package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Player.EachStartingWithYou] — "starting with you, each player …": the controller first, then the
 * other players in turn order from the one after them, regardless of whose turn it is. Contrasted
 * with [Player.ActivePlayerFirst], which rotates to the active player.
 */
class EachStartingWithYouTest : FunSpec({
    val a = EntityId.generate()
    val b = EntityId.generate()
    val c = EntityId.generate()
    val d = EntityId.generate()

    fun board(active: EntityId): GameState = listOf(a, b, c, d).fold(
        GameState(activePlayerId = active, turnOrder = listOf(a, b, c, d), turnNumber = 3)
    ) { s, p -> s.withEntity(p, ComponentContainer.EMPTY) }

    fun resolve(state: GameState, player: Player, controller: EntityId): List<EntityId> =
        TargetResolutionUtils.resolvePlayerTargets(
            EffectTarget.PlayerRef(player),
            state,
            EffectContext(sourceId = null, controllerId = controller)
        )

    test("rotates to the controller, not the active player") {
        val state = board(active = b)
        resolve(state, Player.EachStartingWithYou, controller = d) shouldBe listOf(d, a, b, c)
        resolve(state, Player.ActivePlayerFirst, controller = d) shouldBe listOf(b, c, d, a)
    }

    test("on the controller's own turn it matches APNAP order") {
        val state = board(active = c)
        resolve(state, Player.EachStartingWithYou, controller = c) shouldBe state.apnapOrder
    }

    test("players who have left the game are skipped") {
        val state = board(active = a).withEntity(a, ComponentContainer.of(PlayerLostComponent(LossReason.LIFE_ZERO)))
        resolve(state, Player.EachStartingWithYou, controller = c) shouldBe listOf(c, d, b)
    }
})
