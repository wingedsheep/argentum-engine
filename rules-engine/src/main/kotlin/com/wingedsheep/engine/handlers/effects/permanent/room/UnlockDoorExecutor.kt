package com.wingedsheep.engine.handlers.effects.permanent.room

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.room.RoomDoorUnlocker
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.sdk.scripting.effects.UnlockDoorEffect
import kotlin.reflect.KClass

/**
 * Executor for [UnlockDoorEffect] — the resolution-time "unlock a door" instruction (CR 709.5f).
 *
 * Resolves the targeted Room and unlocks one of its locked doors via the shared
 * [RoomDoorUnlocker], so it emits the same `DoorUnlockedEvent` / `RoomFullyUnlockedEvent` the
 * unlock-cost special action emits and "When you unlock this door" triggers (CR 709.5h) fire
 * normally off the returned events.
 *
 * The target is "up to one" (optional): a fully-unlocked Room is never offered as a target
 * (the `hasLockedDoor()` targeting restriction), and choosing no target resolves as a harmless
 * no-op. When the Room has more than one locked door (it entered without being cast, CR 709.5d),
 * its first locked face is unlocked — CR 709.5f leaves the choice of which door to the controller,
 * which this models deterministically since the single-locked-door case (a normally-cast Room)
 * carries no real choice.
 */
class UnlockDoorExecutor(
    private val staticAbilityHandler: StaticAbilityHandler,
) : EffectExecutor<UnlockDoorEffect> {

    override val effectType: KClass<UnlockDoorEffect> = UnlockDoorEffect::class

    override fun execute(
        state: GameState,
        effect: UnlockDoorEffect,
        context: EffectContext
    ): EffectResult {
        // "Up to one target": no chosen target → nothing to unlock (not an error).
        val roomId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state, emptyList())

        val container = state.getEntity(roomId)
            ?: return EffectResult.success(state, emptyList())
        val room = container.get<RoomComponent>()
            ?: return EffectResult.success(state, emptyList())

        // Choose a locked door to unlock (CR 709.5f). May be empty if the Room was unlocked in
        // response to the trigger going on the stack — then this resolves as a no-op.
        val lockedFace = room.lockedFaces.firstOrNull()
            ?: return EffectResult.success(state, emptyList())

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId
        val (newState, events) = RoomDoorUnlocker.unlock(state, roomId, lockedFace.id, controllerId, staticAbilityHandler)
        return EffectResult.success(newState, events)
    }
}
