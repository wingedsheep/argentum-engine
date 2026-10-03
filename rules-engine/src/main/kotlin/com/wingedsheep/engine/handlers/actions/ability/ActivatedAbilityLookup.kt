package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect

/**
 * Result of resolving an activated-ability id on an object.
 *
 * The concrete [ability] is not enough to infer semantic identity: runtime, static, emblem-granted,
 * and intrinsic abilities all have ids, but those ids do not belong to the receiving object's card
 * definition. Keeping the lookup provenance in the result makes the definition-owned cases
 * explicit and keeps a static granter attached only to the branch that actually supplied the
 * ability.
 */
internal sealed interface ActivatedAbilityLookup {
    val ability: ActivatedAbility

    data class DirectDefinition(
        override val ability: ActivatedAbility,
        val identity: AbilityIdentity,
    ) : ActivatedAbilityLookup

    data class DefinitionDerivedClass(
        override val ability: ActivatedAbility,
        val identity: AbilityIdentity,
    ) : ActivatedAbilityLookup

    /** Added to the object's copiable values by a copy exception (CR 707.9a) — no definition owns it. */
    data class CopyException(override val ability: ActivatedAbility) : ActivatedAbilityLookup

    data class RuntimeGranted(override val ability: ActivatedAbility) : ActivatedAbilityLookup

    data class StaticGranted(
        override val ability: ActivatedAbility,
        val granterId: EntityId,
    ) : ActivatedAbilityLookup

    data class EmblemGranted(override val ability: ActivatedAbility) : ActivatedAbilityLookup

    data class Intrinsic(override val ability: ActivatedAbility) : ActivatedAbilityLookup

    val staticGranterId: EntityId?
        get() = (this as? StaticGranted)?.granterId

    val definitionIdentity: AbilityIdentity?
        get() = when (this) {
            is DirectDefinition -> identity
            is DefinitionDerivedClass -> identity
            is CopyException, is RuntimeGranted, is StaticGranted, is EmblemGranted, is Intrinsic -> null
        }
}

/**
 * Stage 1 of activation (CR 602.2a): find the ability the player named on the object, keeping
 * how it was found ([ActivatedAbilityLookup]).
 */
internal class ActivatedAbilityResolver(
    private val cardRegistry: CardRegistry,
    private val castPermissionUtils: CastPermissionUtils,
) {

    /**
     * Resolve [abilityId] without discarding how the concrete ability was found.
     *
     * The order matches legal-action enumeration and the historical handler lookup. In
     * particular, an id directly declared by the current definition wins over an equal id from a
     * grant. Only the first two branches prove definition ownership; every other branch retains a
     * concrete id for execution while remaining semantically identityless. Static and emblem
     * grants come from the same shared resolvers used by legal-action enumeration, so the engine
     * cannot advertise one grant set and validate another.
     */
    fun lookup(
        state: GameState,
        sourceId: EntityId,
        abilityId: AbilityId,
    ): ActivatedAbilityLookup? {
        val container = state.getEntity(sourceId) ?: return null
        val cardDefinitionId = container.get<CardComponent>()?.cardDefinitionId ?: return null
        val cardDef = cardRegistry.getCard(cardDefinitionId)
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel

        cardDef?.script?.effectiveActivatedAbilities(classLevel)
            ?.firstOrNull { it.id == abilityId }
            ?.let {
                return ActivatedAbilityLookup.DirectDefinition(
                    it,
                    AbilityIdentity(cardDefinitionId, it.id),
                )
            }

        cardDef?.let { findClassLevelUpAbility(it, container, abilityId) }
            ?.let {
                return ActivatedAbilityLookup.DefinitionDerivedClass(
                    it,
                    AbilityIdentity(cardDefinitionId, it.id),
                )
            }

        container.get<CardComponent>()?.copyActivatedAbilities
            ?.firstOrNull { it.id == abilityId }
            ?.let { return ActivatedAbilityLookup.CopyException(it) }

        state.grantedActivatedAbilities
            .firstOrNull { it.entityId == sourceId && it.ability.id == abilityId }
            ?.ability
            ?.let { return ActivatedAbilityLookup.RuntimeGranted(it) }

        castPermissionUtils.getStaticGrantedAbilitiesWithGranter(sourceId, state)
            .firstOrNull { it.ability.id == abilityId }
            ?.let {
                return ActivatedAbilityLookup.StaticGranted(it.ability, it.granterId)
            }

        castPermissionUtils.getEmblemGrantedActivatedAbilities(sourceId, state)
            .firstOrNull { it.id == abilityId }
            ?.let { return ActivatedAbilityLookup.EmblemGranted(it) }

        return resolveIntrinsicManaAbility(state, sourceId, abilityId)
            ?.let { ActivatedAbilityLookup.Intrinsic(it) }
    }

    /**
     * Resolve an intrinsic mana ability granted by a basic-land subtype (CR 305.7).
     * Returns the synthesized ability only if the entity currently projects the
     * matching basic-land subtype, so an `intrinsic_mana_R` request on a land that
     * isn't a Mountain in the projected state is rejected.
     */
    private fun resolveIntrinsicManaAbility(
        state: GameState,
        sourceId: EntityId,
        abilityId: AbilityId,
    ): ActivatedAbility? {
        val ability = IntrinsicManaAbilities.lookup(abilityId) ?: return null
        val color = (ability.effect as? AddManaEffect)?.color ?: return null
        val expectedSubtype = when (color) {
            Color.WHITE -> "Plains"
            Color.BLUE -> "Island"
            Color.BLACK -> "Swamp"
            Color.RED -> "Mountain"
            Color.GREEN -> "Forest"
        }
        val subtypes = state.projectedState.getSubtypes(sourceId)
        if (expectedSubtype !in subtypes) return null
        return ability
    }

    /**
     * Find a class level-up ability by its deterministic ID.
     * Returns the generated ActivatedAbility if the ID matches a valid level-up,
     * or null if this isn't a class level-up ability.
     */
    private fun findClassLevelUpAbility(
        cardDef: CardDefinition,
        container: ComponentContainer,
        abilityId: AbilityId
    ): ActivatedAbility? {
        if (!abilityId.value.startsWith("class_level_up_")) return null
        val classLevelComponent = container.get<ClassLevelComponent>() ?: return null
        val targetLevel = abilityId.value.removePrefix("class_level_up_").toIntOrNull() ?: return null
        if (targetLevel != classLevelComponent.currentLevel + 1) return null
        val levelAbility = cardDef.classLevels.find { it.level == targetLevel } ?: return null
        return ActivatedAbility(
            id = AbilityId.classLevelUp(targetLevel),
            cost = AbilityCost.Atom(CostAtom.Mana(levelAbility.cost)),
            effect = LevelUpClassEffect(targetLevel),
            timing = TimingRule.SorcerySpeed,
            descriptionOverride = "Level up to level $targetLevel"
        )
    }
}
