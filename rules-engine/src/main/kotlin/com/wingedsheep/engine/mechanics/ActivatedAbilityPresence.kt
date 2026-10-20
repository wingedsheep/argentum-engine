package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.ability.CrewRequirement
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.state.components.identity.ownActivatedAbilities
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * How many activated abilities an object currently *has* — backs
 * `StatePredicate.HasActivatedAbilities`. Presence, not activatability: an ability counts whether
 * or not its cost can be paid, its timing allows it, or it functions only from another zone (a
 * creature with cycling has an activated ability on the battlefield too — Tazri, Stalwart Survivor
 * ruling 2023-05-12).
 *
 * Counted: the object's own abilities (printed, copied, class levels' activated abilities, unlocked
 * Room doors, the activated keyword abilities the engine keeps outside `activatedAbilities` —
 * cycling, crew, saddle — and the intrinsic mana ability of each basic land type, CR 305.6) unless
 * it is face down or has lost all abilities; plus every ability granted to it by an effect, a
 * static ability or an emblem, counted exactly as the activated-ability enumerator offers them.
 */
object ActivatedAbilityPresence {
    fun count(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        registry: CardRegistry,
        predicates: PredicateEvaluator,
        excludeManaAbilities: Boolean,
    ): Int {
        val container = state.getEntity(entityId) ?: return 0
        val card = container.get<CardComponent>() ?: return 0
        val onBattlefield = entityId in state.getBattlefield()
        val faceDown = container.has<FaceDownComponent>()
        val removed = onBattlefield && projected.hasLostAllAbilities(entityId)
        fun counted(ability: ActivatedAbility) = !(excludeManaAbilities && ability.isManaAbility)

        var total = 0
        // Intrinsic mana abilities of projected basic land types (CR 305.6) stand in for the printed
        // ones, as in the mana-ability enumerator (a shock land's printed ability *is* its intrinsic
        // one). They reach a face-down permanent too, and survive ability removal only when the same
        // effect set the basic type (Blood Moon), as in [com.wingedsheep.engine.mechanics.mana.ManaAbilityPresence].
        val intrinsic = if (onBattlefield) IntrinsicManaAbilities.forEntity(state, projected, entityId) else emptyList()
        if (!excludeManaAbilities && (!removed || projected.hasBasicLandTypesSetByEffect(entityId))) {
            total += intrinsic.size
        }
        if (!faceDown && !removed) {
            val definition = registry.getCard(card.cardDefinitionId)
            val level = container.get<ClassLevelComponent>()?.currentLevel
            val own = ownActivatedAbilities(card, definition, level)
            total += if (intrinsic.isNotEmpty()) own.count { !it.isManaAbility } else own.count(::counted)
            if (definition != null) {
                // Each "Level N" line of a Class is an activated ability (CR 716.2a).
                total += definition.script.classLevels.count { it.level >= 2 }
                val room = container.get<RoomComponent>()
                if (room != null) {
                    total += definition.cardFaces
                        .filter { RoomFaceId(it.name) in room.unlocked }
                        .sumOf { face -> face.script.activatedAbilities.count(::counted) }
                }
                total += definition.keywordAbilities.count {
                    it is KeywordAbility.Cycling ||
                        (it is KeywordAbility.Numeric && it.keyword == Keyword.SADDLE) ||
                        (!onBattlefield && it is KeywordAbility.Numeric && it.keyword == Keyword.CREW)
                }
            }
        }
        // Crew read off projected keywords covers both printed (gone with lost abilities) and granted.
        if (onBattlefield) total += CrewRequirement.count(state, projected, registry, entityId)

        total += state.grantedActivatedAbilities.count { it.entityId == entityId && counted(it.ability) }
        val nested = predicates.duringManaAbilityQuery(entityId)
        val permissions = CastPermissionUtils(registry, nested, nested.conditions)
        total += permissions.getStaticGrantedAbilitiesWithGranter(entityId, state, projected).count { counted(it.ability) }
        if (onBattlefield) {
            total += permissions.getEmblemGrantedActivatedAbilities(entityId, state, projected).count(::counted)
        }
        return total
    }
}
