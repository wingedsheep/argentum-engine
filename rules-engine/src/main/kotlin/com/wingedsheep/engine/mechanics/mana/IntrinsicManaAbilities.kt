package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Intrinsic mana abilities granted by basic land subtypes (CR 305.7).
 *
 * A land with the Plains/Island/Swamp/Mountain/Forest subtype has the intrinsic
 * mana ability "{T}: Add {W/U/B/R/G}.", regardless of whether the card definition
 * declares it. This applies to non-basic lands that gain a basic land type via
 * type-changing effects (e.g. shock lands like Steam Vents, fetches, Sea's Claim)
 * and to basic lands themselves.
 *
 * Both [com.wingedsheep.engine.legalactions.enumerators.ManaAbilityEnumerator] and
 * [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler] derive
 * intrinsic abilities from the projected basic-land subtypes via this helper, so
 * legal-action enumeration, validation, and resolution all agree.
 */
object IntrinsicManaAbilities {

    private val SUBTYPE_TO_COLOR: Map<String, Color> = mapOf(
        "Plains" to Color.WHITE,
        "Island" to Color.BLUE,
        "Swamp" to Color.BLACK,
        "Mountain" to Color.RED,
        "Forest" to Color.GREEN,
    )

    /**
     * Derives the intrinsic mana abilities a battlefield entity has from its
     * projected basic-land subtypes. Returns an empty list for non-lands and for
     * lands without basic land subtypes (those use the card definition's own
     * activated abilities instead).
     */
    fun forEntity(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
    ): List<ActivatedAbility> {
        val container = state.getEntity(entityId) ?: return emptyList()
        val card = container.get<CardComponent>() ?: return emptyList()
        if (!card.typeLine.isLand) return emptyList()

        val subtypes = projected.getSubtypes(entityId)
        val colors = SUBTYPE_TO_COLOR.entries
            .filter { (subtype, _) -> subtype in subtypes }
            .map { it.value }
        return colors.map(::build)
    }

    /**
     * Looks up an intrinsic mana ability by id. Used by the activation handler so
     * that an `ActivateAbility` action carrying [AbilityId.intrinsicMana] resolves
     * even though no card definition declares it.
     */
    fun lookup(abilityId: AbilityId): ActivatedAbility? {
        if (!abilityId.value.startsWith(INTRINSIC_PREFIX)) return null
        val symbol = abilityId.value.removePrefix(INTRINSIC_PREFIX).singleOrNull() ?: return null
        val color = Color.fromSymbol(symbol) ?: return null
        return build(color)
    }

    private fun build(color: Color): ActivatedAbility = ActivatedAbility(
        id = AbilityId.intrinsicMana(color.symbol),
        cost = AbilityCost.Tap,
        effect = AddManaEffect(color),
        isManaAbility = true,
        timing = TimingRule.ManaAbility,
    )

    private const val INTRINSIC_PREFIX = "intrinsic_mana_"
}
