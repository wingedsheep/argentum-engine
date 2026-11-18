package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.manaAbilitySourceAllowed
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * Whether the auto-payer may tap a permanent's mana abilities for [playerId] — the one question
 * every [ManaSolver] source walk asks before it counts a source.
 *
 * A mana ability is still an activated ability (CR 605.1a), so the statics that stop or tax
 * activated abilities reach it unless they say "unless they're mana abilities":
 * - an activation lock — Phyrexian Revoker's "Activated abilities of sources with the chosen name
 *   can't be activated", Cursed Totem, Clarion Conqueror, Grand Abolisher
 *   ([CastPermissionUtils.isActivationForbidden]);
 * - an activation-cost tax — Oppressive Rays' "Activated abilities of enchanted creature cost {3}
 *   more to activate" ([CastPermissionUtils.taxesManaAbilities]).
 *
 * The manual path already honours both: `ManaAbilityEnumerator` drops a locked source and prices a
 * taxed ability at its total cost, and `ActivateAbilityHandler` refuses or charges it. Auto-pay
 * leaves a locked source alone (activating it is illegal) and a taxed one too — tapping it costs
 * the tax on top, which the solver's free-tap model can't bill, and paying {3} to make one mana is
 * a choice the player makes by activating the ability by hand, after which its mana floats.
 *
 * Built once per walk. The battlefield scan behind [restricted] runs lazily, on the first source
 * that passes the walk's own checks, and is one pass over the statics; only when it finds a lock
 * or tax that could reach a mana ability does any per-source check run, so a board without one
 * (nearly all of them) pays a single scan per walk and nothing per source.
 */
internal class ManaAbilityActivationGate(
    private val state: GameState,
    private val playerId: EntityId,
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val permissions: () -> CastPermissionUtils,
) {
    private val restricted by lazy(LazyThreadSafetyMode.NONE) { mayRestrictManaAbilities(state, cardRegistry) }

    /** True when [playerId] may activate [sourceId]'s mana abilities at their printed cost. */
    fun allows(sourceId: EntityId): Boolean {
        if (!state.manaAbilitySourceAllowed(playerId, sourceId, predicateEvaluator)) return false
        if (!restricted) return true
        val utils = permissions()
        return !utils.isActivationForbidden(state, sourceId, playerId, abilityIsManaAbility = true) &&
            !utils.taxesManaAbilities(state, sourceId, playerId)
    }

    companion object {
        /**
         * Whether any battlefield permanent (printed, Class-level or runtime-granted statics,
         * conditional ones included regardless of their condition) carries a static that could stop
         * or tax a mana ability. A conservative pre-filter: a `true` only switches on the exact
         * per-source checks.
         */
        fun mayRestrictManaAbilities(state: GameState, cardRegistry: CardRegistry): Boolean =
            anyStatic(state, cardRegistry, ::restrictsManaAbilities)

        /**
         * Whether any battlefield static could change what a mana ability costs — a tax that
         * reaches mana abilities, or a [ReduceActivatedAbilityCost] that does. The pre-filter
         * `ManaAbilityEnumerator` asks before pricing each mana ability through
         * [CastPermissionUtils.applyActivatedAbilityCostReduction], the same totalling
         * `ActivateAbilityHandler` charges.
         */
        fun mayChangeManaAbilityCosts(state: GameState, cardRegistry: CardRegistry): Boolean =
            anyStatic(state, cardRegistry, ::changesManaAbilityCosts)

        private fun anyStatic(
            state: GameState,
            cardRegistry: CardRegistry,
            matches: (StaticAbility) -> Boolean,
        ): Boolean {
            fun unwrapped(ability: StaticAbility): Boolean =
                if (ability is ConditionalStaticAbility) unwrapped(ability.ability) else matches(ability)
            for (permanentId in state.getBattlefield()) {
                val card = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                if (def.liveStaticAbilities(state, permanentId).any(::unwrapped)) return true
            }
            return state.grantedStaticAbilities.any { unwrapped(it.ability) }
        }

        private fun restrictsManaAbilities(ability: StaticAbility): Boolean = when (ability) {
            is PreventActivatedAbilities -> ability.abilities.covers(isManaAbility = true, isLoyaltyAbility = false)
            is PlayersCantActivateAbilities -> ability.abilities.covers(isManaAbility = true, isLoyaltyAbility = false)
            is IncreaseActivatedAbilityCost -> taxesManaAbilities(ability)
            else -> false
        }

        private fun changesManaAbilityCosts(ability: StaticAbility): Boolean = when (ability) {
            is IncreaseActivatedAbilityCost -> taxesManaAbilities(ability)
            is ReduceActivatedAbilityCost -> !ability.excludeManaAbilities
            else -> false
        }

        private fun taxesManaAbilities(ability: IncreaseActivatedAbilityCost): Boolean =
            !ability.excludeManaAbilities && !ability.loyaltyAbilitiesOnly
    }
}
