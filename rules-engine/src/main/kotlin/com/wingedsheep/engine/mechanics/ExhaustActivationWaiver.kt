package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.IgnoreExhaustActivationLimit

/**
 * Whether a player currently has permission to activate exhaust abilities (CR 702.177) as though
 * they hadn't been activated — i.e. whether the "activate only once" memory those abilities carry
 * ([com.wingedsheep.sdk.scripting.ActivationRestriction.Once]) is waived.
 *
 * Lives here rather than in `CastPermissionUtils` because three independent activation-legality
 * paths need the same answer and must not drift: the legal-action enumerators and
 * `ActivateAbilityHandler` (both through `CastPermissionUtils`), and `ManaSolver`'s inlined
 * restriction check for auto-tapping, which deliberately doesn't depend on the legalactions module.
 */
object ExhaustActivationWaiver {

    /**
     * True when some permanent [playerId] controls grants
     * [IgnoreExhaustActivationLimit] and its condition currently holds.
     *
     * Both printed statics (from the card definition, honoring the permanent's current Class level)
     * and statics granted at runtime are scanned. Each waiver's condition is evaluated in the
     * granting permanent's controller's context and re-checked every call, so Elvish Refueler's
     * "During your turn, as long as you haven't activated an exhaust ability this turn" gate stops
     * applying the instant the turn's first exhaust ability is activated.
     */
    fun isWaivedFor(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator
    ): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>()
            val classLevel = state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel
            val printed = card?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.script?.effectiveStaticAbilities(classLevel).orEmpty()
            val granted = state.grantedStaticAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            for (ability in printed + granted) {
                val waiver = ability as? IgnoreExhaustActivationLimit ?: continue
                val condition = waiver.condition ?: return true
                val context = EffectContext(sourceId = entityId, controllerId = playerId)
                if (conditionEvaluator.evaluate(state, condition, context)) return true
            }
        }
        return false
    }
}
