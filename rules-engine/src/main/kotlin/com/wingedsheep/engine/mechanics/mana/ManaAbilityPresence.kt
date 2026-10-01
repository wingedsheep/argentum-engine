package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.StaticAbility

/** Presence of an ability is independent of costs, activation locks, and current production. */
object ManaAbilityPresence {
    fun hasAbility(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        registry: CardRegistry,
        predicates: PredicateEvaluator,
    ): Boolean {
        if (entityId !in state.getBattlefield()) return false
        val container = state.getEntity(entityId) ?: return false
        val card = container.get<CardComponent>() ?: return false
        val faceDown = container.has<FaceDownComponent>()
        val removed = projected.hasLostAllAbilities(entityId)
        val intrinsic = projected.hasType(entityId, "LAND") &&
            projected.getSubtypes(entityId).any { it in basicLandTypes }
        if (intrinsic && (!removed || projected.hasBasicLandTypesSetByEffect(entityId))) return true

        if (!faceDown && !removed) {
            val definition = registry.getCard(card.cardDefinitionId)
            if (definition != null) {
                val level = container.get<ClassLevelComponent>()?.currentLevel
                if (definition.script.effectiveActivatedAbilities(level).any {
                        it.isManaAbility && it.activateFromZone == Zone.BATTLEFIELD
                    }) return true
                val room = container.get<RoomComponent>()
                if (room != null && definition.cardFaces.any { face ->
                        RoomFaceId(face.name) in room.unlocked && face.script.activatedAbilities.any {
                            it.isManaAbility && it.activateFromZone == Zone.BATTLEFIELD
                        }
                    }) return true
                // These data types represent triggered mana abilities on their source, rather than
                // abilities granted to the tapped land (e.g. Wild Growth belongs to its Aura).
                if (RoomFaceStatics.activeStaticAbilities(container, definition).any(::isTriggeredManaAbility)) return true
            }
        }
        if (state.grantedActivatedAbilities.any {
                it.entityId == entityId && it.ability.isManaAbility && it.ability.activateFromZone == Zone.BATTLEFIELD
            }) return true
        if (state.grantedStaticAbilities.any {
                it.entityId == entityId && isTriggeredManaAbility(it.ability)
            }) return true

        val nested = predicates.duringManaAbilityQuery(entityId)
        val permissions = CastPermissionUtils(registry, nested, nested.conditions)
        return permissions.getStaticGrantedAbilitiesWithGranter(entityId, state, projected).any {
            it.ability.isManaAbility && it.ability.activateFromZone == Zone.BATTLEFIELD &&
                !projected.hasLostAllAbilities(it.granterId)
        } || permissions.getEmblemGrantedActivatedAbilities(entityId, state, projected).any {
            it.isManaAbility && it.activateFromZone == Zone.BATTLEFIELD
        }
    }

    private fun isTriggeredManaAbility(ability: StaticAbility): Boolean =
        ability is AdditionalManaOnTap || ability is AdditionalManaOnSourceTap

    private val basicLandTypes = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
}
