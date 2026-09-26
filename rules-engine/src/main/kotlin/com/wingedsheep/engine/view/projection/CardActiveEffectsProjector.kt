package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.combat.rules.DefenderBypass
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.view.ClientCardEffect
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Projects the badges shown on a card: restrictions and requirements from floating effects, damage
 * shields, combat requirements, intervening-if progress, type and colour changes, and abilities
 * granted to it that its printed text doesn't show.
 *
 * Each badge family is its own function; [project] concatenates them in a fixed order, which is the
 * order the client lists them in.
 */
internal class CardActiveEffectsProjector(
    private val cardRegistry: CardRegistry,
    private val visibility: Visibility,
    private val conditionBadges: ConditionBadgeProjector,
    private val predicateEvaluator: PredicateEvaluator
) {
    /**
     * Build a list of active effects on a card for display as badges.
     *
     * [projectedState] is null for a face-down permanent seen by someone who may not know what it
     * is: the badges read off the projection (type, colour and creature-type changes) are then * withheld.
     */
    fun project(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState? = null
    ): List<ClientCardEffect> {
        val effects = mutableListOf<ClientCardEffect>()
        effects += textReplacementBadges(state, entityId)
        effects += outgoingDamageDoublerBadges(state, entityId)
        effects += floatingEffectBadges(state, entityId)
        effects += damageShieldBadges(state, entityId)
        effects += combatRequirementBadges(state, entityId)
        // Check for triggered ability condition indicators (intervening-if progress)
        effects += conditionBadges.triggerConditionBadges(state, entityId)
        if (projectedState != null) {
            effects += typeChangeBadges(state, entityId, projectedState)
            effects += colorChangeBadges(state, entityId, projectedState)
            effects += matchingSourceDamagePreventedBadges(state, entityId, projectedState)
        }
        // Granted abilities, printed block restrictions and the defender bypass share one set of
        // shown descriptions, so the same ability reached by two routes shows once.
        val seenDescriptions = HashSet<String>()
        effects += grantedAbilityBadges(state, entityId, seenDescriptions)
        effects += blockRestrictionBadges(state, entityId, seenDescriptions)
        effects += defenderBypassBadges(state, entityId, seenDescriptions)
        return effects
    }

    /**
     * Badge the creature types this permanent has noted — Long List of the Ents' running list, and
     * A Killer Among Us's single secret choice.
     *
     * A *secret* note (`NotedCreatureTypesComponent.secretTo`) is hidden information: only the
     * player who made it gets the badge, so an opponent's view of the permanent looks exactly the
     * same whichever type was chosen. A spectator sees neither, since they'd otherwise leak the
     * answer to anyone watching. Paying the reveal cost clears `secretTo`, and the badge becomes
     * public in the same update.
     *
     * Without this the chooser has no way to remember what they picked — the whole point of the
     * note is that the *engine* keeps track of the piece of paper (CR 702.106b) for them.
     */
    fun notedCreatureTypeBadges(
        container: ComponentContainer,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): List<ClientCardEffect> {
        val noted = container.get<NotedCreatureTypesComponent>() ?: return emptyList()
        if (noted.types.isEmpty()) return emptyList()
        val secret = noted.secretTo != null
        if (secret && (isSpectator || !noted.isVisibleTo(viewingPlayerId))) return emptyList()
        return listOf(
            ClientCardEffect(
                effectId = "noted_creature_types_${noted.types.sorted().joinToString("_")}",
                name = if (secret) "Chosen (secret)" else "Noted",
                description = noted.types.sorted().joinToString(", ") +
                    if (secret) " — only you can see this" else "",
                icon = "creature-type"
            )
        )
    }

    /** Check for text replacements. */
    private fun textReplacementBadges(state: GameState, entityId: EntityId): List<ClientCardEffect> =
        TextChanges.of(state, entityId)?.replacements?.map { r ->
            ClientCardEffect(
                effectId = "text_modified_${r.fromWord}_${r.toWord}",
                name = "Text Modified",
                description = "${r.fromWord} → ${r.toWord}",
                icon = "text-change"
            )
        } ?: emptyList()

    /**
     * Damage this creature deals is doubled by an Equipment/Aura attached to it (Mjölnir, Hammer of
     * Thor). This is the source-side mirror of the player badges built in
     * [PlayerActiveEffectsProjector]: the doubling is a property of *this* creature's outgoing
     * damage, so it belongs here rather than warning every player that damage dealt to them is
     * doubled. Attachment is checked by the engine, so the badge is absent while the Equipment is
     * unequipped.
     */
    private fun outgoingDamageDoublerBadges(state: GameState, entityId: EntityId): List<ClientCardEffect> =
        DamageUtils.damageDoublersAffectingSource(state, entityId, predicateEvaluator = predicateEvaluator).map { doubler ->
            val scope = when (doubler.damageType) {
                is DamageType.Combat -> "Combat damage"
                is DamageType.NonCombat -> "Noncombat damage"
                is DamageType.Any -> "Damage"
            }
            ClientCardEffect(
                effectId = "damage_doubled_source_${doubler.sourceId.value}",
                name = "Damage Doubled",
                description = "$scope this creature deals is doubled by ${doubler.sourceName}",
                icon = "double-damage"
            )
        }

    /** One badge per floating effect on this card that restricts, requires or redirects something. */
    private fun floatingEffectBadges(state: GameState, entityId: EntityId): List<ClientCardEffect> =
        state.floatingEffects
            .filter { entityId in it.effect.affectedEntities }
            .mapNotNull { modificationBadge(state, it.effect.modification) }

    private fun modificationBadge(
        state: GameState,
        modification: SerializableModification
    ): ClientCardEffect? = when (modification) {
        is SerializableModification.CantBeBlockedExceptByColor -> {
            val colorName = modification.color.lowercase().replaceFirstChar { it.uppercase() }
            ClientCardEffect(
                effectId = "cant_be_blocked_except_by_${modification.color.lowercase()}",
                name = "Evasion",
                description = "Can't be blocked except by $colorName creatures",
                icon = "evasion"
            )
        }
        // The filter-based sibling of the color case above (Speed, Young Avenger's "can't be
        // blocked this turn except by creatures with haste", Resilient Roadrunner). It routes
        // through the same projected evasion channel, so the block rules already enforced it
        // — only the badge was missing, leaving the restriction invisible to both players.
        is SerializableModification.CantBeBlockedExceptBy -> ClientCardEffect(
            effectId = "cant_be_blocked_except_by",
            name = "Evasion",
            description = "Can't be blocked except by ${modification.blockerFilter.description}",
            icon = "evasion"
        )
        is SerializableModification.MustBeBlockedByAll -> ClientCardEffect(
            effectId = "must_be_blocked_by_all",
            name = "Lure",
            description = "Must be blocked by all creatures able to block it",
            icon = "lure"
        )
        is SerializableModification.MustBeBlockedIfAble -> ClientCardEffect(
            effectId = "must_be_blocked",
            name = "Must Be Blocked",
            description = "Must be blocked if able",
            icon = "lure"
        )
        is SerializableModification.SetCantBlock -> ClientCardEffect(
            effectId = "cant_block",
            name = "Can't Block",
            description = "This creature can't block this turn",
            icon = "cant-block"
        )
        // PreventNextDamage, RegenerationShield and RemoveDamageShield are totalled across all
        // floating effects into one badge each — see damageShieldBadges.
        is SerializableModification.PreventAllDamageDealtBy -> ClientCardEffect(
            effectId = "prevent_all_damage_dealt_by",
            name = "Silenced",
            description = "All damage this creature would deal is prevented this turn",
            icon = "prevent-damage"
        )
        is SerializableModification.SetCantAttack -> ClientCardEffect(
            effectId = "cant_attack",
            name = "Can't Attack",
            description = "This creature can't attack",
            icon = "cant-attack"
        )
        is SerializableModification.CantBeRegenerated -> ClientCardEffect(
            effectId = "cant_be_regenerated",
            name = "No Regen",
            description = "This creature can't be regenerated",
            icon = "cant-attack"
        )
        is SerializableModification.ExileOnDeath -> ClientCardEffect(
            effectId = "exile_on_death",
            name = "Exile on Death",
            description = "If this creature would die, exile it instead",
            icon = "exile-on-death"
        )
        is SerializableModification.ExileControllerGraveyardOnDeath -> ClientCardEffect(
            effectId = "exile_gy_on_death",
            name = "Exile GY on Death",
            description = "When this creature dies, its controller's graveyard is exiled",
            icon = "exile-on-death"
        )
        is SerializableModification.MustBlockSpecificAttacker -> {
            // Name the attacker. The pinned creature is not always the obvious one — an
            // ANY-bound "blocks that Wolf" trigger (Tolsimir, Midnight's Light) pins the
            // blocker to a creature other than the ability's source, and a defender who
            // guesses wrong only finds out when their declaration is rejected.
            val attackerName = state.getEntity(modification.attackerId)
                ?.get<CardComponent>()?.name
            ClientCardEffect(
                effectId = "must_block_${modification.attackerId}",
                name = "Must Block",
                description = if (attackerName != null) {
                    "This creature must block $attackerName this combat if able"
                } else {
                    "This creature must block a specific attacker if able"
                },
                icon = "must-attack"
            )
        }
        // The restriction mirror. Same reason to badge it: the defender otherwise discovers
        // the pairwise ban only when the block declaration bounces back.
        is SerializableModification.CantBlockSpecificAttacker -> {
            val attackerName = state.getEntity(modification.attackerId)
                ?.get<CardComponent>()?.name
            ClientCardEffect(
                effectId = "cant_block_${modification.attackerId}",
                name = "Can't Block",
                description = if (attackerName != null) {
                    "This creature can't block $attackerName this turn"
                } else {
                    "This creature can't block a specific attacker this turn"
                },
                icon = "cant-block"
            )
        }
        // The unrestricted requirement (Culvert Ambusher). Badged for the same reason as
        // "Must Attack": the defending player finds out about it when their declaration is
        // rejected, which is far too late to be the first they hear of it.
        is SerializableModification.SetMustBlock -> ClientCardEffect(
            effectId = "must_block_this_turn",
            name = "Must Block",
            description = "This creature must block this turn if able",
            icon = "must-attack"
        )
        // PreventAllCombatDamage and PreventCombatDamageFromGroup are not card-scoped — they hold
        // no affected entity at all — so they are badged on the player in
        // PlayerActiveEffectsProjector instead.
        is SerializableModification.PreventCombatDamageToAndBy -> ClientCardEffect(
            effectId = "prevent_combat_damage_to_and_by",
            name = "No Combat Dmg",
            description = "All combat damage dealt to and dealt by this creature is prevented",
            icon = "prevent-damage"
        )
        is SerializableModification.RedirectNextDamage -> {
            val amountText = modification.amount?.let { "$it" } ?: "all"
            ClientCardEffect(
                effectId = "redirect_next_damage",
                name = "Redirect $amountText",
                description = "The next $amountText damage that would be dealt to this is redirected",
                icon = "redirect"
            )
        }
        is SerializableModification.PreventNextDamageFromSourceShield -> ClientCardEffect(
            effectId = "deflect_damage_${modification.damageSourceId}",
            name = "Deflect",
            description = "The next damage from the chosen source is prevented; a triggered ability then resolves",
            icon = "redirect"
        )
        is SerializableModification.ReflectCombatDamage -> ClientCardEffect(
            effectId = "reflect_combat_damage",
            name = "Reflect",
            description = "Combat damage dealt to you is also dealt to the attacking player",
            icon = "redirect"
        )
        is SerializableModification.RedirectCombatDamageToController -> ClientCardEffect(
            effectId = "redirect_combat_damage_to_controller",
            name = "Redirected",
            description = "Combat damage this creature would deal is dealt to its controller instead",
            icon = "redirect"
        )
        is SerializableModification.RemoveAllAbilities -> ClientCardEffect(
            effectId = "lost_all_abilities",
            name = "No Abilities",
            description = "This permanent has lost all abilities",
            icon = "lost-abilities"
        )
        // SetCreatureSubtypes is surfaced once in typeChangeBadges, using the projected state,
        // so superseded floating effects (e.g., an earlier Scout transform that
        // a later Soldier transform overrode) don't produce duplicate badges.
        // ChangeColor is surfaced once in colorChangeBadges from projected state (same reasoning).
        // Other modifications don't need badges (stats/keywords/types are shown elsewhere)
        else -> null
    }

    /**
     * Damage-prevention, regeneration and remove-damage shields on this card, each totalled across
     * every floating effect into one badge.
     */
    private fun damageShieldBadges(state: GameState, entityId: EntityId): List<ClientCardEffect> {
        var preventDamageTotal = 0
        var regenerationShieldCount = 0
        var removeDamageShieldCount = 0
        for (floatingEffect in state.floatingEffects) {
            if (entityId !in floatingEffect.effect.affectedEntities) continue
            when (val modification = floatingEffect.effect.modification) {
                is SerializableModification.PreventNextDamage -> preventDamageTotal += modification.remainingAmount
                is SerializableModification.RegenerationShield -> regenerationShieldCount++
                is SerializableModification.RemoveDamageShield -> removeDamageShieldCount++
                else -> {}
            }
        }

        val effects = mutableListOf<ClientCardEffect>()
        if (preventDamageTotal > 0) {
            effects.add(
                ClientCardEffect(
                    effectId = "prevent_damage",
                    name = "Prevent $preventDamageTotal",
                    description = "Prevents the next $preventDamageTotal damage that would be dealt to this creature",
                    icon = "prevent-damage"
                )
            )
        }

        if (regenerationShieldCount > 0) {
            val name = if (regenerationShieldCount > 1) "Regen x$regenerationShieldCount" else "Regen"
            effects.add(
                ClientCardEffect(
                    effectId = "regeneration",
                    name = name,
                    description = if (regenerationShieldCount > 1)
                        "Has $regenerationShieldCount regeneration shields (prevents destruction, taps, removes damage and from combat)"
                    else
                        "Has a regeneration shield (prevents destruction, taps, removes damage and from combat)",
                    icon = "regeneration"
                )
            )
        }

        if (removeDamageShieldCount > 0) {
            val name = if (removeDamageShieldCount > 1) "Shielded x$removeDamageShieldCount" else "Shielded"
            effects.add(
                ClientCardEffect(
                    effectId = "remove_damage_shield",
                    name = name,
                    description = "The next time this permanent would be destroyed this turn, " +
                        "remove all damage marked on it instead",
                    icon = "regeneration"
                )
            )
        }
        return effects
    }

    /** Standing attack requirements that live on the creature as components: must attack, goad. */
    private fun combatRequirementBadges(state: GameState, entityId: EntityId): List<ClientCardEffect> {
        val effects = mutableListOf<ClientCardEffect>()

        // Check for MustAttackThisTurnComponent (e.g., Walking Desecration effect)
        val mustAttack = state.getEntity(entityId)?.has<MustAttackThisTurnComponent>() == true
        if (mustAttack) {
            effects.add(
                ClientCardEffect(
                    effectId = "must_attack_this_turn",
                    name = "Must Attack",
                    description = "This creature must attack this turn if able",
                    icon = "must-attack"
                )
            )
        }

        // Check for GoadedComponent (CR 701.15). Surfaces the standing combat
        // requirement on the creature so opposing combat decisions are obvious
        // without the player having to recall who goaded it.
        val goaded = state.getEntity(entityId)?.get<GoadedComponent>()
        if (goaded != null) {
            val goaderNames = goaded.goaderIds
                .mapNotNull { state.getEntity(it)?.get<PlayerComponent>()?.name }
                .ifEmpty { listOf("an opponent") }
            val goaderList = when (goaderNames.size) {
                1 -> goaderNames.single()
                2 -> "${goaderNames[0]} and ${goaderNames[1]}"
                else -> goaderNames.dropLast(1).joinToString(", ") + ", and " + goaderNames.last()
            }
            effects.add(
                ClientCardEffect(
                    effectId = "goaded",
                    name = "Goaded",
                    description = "This creature attacks each combat if able and attacks " +
                        "a player other than $goaderList if able",
                    icon = "must-attack"
                )
            )
        }
        return effects
    }

    /**
     * "type-change" badges: creature subtypes set or added, card types replaced, basic land types
     * replaced. Each is computed from projected state (rather than per floating effect) so
     * superseded transformations don't produce duplicate badges (e.g., Figure of Fable Scout →
     * Soldier).
     */
    private fun typeChangeBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState
    ): List<ClientCardEffect> {
        val baseCardComponent = state.getEntity(entityId)?.get<CardComponent>()
        val baseSubtypes = baseCardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        return creatureSubtypeChangeBadges(state, entityId, projectedState, baseCardComponent, baseSubtypes) +
            cardTypeChangeBadges(state, entityId, projectedState, baseCardComponent) +
            landTypeChangeBadges(state, entityId, projectedState, baseSubtypes)
    }

    /**
     * Surface a single "type-change" badge when projected creature subtypes diverge from what the
     * printed card art shows.
     */
    private fun creatureSubtypeChangeBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState,
        baseCardComponent: CardComponent?,
        baseSubtypes: Set<String>
    ): List<ClientCardEffect> {
        val projectedSubtypes = projectedState.getSubtypes(entityId)
        val hasSetCreatureSubtypes = state.floatingEffects.any {
            entityId in it.effect.affectedEntities &&
                it.effect.modification is SerializableModification.SetCreatureSubtypes
        }
        // "Is all creature types" (Undercover Skrull's graveyard-gated static, Stalactite
        // Dagger) projects every creature type. The type line deliberately collapses back to the
        // printed subtypes rather than rendering ~150 of them, and a *granted* all-types has no
        // CHANGELING keyword to badge — so without this the state is invisible. Checked before
        // the diff branches below, which would otherwise try to list every type.
        val isEveryCreatureType = hasEveryCreatureType(projectedSubtypes)
        val hasChangelingKeyword = baseCardComponent?.baseKeywords?.contains(Keyword.CHANGELING) == true
        if (isEveryCreatureType) {
            // A *native* changeling already reads off its printed keyword badge; only a
            // granted all-types needs one of its own.
            if (hasChangelingKeyword) return emptyList()
            return listOf(
                ClientCardEffect(
                    effectId = "all_creature_types",
                    name = "All types",
                    description = "Is every creature type",
                    icon = "type-change"
                )
            )
        }
        if (hasSetCreatureSubtypes) {
            if (projectedSubtypes.isEmpty() || projectedSubtypes == baseSubtypes) return emptyList()
            val joined = projectedSubtypes.joinToString(" ")
            return listOf(
                ClientCardEffect(
                    effectId = "type_changed",
                    name = joined,
                    description = "Creature types are now $joined",
                    icon = "type-change"
                )
            )
        }
        // AddSubtype floating effects (e.g. Curious Colossus "becomes a Coward
        // in addition to its other types") don't replace the printed subtypes,
        // so the projected vs base diff is the right signal — but we read the
        // values straight from the floating effects so CHANGELING (which
        // projects every creature type) doesn't flood the badge.
        val addedSubtypes = state.floatingEffects
            .filter { entityId in it.effect.affectedEntities }
            .mapNotNull { (it.effect.modification as? SerializableModification.AddSubtype)?.subtype }
            .filter { it !in baseSubtypes }
            .distinct()
        if (addedSubtypes.isEmpty()) return emptyList()
        val joined = addedSubtypes.joinToString(" ")
        return listOf(
            ClientCardEffect(
                effectId = "type_added",
                name = "+$joined",
                description = "Also a $joined in addition to its other types",
                icon = "type-change"
            )
        )
    }

    /**
     * Surface a "type-change" badge when a permanent's CARD TYPES are replaced by a type-changing
     * effect (Kitesail Larcenist "becomes a Treasure artifact", Song of the Dryads "becomes a Forest
     * land", Polymorphist's Jest "becomes a Frog"). This is the full-replacement modification
     * (SetCardTypes); additive "becomes a creature in addition to its other types" animate effects
     * use AddType and don't trip this. The printed art still reads as the original object, so the
     * badge makes the new type visible and pairs with the P/T box vanishing (CR 208.3). Driven from
     * projected state so superseded transformations don't stack, and the projected subtypes ride
     * along ("Artifact — Treasure").
     */
    private fun cardTypeChangeBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState,
        baseCardComponent: CardComponent?
    ): List<ClientCardEffect> {
        val hasSetCardTypes = state.floatingEffects.any {
            entityId in it.effect.affectedEntities &&
                it.effect.modification is SerializableModification.SetCardTypes
        }
        if (!hasSetCardTypes) return emptyList()
        val baseTypes = baseCardComponent?.typeLine?.cardTypes?.map { it.name }?.toSet() ?: emptySet()
        val projectedTypes = projectedState.getTypes(entityId)
        if (projectedTypes.isEmpty() || projectedTypes == baseTypes) return emptyList()
        val typeWords = CardType.entries
            .filter { it.name in projectedTypes }
            .map { it.displayName }
        val subtypeWords = projectedState.getSubtypes(entityId).toList()
        val newTypeLine = buildString {
            append(typeWords.joinToString(" "))
            if (subtypeWords.isNotEmpty()) append(" — ").append(subtypeWords.joinToString(" "))
        }
        return listOf(
            ClientCardEffect(
                effectId = "card_type_changed",
                name = newTypeLine,
                description = "Card type is now $newTypeLine",
                icon = "type-change"
            )
        )
    }

    /**
     * Surface a "type-change" badge when a land's basic land types are replaced (e.g., Slimy Kavu /
     * Dream Thrush "target land becomes a Swamp"). The type line text already reflects this, but a
     * battlefield permanent is read by its art, so the badge makes the change visible. Driven from
     * projected state so superseded transformations (re-targeting the same land) don't stack.
     * Dream Thrush's chosen-type variant resolves to a concrete SetBasicLandTypes at execution time,
     * so the floating effect is always SetBasicLandTypes here.
     */
    private fun landTypeChangeBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState,
        baseSubtypes: Set<String>
    ): List<ClientCardEffect> {
        val hasSetBasicLandTypes = state.floatingEffects.any {
            entityId in it.effect.affectedEntities &&
                it.effect.modification is SerializableModification.SetBasicLandTypes
        }
        if (!hasSetBasicLandTypes) return emptyList()
        val basicLandTypes = Subtype.ALL_BASIC_LAND_TYPES
        val baseLandTypes = baseSubtypes.filter { it in basicLandTypes }.toSet()
        val projectedLandTypes = projectedState.getSubtypes(entityId)
            .filter { it in basicLandTypes }
        if (projectedLandTypes.isEmpty() || projectedLandTypes.toSet() == baseLandTypes) return emptyList()
        val joined = projectedLandTypes.joinToString(" ")
        return listOf(
            ClientCardEffect(
                effectId = "land_type_changed",
                name = joined,
                description = "Land types are now $joined",
                icon = "type-change"
            )
        )
    }

    /**
     * Surface a single "color-change" badge when a ChangeColor floating effect is replacing this
     * entity's colors. Driven from projected state so superseded transformations don't stack (Tam
     * re-targeting same creature).
     */
    private fun colorChangeBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState
    ): List<ClientCardEffect> {
        val hasChangeColor = state.floatingEffects.any {
            entityId in it.effect.affectedEntities &&
                it.effect.modification is SerializableModification.ChangeColor
        }
        if (!hasChangeColor) return emptyList()
        val baseCardComponent = state.getEntity(entityId)?.get<CardComponent>()
        val baseColors = baseCardComponent?.colors?.map { it.name }?.toSet() ?: emptySet()
        val projectedColors = projectedState.getColors(entityId)
        if (projectedColors == baseColors) return emptyList()
        val name = when {
            projectedColors.isEmpty() -> "Colorless"
            projectedColors.size == 5 -> "All Colors"
            else -> projectedColors.joinToString(" ") {
                it.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        }
        return listOf(
            ClientCardEffect(
                effectId = "color_changed",
                name = name,
                description = "Colors are now $name",
                icon = "color-change"
            )
        )
    }

    /**
     * Badge a permanent whose next damage a [SerializableModification.PreventNextDamageFromMatching]
     * shield would prevent (Circle of Solace's chosen creature type), so the attacking player can
     * see it before damage.
     */
    private fun matchingSourceDamagePreventedBadges(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState
    ): List<ClientCardEffect> {
        val floatingEffect = state.floatingEffects.firstOrNull { floatingEffect ->
            val modification = floatingEffect.effect.modification
            modification is SerializableModification.PreventNextDamageFromMatching &&
                predicateEvaluator.matches(
                    state, projectedState, entityId, modification.filter,
                    PredicateContext(controllerId = floatingEffect.controllerId)
                )
        } ?: return emptyList()
        val protectedName = floatingEffect.effect.affectedEntities.firstNotNullOfOrNull { protectedId ->
            state.getEntity(protectedId)?.let { it.get<PlayerComponent>()?.name ?: it.get<CardComponent>()?.name }
        } ?: "its recipient"
        return listOf(
            ClientCardEffect(
                effectId = "damage_prevented_next_${floatingEffect.id.value}",
                name = "Damage Prevented",
                description = "The next time this would deal damage to $protectedName this turn, that damage is prevented",
                icon = "prevent-damage"
            )
        )
    }

    /**
     * Surface temporarily-granted abilities (triggered / activated / cast-keyword / static). E.g.
     * Sygg, Wanderwine Wisdom grants a draw-on-combat-damage trigger until end of turn; Songcrafter
     * Mage grants harmonize to a graveyard spell. The grant is real game state but the printed
     * oracle text on the recipient doesn't reflect it, so without a badge a player can't see it.
     *
     * The *same* ability can be granted to one permanent more than once — a land earthbended
     * twice holds two separate grant entries of the identical "return it tapped" trigger. To the
     * player they're one ability, so dedupe by the shown description and emit a single badge
     * instead of stacking duplicate "Granted Ability" tiles.
     */
    private fun grantedAbilityBadges(
        state: GameState,
        entityId: EntityId,
        seenDescriptions: MutableSet<String>
    ): List<ClientCardEffect> {
        val effects = mutableListOf<ClientCardEffect>()
        fun grant(effectId: String, description: String) {
            if (!seenDescriptions.add(description)) return
            effects.add(
                ClientCardEffect(
                    effectId = effectId,
                    name = "Granted Ability",
                    description = description,
                    icon = "granted-ability"
                )
            )
        }
        for (granted in state.grantedTriggeredAbilities) {
            if (granted.entityId != entityId) continue
            grant("granted_trig_${granted.ability.id.value}", granted.ability.description)
        }
        for (granted in state.grantedStateTriggeredAbilities) {
            if (granted.entityId != entityId) continue
            grant("granted_state_trig_${granted.ability.id.value}", granted.ability.description)
        }
        for (granted in state.grantedActivatedAbilities) {
            if (granted.entityId != entityId) continue
            grant("granted_act_${granted.ability.id.value}", granted.ability.description)
        }
        for (granted in state.grantedKeywordAbilities) {
            if (granted.entityId != entityId) continue
            grant("granted_kw_${granted.ability.keyword?.name ?: "keyword"}", granted.ability.description)
        }
        // Granted *static* abilities (e.g. Cavern Stomper's "{3}{G}: can't be blocked by creatures
        // with power 2 or less"). These live in their own GameState list — combat reads them directly
        // rather than through the layer system — so they never reach the projected keyword set that
        // feeds `abilityFlags`. Without this badge the grant is invisible after it resolves.
        for (granted in state.grantedStaticAbilities) {
            if (granted.entityId != entityId) continue
            grant("granted_static_${granted.ability.description.hashCode()}", granted.ability.description)
        }
        return effects
    }

    /**
     * Printed "can't be blocked by more than N" restrictions (incl. the conditional descend form,
     * e.g. Akawalli's descend-8) are read directly by BlockPhaseManager, never through the layer
     * system, so they never reach the projected keyword set / abilityFlags and get no keyword chip.
     * Surface an active one as a badge so the restriction is visible in play.
     */
    private fun blockRestrictionBadges(
        state: GameState,
        entityId: EntityId,
        seenDescriptions: MutableSet<String>
    ): List<ClientCardEffect> {
        val cardDefForRestrictions = state.getEntity(entityId)?.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            ?: return emptyList()
        val restrictionController = state.projectedState.getController(entityId) ?: return emptyList()
        val effects = mutableListOf<ClientCardEffect>()
        for (ability in cardDefForRestrictions.script.staticAbilities) {
            val active = visibility.activeStaticAbility(state, ability, entityId, restrictionController)
            if (active is CantBeBlockedByMoreThan && active.filter.scope is Scope.Self) {
                val description = active.description.replaceFirstChar { it.uppercase() }
                if (seenDescriptions.add(description)) {
                    effects.add(
                        ClientCardEffect(
                            effectId = "block_restriction_${description.hashCode()}",
                            name = "Evasion",
                            description = description,
                            icon = "evasion"
                        )
                    )
                }
            }
        }
        return effects
    }

    /**
     * Printed "can attack despite defender" (Shipwreck Sentry, Mechan Shieldmate, …) and the
     * temporary Krotiq-style grant are read directly by AttackRestrictionRules, never through the
     * layer system, so they never reach the projected keyword set / abilityFlags. Surface a badge
     * while the creature actually has Defender AND the restriction is currently lifted, so the
     * player can see a Defender that can presently attack (e.g. after an artifact entered this
     * turn). Shares DefenderBypass with the attack-legality rule so the badge shows exactly when
     * the attack would be allowed.
     */
    private fun defenderBypassBadges(
        state: GameState,
        entityId: EntityId,
        seenDescriptions: MutableSet<String>
    ): List<ClientCardEffect> {
        val restrictionController = state.projectedState.getController(entityId) ?: return emptyList()
        if (!state.projectedState.hasKeyword(entityId, Keyword.DEFENDER)) return emptyList()
        if (!DefenderBypass.isActive(state, entityId, restrictionController, cardRegistry, predicateEvaluator = predicateEvaluator)) return emptyList()
        val description = "Can attack despite defender"
        if (!seenDescriptions.add(description)) return emptyList()
        return listOf(
            ClientCardEffect(
                effectId = "can_attack_despite_defender",
                name = "Can Attack",
                description = description,
                icon = "can-attack"
            )
        )
    }
}

/**
 * Whether [subtypes] covers every creature type — printed changeling, granted changeling, or an
 * "is all creature types" static (Undercover Skrull, Stalactite Dagger). Two places need the
 * same answer and must not drift: the type line collapses back to the printed subtypes rather
 * than rendering ~150 entries, and the badge builder substitutes a single "All types" badge.
 */
internal fun hasEveryCreatureType(subtypes: Collection<String>): Boolean {
    if (subtypes.isEmpty()) return false
    // Hash once: one caller hands us a List, and a linear `in` per creature type would make
    // this ~150 scans of it on every card of every state transform.
    val lookup = if (subtypes is Set<String>) subtypes else subtypes.toSet()
    return Subtype.ALL_CREATURE_TYPES.all { it in lookup }
}
