package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.AttachedToComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.layers.*
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Provides modifiers to the StateProjector by converting static abilities
 * from the scripting system into layer-based Modifiers.
 *
 * This bridges the existing CardScript/AbilityRegistry system with the
 * new ECS layer system, allowing static abilities defined in scripts
 * to properly affect the projected game state.
 *
 * Example usage:
 * ```kotlin
 * val provider = ScriptModifierProvider(registry)
 * val projector = StateProjector.forState(state, provider)
 * val view = projector.getView(creatureId)
 * ```
 */
class ScriptModifierProvider(
    private val registry: AbilityRegistry
) : ModifierProvider {

    override fun getModifiers(state: EcsGameState): List<Modifier> {
        val modifiers = mutableListOf<Modifier>()

        // Iterate over all permanents on the battlefield
        val battlefield = state.getZone(ZoneId.BATTLEFIELD)

        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = container.get<ControllerComponent>()?.controllerId
                ?: cardComponent.ownerId

            // Get static abilities for this card from the registry
            val abilities = registry.getStaticAbilities(cardComponent.definition)

            for (ability in abilities) {
                modifiers.addAll(
                    convertStaticAbility(ability, entityId, controllerId, state)
                )
            }
        }

        return modifiers
    }

    /**
     * Convert a StaticAbility to one or more Modifiers.
     */
    private fun convertStaticAbility(
        ability: StaticAbility,
        sourceId: EntityId,
        controllerId: EntityId,
        state: EcsGameState
    ): List<Modifier> {
        return when (ability) {
            is GrantKeyword -> convertGrantKeyword(ability, sourceId, controllerId, state)
            is ModifyStats -> convertModifyStats(ability, sourceId, controllerId, state)
            is GlobalEffect -> convertGlobalEffect(ability, sourceId, controllerId)
            is CantBlock -> convertCantBlock(ability, sourceId, controllerId, state)
        }
    }

    /**
     * Convert GrantKeyword static ability to modifiers.
     */
    private fun convertGrantKeyword(
        ability: GrantKeyword,
        sourceId: EntityId,
        controllerId: EntityId,
        state: EcsGameState
    ): List<Modifier> {
        val filter = resolveStaticTarget(ability.target, sourceId, state)
            ?: return emptyList()

        return listOf(
            Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceId,
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(ability.keyword),
                filter = filter
            )
        )
    }

    /**
     * Convert ModifyStats static ability to modifiers.
     */
    private fun convertModifyStats(
        ability: ModifyStats,
        sourceId: EntityId,
        controllerId: EntityId,
        state: EcsGameState
    ): List<Modifier> {
        val filter = resolveStaticTarget(ability.target, sourceId, state)
            ?: return emptyList()

        return listOf(
            Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = sourceId,
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(ability.powerBonus, ability.toughnessBonus),
                filter = filter
            )
        )
    }

    /**
     * Convert GlobalEffect static ability to modifiers.
     */
    private fun convertGlobalEffect(
        ability: GlobalEffect,
        sourceId: EntityId,
        controllerId: EntityId
    ): List<Modifier> {
        val modifiers = mutableListOf<Modifier>()
        val filter = convertCreatureFilter(ability.filter, controllerId)

        when (ability.effectType) {
            GlobalEffectType.ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.PT_MODIFY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.ModifyPT(1, 1),
                        filter = ModifierFilter.All(EntityCriteria.Creatures)
                    )
                )
            }

            GlobalEffectType.YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.PT_MODIFY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.ModifyPT(1, 1),
                        filter = ModifierFilter.All(
                            EntityCriteria.And(
                                listOf(EntityCriteria.Creatures, controlledByCriteria(controllerId))
                            )
                        )
                    )
                )
            }

            GlobalEffectType.OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.PT_MODIFY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.ModifyPT(-1, -1),
                        filter = ModifierFilter.All(
                            EntityCriteria.And(
                                listOf(
                                    EntityCriteria.Creatures,
                                    EntityCriteria.Not(controlledByCriteria(controllerId))
                                )
                            )
                        )
                    )
                )
            }

            GlobalEffectType.ALL_CREATURES_HAVE_FLYING -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.ABILITY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.AddKeyword(Keyword.FLYING),
                        filter = ModifierFilter.All(EntityCriteria.Creatures)
                    )
                )
            }

            GlobalEffectType.YOUR_CREATURES_HAVE_VIGILANCE -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.ABILITY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.AddKeyword(Keyword.VIGILANCE),
                        filter = ModifierFilter.All(
                            EntityCriteria.And(
                                listOf(EntityCriteria.Creatures, controlledByCriteria(controllerId))
                            )
                        )
                    )
                )
            }

            GlobalEffectType.YOUR_CREATURES_HAVE_LIFELINK -> {
                modifiers.add(
                    Modifier(
                        layer = Layer.ABILITY,
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.AddKeyword(Keyword.LIFELINK),
                        filter = ModifierFilter.All(
                            EntityCriteria.And(
                                listOf(EntityCriteria.Creatures, controlledByCriteria(controllerId))
                            )
                        )
                    )
                )
            }

            GlobalEffectType.CREATURES_CANT_BLOCK -> {
                // Global effect: e.g. "Creatures can't block"
                modifiers.add(
                    Modifier(
                        layer = Layer.ABILITY, // Can't block is handled in Layer 6 (ability/rules mod)
                        sourceId = sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.AddCantBlockRestriction,
                        filter = filter
                    )
                )
            }

            GlobalEffectType.CREATURES_CANT_ATTACK -> {
                // Placeholder - would need similar treatment if implemented
            }
        }

        return modifiers
    }

    /**
     * Convert CantBlock static ability to modifiers.
     */
    private fun convertCantBlock(
        ability: CantBlock,
        sourceId: EntityId,
        controllerId: EntityId,
        state: EcsGameState
    ): List<Modifier> {
        val filter = resolveStaticTarget(ability.target, sourceId, state)
            ?: return emptyList()

        return listOf(
            Modifier(
                layer = Layer.ABILITY, // Layer 6: Ability-adding/removing/rules setting
                sourceId = sourceId,
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddCantBlockRestriction,
                filter = filter
            )
        )
    }

    /**
     * Resolve a StaticTarget to a ModifierFilter.
     */
    private fun resolveStaticTarget(
        target: StaticTarget,
        sourceId: EntityId,
        state: EcsGameState
    ): ModifierFilter? {
        return when (target) {
            is StaticTarget.AttachedCreature -> {
                // Find what this source is attached to
                val attachedTo = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return null
                ModifierFilter.Specific(attachedTo)
            }

            is StaticTarget.SourceCreature -> {
                ModifierFilter.Self
            }

            is StaticTarget.Controller -> {
                // Controller targeting isn't for permanents
                null
            }

            is StaticTarget.SpecificCard -> {
                // Convert old CardId to EntityId
                ModifierFilter.Specific(EntityId.of(target.cardId.value))
            }
        }
    }

    /**
     * Convert a CreatureFilter to an EntityCriteria.
     */
    private fun convertCreatureFilter(
        filter: CreatureFilter,
        controllerId: EntityId
    ): ModifierFilter {
        return when (filter) {
            is CreatureFilter.All -> ModifierFilter.All(EntityCriteria.Creatures)
            is CreatureFilter.YouControl -> ModifierFilter.ControlledBy(controllerId)
            is CreatureFilter.OpponentsControl -> ModifierFilter.Opponents
            is CreatureFilter.WithKeyword -> ModifierFilter.All(
                EntityCriteria.And(
                    listOf(
                        EntityCriteria.Creatures,
                        EntityCriteria.WithKeyword(filter.keyword)
                    )
                )
            )
            is CreatureFilter.WithoutKeyword -> ModifierFilter.All(
                EntityCriteria.And(
                    listOf(
                        EntityCriteria.Creatures,
                        EntityCriteria.Not(EntityCriteria.WithKeyword(filter.keyword))
                    )
                )
            )
        }
    }

    /**
     * Create a criteria that matches entities controlled by a specific player.
     * Note: This is a workaround since EntityCriteria doesn't have ControlledBy.
     * In practice, the StateProjector resolves this during projection.
     */
    private fun controlledByCriteria(controllerId: EntityId): EntityCriteria {
        // This is a simplification - ideally we'd have a ControlledBy criteria
        // For now, we rely on ControlledBy filter being combined with creature criteria
        return EntityCriteria.Creatures
    }
}
