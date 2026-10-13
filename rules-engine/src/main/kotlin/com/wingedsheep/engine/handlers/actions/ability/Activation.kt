package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * An activation once it has been announced (CR 602.2a–b): the ability is known, its total cost is
 * determined, and X is bound as far as the action already allows. Built by
 * `ActivateAbilityHandler.announce` and read by every later stage — the choice pauses, payment,
 * and stack placement / mana-ability resolution — so none of them re-derives it.
 */
internal data class Activation(
    val action: ActivateAbility,
    val activationReferences: ObjectReferenceEnvironment,
    val container: ComponentContainer,
    val cardComponent: CardComponent,
    val sourceName: String,
    val abilityLookup: ActivatedAbilityLookup,
    val textReplacement: TextReplacementComponent?,
    /** The total cost, as [ActivationCostTotaller.total] determines it. */
    val effectiveCost: AbilityCost,
    /**
     * X for this activation: an X the ability's own text defines (CR 107.3c), else one measured
     * from a variable-permanents cost choice, else the action's announced X.
     */
    val effectiveXValue: Int?,
) {
    val ability: ActivatedAbility get() = abilityLookup.ability
    val staticGranterId: EntityId? get() = abilityLookup.staticGranterId

    /** The ability's target requirements with text-changing effects applied. */
    val targetRequirements: List<TargetRequirement>
        get() = if (textReplacement != null) {
            (ability.targetRequirements + modalTargetRequirements()).map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements + modalTargetRequirements()
        }

    private fun modalTargetRequirements(): List<TargetRequirement> =
        ((ability.effect as? com.wingedsheep.sdk.scripting.effects.ModalEffect))?.let { modal ->
            action.chosenModes.orEmpty().flatMap { modal.modes[it].targetRequirements }
        } ?: emptyList()

    val variablePermanentsCost: CostAtom.VariablePermanents?
        get() = effectiveCost.extractVariablePermanentsCost()

    /** The permanents chosen for a variable-permanents cost, when the action carries them. */
    val chosenForCost: List<EntityId>
        get() = action.costPayment?.variableCostPermanents ?: emptyList()
}
