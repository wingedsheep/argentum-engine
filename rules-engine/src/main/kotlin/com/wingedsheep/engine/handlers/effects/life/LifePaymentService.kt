package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceLifePaymentWithLibraryExile
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The single choke point for **paying** life (CR 118.8) — life spent to satisfy a cost, as opposed
 * to life lost to damage or to a "you lose N life" effect.
 *
 * Every payment site in the engine funnels here: cost atoms ([com.wingedsheep.sdk.scripting.costs.CostAtom.PayLife]),
 * additional casting costs, Phyrexian-style and ward life costs paid through a continuation,
 * pain-cost mana abilities, and the `PayLife` / `PayDynamicLife` resolution effects. They used to
 * each hand-roll the same read-subtract-write-emit; centralising it means a replacement effect that
 * intercepts life payments has exactly one place to hook in.
 *
 * The one such replacement today is [ReplaceLifePaymentWithLibraryExile] (Ashiok, Wicked
 * Manipulator). It is deliberately *not* routed through [com.wingedsheep.engine.replacement.ReplacementEffectProcessor]:
 * that pipeline models replaceable events that can pause on a competing-replacement choice, and a
 * life payment can't. Ashiok's ability is mandatory and unsplittable, and once applied the payment
 * is no longer a life payment, so a second copy has nothing left to replace — there is never a
 * choice to present. This mirrors how `ModifyLifeLoss` / `LifeLossFloor` are applied by a direct
 * battlefield scan in [DamageUtils].
 *
 * Life *loss* is a different event and keeps its own path ([DamageUtils.loseLife] with
 * `LifeChangeReason.LIFE_LOSS`, or the damage pipeline) — which is why Ashiok's reminder text says
 * damage and unpayable costs still cause you to lose life.
 */
object LifePaymentService {

    /**
     * Pay [amount] life from [payerId], applying any life-payment replacement first.
     *
     * @return the updated state paired with the events the payment produced, or `null` when
     *   [payerId] has no life total (nothing mutated) so cost callers can surface a payment
     *   failure. A non-positive [amount] is a no-op that still succeeds.
     */
    fun pay(state: GameState, payerId: EntityId, amount: Int): Pair<GameState, List<GameEvent>>? {
        if (state.getEntity(payerId)?.get<LifeTotalComponent>() == null) return null
        if (amount <= 0) return state to emptyList()

        exileFromLibraryInstead(state, payerId, amount)?.let { return it }

        val (newState, event) = DamageUtils.loseLife(state, payerId, amount, LifeChangeReason.PAYMENT)
        return newState to listOfNotNull(event)
    }

    /**
     * Apply [ReplaceLifePaymentWithLibraryExile] if [payerId] is under one and their library holds
     * at least [amount] cards, exiling that many cards off the top instead of deducting life.
     *
     * The exile goes through [ZoneTransitionService.moveToZoneBatch] so it is a real zone change:
     * it emits `ZoneChangeEvent`s, feeds the `CARDS_PUT_INTO_EXILE` turn tracker (which Ashiok's
     * own Nightmare tokens read), and lets leaves-the-library and enters-exile effects see it.
     *
     * Returns `null` when no replacement applies, so the caller pays life normally.
     */
    private fun exileFromLibraryInstead(
        state: GameState,
        payerId: EntityId,
        amount: Int
    ): Pair<GameState, List<GameEvent>>? {
        if (!hasLibraryExileReplacement(state, payerId)) return null

        val library = state.getZone(ZoneKey(payerId, Zone.LIBRARY))
        // "while your library has at least that many cards in it" — a shallower library means the
        // replacement simply doesn't apply and life is paid as normal (printed ruling).
        if (library.size < amount) return null

        val result = ZoneTransitionService.moveToZoneBatch(state, library.take(amount), Zone.EXILE)
        return result.state to result.events
    }

    /**
     * Whether any battlefield permanent grants [payerId] a life-payment-to-library-exile
     * replacement. Reads the *projected* controller so a stolen Ashiok follows its new controller
     * (control change is a layer-2 effect, so the base `ControllerComponent` still names the
     * original controller).
     */
    private fun hasLibraryExileReplacement(state: GameState, payerId: EntityId): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacements = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue

            for (effect in replacements.replacementEffects) {
                if (effect !is ReplaceLifePaymentWithLibraryExile) continue
                val pattern = effect.appliesTo as? EventPattern.LifePaymentEvent ?: continue
                val applies = when (pattern.player) {
                    Player.Each, Player.Any -> true
                    Player.You -> payerId == sourceControllerId
                    // Not `!= sourceControllerId`: a teammate is not an opponent (CR 102.3).
                    Player.EachOpponent -> state.isOpponentOf(payerId, sourceControllerId)
                    else -> false
                }
                if (applies) return true
            }
        }
        return false
    }
}
