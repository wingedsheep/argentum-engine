package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantCantBeBlockedToSmallCreaturesComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HexproofFromColorComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Projects the game state by applying continuous effects in layer order (Rule 613).
 *
 * The StateProjector transforms "base state" (stored components) into
 * "projected state" (what the game sees after all effects are applied).
 *
 * Layer Order:
 * 1. Copy effects
 * 2. Control-changing effects
 * 3. Text-changing effects
 * 4. Type-changing effects
 * 5. Color-changing effects
 * 6. Ability-adding/removing effects
 * 7. Power/toughness modifications
 *    a. Characteristic-defining abilities
 *    b. Setting P/T to specific values
 *    c. Modifications from +N/+N effects
 *    d. Counters (+1/+1, -1/-1)
 *    e. Effects that switch P/T
 */
/**
 * Mapping from keyword counter types to the keyword name they grant (Rule 122.1b).
 */
private val KEYWORD_COUNTER_MAP = mapOf(
    CounterType.FLYING to Keyword.FLYING.name,
    CounterType.FIRST_STRIKE to Keyword.FIRST_STRIKE.name,
    CounterType.LIFELINK to Keyword.LIFELINK.name,
    CounterType.INDESTRUCTIBLE to Keyword.INDESTRUCTIBLE.name,
    CounterType.DEATHTOUCH to Keyword.DEATHTOUCH.name,
    CounterType.TRAMPLE to Keyword.TRAMPLE.name,
    CounterType.HEXPROOF to Keyword.HEXPROOF.name,
    CounterType.REACH to Keyword.REACH.name
)

class StateProjector(
    // Inject an empty projection as the supplier — reaching for [GameState.projectedState]
    // here would re-enter our own lazy initializer. Mid-layer callers thread their
    // intermediate snapshot through the `projectedState` parameter explicitly; the empty
    // supplier is the safety net for paths that don't carry one (predicate matching falls
    // back to base CardComponent for missing entries).
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(
        defaultProjection = { ProjectedState(it, emptyMap()) }
    )
) {
    private val filterResolver = AffectsFilterResolver()
    private val effectApplicator = EffectApplicator(dynamicAmountEvaluator)
    private val effectSorter = EffectSorter()

    /**
     * Project the full game state with all continuous effects applied.
     */
    fun project(state: GameState): ProjectedState {
        val projectedValues = mutableMapOf<EntityId, MutableProjectedValues>()
        val dynamicStatEntities = mutableListOf<Pair<EntityId, CardComponent>>()

        // Initialize all permanents with their base values
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (container.has<FaceDownComponent>()) {
                projectedValues[entityId] = MutableProjectedValues(
                    power = 2,
                    toughness = 2,
                    keywords = mutableSetOf(),
                    colors = mutableSetOf(),
                    types = mutableSetOf("CREATURE"),
                    subtypes = mutableSetOf(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = true
                )
            } else {
                val baseStats = cardComponent.baseStats
                projectedValues[entityId] = MutableProjectedValues(
                    power = baseStats?.basePower,
                    toughness = baseStats?.baseToughness,
                    keywords = (cardComponent.baseKeywords.map { it.name } +
                        cardComponent.baseFlags.map { it.name } +
                        (container.get<ProtectionComponent>()?.colors?.map { "PROTECTION_FROM_${it.name}" } ?: emptyList()) +
                        (container.get<ProtectionComponent>()?.subtypes?.map { "PROTECTION_FROM_SUBTYPE_${it.uppercase()}" } ?: emptyList()) +
            (container.get<ProtectionComponent>()?.supertypes?.map { "PROTECTION_FROM_SUPERTYPE_${it.uppercase()}" } ?: emptyList()) +
                        (container.get<HexproofFromColorComponent>()?.colors?.map { "HEXPROOF_FROM_${it.name}" } ?: emptyList()) +
                        (container.get<ToxicComponent>()?.let { listOf("TOXIC_${it.amount}") } ?: emptyList())).toMutableSet(),
                    colors = cardComponent.colors.map { it.name }.toMutableSet(),
                    types = extractTypes(cardComponent),
                    subtypes = cardComponent.typeLine.subtypes.map { it.value }.toMutableSet(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = false
                )

                // Rule 122.1b: Keyword counters grant their keyword
                val countersComponent = container.get<CountersComponent>()
                if (countersComponent != null) {
                    KEYWORD_COUNTER_MAP.forEach { (counterType, keywordName) ->
                        if (countersComponent.getCount(counterType) > 0) {
                            projectedValues[entityId]?.keywords?.add(keywordName)
                        }
                    }
                    // CR 702.147a: a decayed counter grants the Decayed keyword ability — the
                    // "can't block" static half is realized here (the attack-triggered "sacrifice
                    // at end of combat" half is detected by TriggerDetector off the counter).
                    if (countersComponent.getCount(CounterType.DECAYED) > 0) {
                        projectedValues[entityId]?.let {
                            it.keywords.add(Keyword.DECAYED.name)
                            it.cantBlock = true
                        }
                    }
                }

                if (Keyword.CHANGELING in cardComponent.baseKeywords) {
                    projectedValues[entityId]?.subtypes?.addAll(Subtype.ALL_CREATURE_TYPES)
                }

                if (baseStats?.isDynamic == true) {
                    dynamicStatEntities.add(entityId to cardComponent)
                }
            }
        }

        // Apply Layer 3 text-changing effects (TextReplacementComponent)
        applyTextReplacements(state, projectedValues)

        // Collect all active continuous effects
        val effects = collectContinuousEffects(state, projectedValues)

        // Sort effects by layer and dependency
        val sortedEffects = effectSorter.sortByLayerAndDependency(effects, state)

        // === Layer 2 (Control) ===
        val controlEffects = sortedEffects.filter { it.layer == Layer.CONTROL }
        for (effect in controlEffects) {
            effectApplicator.applyEffect(effect, state, projectedValues)
        }

        // Re-resolve controller-dependent filters for layers 3-6 now that control is established
        val nonControlNonPTEffects = sortedEffects.filter { it.layer != Layer.CONTROL && it.layer != Layer.POWER_TOUGHNESS }
            .map { effect -> applyControllerGate(effect, projectedValues) }
            .map { effect ->
                if (effect.affectsFilter != null && filterResolver.isControllerDependentFilter(effect.affectsFilter)) {
                    effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
                } else {
                    effect
                }
            }

        // === Layers 3-4 (Text + Type) ===
        // Apply type-changing effects first so creature-dependent filters in layers 5-6
        // see permanents that became creatures (e.g., Opalescence making enchantments creatures).
        val typeLayerEffects = nonControlNonPTEffects.filter { it.layer == Layer.TEXT || it.layer == Layer.TYPE }
        for (effect in typeLayerEffects) {
            effectApplicator.applyEffect(effect, state, projectedValues)
        }

        // CR 701.54c: a player's Ring-bearer is legendary (the Ring emblem's first ability).
        // Applied here as a type-changing (Layer 4) effect, after Layer 2 control is established, so
        // it reads the *projected* controller: "is your Ring-bearer" requires the creature to still
        // be under its designator's control (CR 701.54e), so a Ring-bearer stolen by a control-
        // changing effect stops being legendary. isLegendary() reads the projected type set, so
        // adding "LEGENDARY" suffices, and it follows whichever creature currently holds the
        // designation.
        for (entityId in state.getBattlefield()) {
            val bearer = state.getEntity(entityId)?.get<RingBearerComponent>() ?: continue
            if (projectedValues[entityId]?.controllerId == bearer.ownerId) {
                projectedValues[entityId]?.types?.add("LEGENDARY")
            }
        }

        // Re-resolve creature-dependent filters for layers 5-6 now that type changes are applied
        val postTypeEffects = nonControlNonPTEffects.filter { it.layer != Layer.TEXT && it.layer != Layer.TYPE }
            .map { effect ->
                if (effect.affectsFilter != null && filterResolver.isCreatureDependentFilter(effect.affectsFilter)) {
                    effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
                } else {
                    effect
                }
            }

        // === Layers 5-6 (Color + Ability) ===
        for (effect in postTypeEffects) {
            effectApplicator.applyEffect(effect, state, projectedValues)
        }

        // Rule 122.1b: re-apply keyword counters after Layer 6.
        // Counters that grant a keyword are themselves abilities of the object. When a "loses
        // all abilities" effect (Layer 6) was applied above, those counter-granted keywords
        // were wiped. Counter placement is generally later than (or part of the same effect as)
        // the lose-all-abilities effect (e.g., Abigale, Eloquent First-Year), so the counter
        // grant must win. Re-applying here is a pragmatic stand-in for proper Layer 6
        // timestamp ordering of counter-granted keywords.
        for ((entityId, values) in projectedValues) {
            val countersComponent = state.getEntity(entityId)?.get<CountersComponent>() ?: continue
            KEYWORD_COUNTER_MAP.forEach { (counterType, keywordName) ->
                if (countersComponent.getCount(counterType) > 0) {
                    values.keywords.add(keywordName)
                }
            }
            // Re-apply the decayed counter's Decayed grant after Layer 6 for the same reason
            // (a "loses all abilities" effect must not wipe a counter-granted ability).
            if (countersComponent.getCount(CounterType.DECAYED) > 0) {
                values.keywords.add(Keyword.DECAYED.name)
                values.cantBlock = true
            }
        }

        // Resolve CDAs (Layer 7a) - evaluate dynamic power/toughness
        resolveCDAs(state, projectedValues, dynamicStatEntities)

        // Collect sources that generate RemoveAllAbilities effects. These sources (e.g., Humility)
        // should not have their own effects suppressed even when they themselves lose abilities,
        // because their continuous effects are self-sustaining (removing the ability that removes
        // abilities would create a paradox).
        val removeAllAbilitiesSources = sortedEffects
            .filter { it.modification is Modification.RemoveAllAbilities }
            .map { it.sourceId }
            .toSet()

        // Re-resolve affected entities for Layer 7 effects that depend on subtypes, controller, or creature type.
        // Also suppress effects from sources that lost all abilities in Layer 6 (Rule 613: Humility
        // removing a lord's abilities means its continuous effects no longer apply in Layer 7).
        // Exception: sources that themselves generate RemoveAllAbilities are exempt — their effects
        // are self-sustaining (e.g., Humility's own P/T-setting effect persists).
        val resolvedLayer7Effects = sortedEffects.mapNotNull { rawEffect ->
            val effect = applyControllerGate(rawEffect, projectedValues)
            if (effect.layer != Layer.POWER_TOUGHNESS) return@mapNotNull effect

            // Suppress effects from sources that lost all abilities (e.g., a lord under Humility),
            // but NOT from sources that are themselves the source of a RemoveAllAbilities effect.
            val sourceProjected = projectedValues[effect.sourceId]
            if (sourceProjected != null && sourceProjected.lostAllAbilities &&
                effect.sourceId !in removeAllAbilitiesSources) {
                return@mapNotNull null
            }

            if (effect.affectsFilter != null &&
                (filterResolver.isSubtypeDependentFilter(effect.affectsFilter) ||
                    filterResolver.isControllerDependentFilter(effect.affectsFilter) ||
                    filterResolver.isCreatureDependentFilter(effect.affectsFilter))) {
                effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
            } else {
                effect
            }
        }

        // Apply layer 7 continuous effects
        for (effect in resolvedLayer7Effects) {
            if (effect.layer == Layer.POWER_TOUGHNESS) {
                effectApplicator.applyEffect(effect, state, projectedValues)
            }
        }

        // Apply counters (layer 7d)
        effectApplicator.applyCounters(state, projectedValues)

        // Post-layer pass: grant CANT_BE_BLOCKED to creatures qualifying via
        // GrantCantBeBlockedToSmallCreatures (e.g., Tetsuko Umezawa, Fugitive).
        // Must happen after all P/T layers so projected power/toughness is final.
        applyGrantCantBeBlockedToSmallCreatures(state, projectedValues)

        // Post-layer pass: enforce the affected-power half of
        // [Duration.WhileSourceTappedAndAffectedPowerAtMostSource] (Old Man of the Sea).
        // We could only apply the source-tapped half during Layer 2 collection because Layer 7
        // hadn't projected power yet. Now that final projected power is known, walk the floating
        // effects with this Duration and revert the controller change for any affected entity
        // whose projected power exceeds the source's projected power. This catches every pump
        // source — base printed power, +1/+1 / -1/-1 counters, Layer-7 floating pumps (Giant
        // Growth, Aggressive Urge), and lord-style anthems alike.
        applyAffectedPowerAtMostSourceGate(state, projectedValues)

        // Convert to immutable
        val finalValues = projectedValues.mapValues { (_, v) ->
            ProjectedValues(
                power = v.power,
                toughness = v.toughness,
                keywords = v.keywords.toSet(),
                colors = v.colors.toSet(),
                types = v.types.toSet(),
                subtypes = v.subtypes.toSet(),
                controllerId = v.controllerId,
                isFaceDown = v.isFaceDown,
                isSuspected = v.isSuspected,
                cantAttack = v.cantAttack,
                cantBlock = v.cantBlock,
                mustAttack = v.mustAttack,
                mustBlock = v.mustBlock,
                cantBeBlockedExceptByFilters = v.cantBeBlockedExceptByFilters.toList(),
                canOnlyBlockCreaturesWithFilters = v.canOnlyBlockCreaturesWithFilters.toList(),
                additionalBlockCount = v.additionalBlockCount,
                lostAllAbilities = v.lostAllAbilities
            )
        }

        return ProjectedState(state, finalValues)
    }

    fun getProjectedPower(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getPower(entityId) ?: 0
    }

    fun getProjectedToughness(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getToughness(entityId) ?: 0
    }

    fun getProjectedKeywords(state: GameState, entityId: EntityId): Set<Keyword> {
        val projected = project(state)
        return projected.getKeywords(entityId).mapNotNull { keywordName ->
            try {
                Keyword.valueOf(keywordName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    }

    fun hasProjectedKeyword(state: GameState, entityId: EntityId, keyword: Keyword): Boolean {
        val projected = project(state)
        return projected.hasKeyword(entityId, keyword)
    }

    private fun applyTextReplacements(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val textReplacement = container.get<TextReplacementComponent>() ?: continue
            val values = projectedValues[entityId] ?: continue

            val transformedSubtypes = values.subtypes.map { textReplacement.applyToCreatureType(it) }.toMutableSet()
            values.subtypes.clear()
            values.subtypes.addAll(transformedSubtypes)

            val oldSubtypesInTypes = values.types.filter { type ->
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == type } == true
            }.toSet()
            values.types.removeAll(oldSubtypesInTypes)
            values.types.addAll(transformedSubtypes)

            val protectionSubtypePrefix = "PROTECTION_FROM_SUBTYPE_"
            val protectionKeywords = values.keywords.filter { it.startsWith(protectionSubtypePrefix) }
            for (keyword in protectionKeywords) {
                val originalSubtype = keyword.removePrefix(protectionSubtypePrefix)
                val transformed = textReplacement.applyToCreatureType(originalSubtype).uppercase()
                if (transformed != originalSubtype) {
                    values.keywords.remove(keyword)
                    values.keywords.add("$protectionSubtypePrefix$transformed")
                }
            }

            // Rewrite protection-from-color keywords for color-word changes (Crystal Spray:
            // "protection from red" -> "protection from blue"). Color protection keywords are
            // "PROTECTION_FROM_<COLOR.name>"; skip the SUBTYPE_/SUPERTYPE_ variants by only
            // matching suffixes that name an actual Color.
            val colorPrefix = "PROTECTION_FROM_"
            val colorProtectionKeywords = values.keywords.filter { kw ->
                kw.startsWith(colorPrefix) &&
                    com.wingedsheep.sdk.core.Color.entries.any { it.name == kw.removePrefix(colorPrefix) }
            }
            for (keyword in colorProtectionKeywords) {
                val originalColor = com.wingedsheep.sdk.core.Color.valueOf(keyword.removePrefix(colorPrefix))
                val transformed = textReplacement.replaceColor(originalColor)
                if (transformed != originalColor) {
                    values.keywords.remove(keyword)
                    values.keywords.add("$colorPrefix${transformed.name}")
                }
            }
        }
    }

    private fun extractTypes(card: CardComponent): MutableSet<String> {
        val types = mutableSetOf<String>()
        types.addAll(card.typeLine.supertypes.map { it.name })
        types.addAll(card.typeLine.cardTypes.map { it.name })
        types.addAll(card.typeLine.subtypes.map { it.value })
        return types
    }

    /**
     * Enforce [ContinuousEffect.controllerGate] ("for as long as you control it") and
     * [ContinuousEffect.sourceControllerGate] ("for as long as you control this [source]").
     * Called after Layer 2 control is resolved, so it reflects every control-changing effect
     * (steals, Threaten, static control Auras). A no-op for effects without either gate.
     *
     * The two gates differ in *what* they watch: [controllerGate] drops the affected entities
     * whose own controller drifted; [sourceControllerGate] drops the entire effect if the
     * source's controller drifted.
     */
    private fun applyControllerGate(
        effect: ContinuousEffect,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): ContinuousEffect {
        // Source-controller gate first — failure wipes the whole effect, so the per-affected gate
        // below has nothing left to do.
        val sourceGate = effect.sourceControllerGate
        if (sourceGate != null) {
            val sourceController = projectedValues[effect.sourceId]?.controllerId
            if (sourceController != sourceGate) {
                return if (effect.affectedEntities.isEmpty()) effect
                else effect.copy(affectedEntities = emptySet())
            }
        }
        val gate = effect.controllerGate ?: return effect
        val stillControlled = effect.affectedEntities.filterTo(mutableSetOf()) { entityId ->
            projectedValues[entityId]?.controllerId == gate
        }
        return if (stillControlled.size == effect.affectedEntities.size) {
            effect
        } else {
            effect.copy(affectedEntities = stillControlled)
        }
    }

    private fun collectContinuousEffects(
        state: GameState,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): List<ContinuousEffect> {
        val effects = mutableListOf<ContinuousEffect>()

        // 1. Collect effects from static abilities on permanents
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val continuousEffectComponent = container.get<ContinuousEffectSourceComponent>()
            if (continuousEffectComponent != null) {
                val textReplacement = container.get<TextReplacementComponent>()
                effects.addAll(continuousEffectComponent.effects.map { effect ->
                    val effectiveFilter = if (textReplacement != null && effect.affectsFilter != null) {
                        effect.affectsFilter.applyTextReplacement(textReplacement)
                    } else {
                        effect.affectsFilter
                    }
                    ContinuousEffect(
                        sourceId = entityId,
                        timestamp = container.get<com.wingedsheep.engine.state.components.battlefield.TimestampComponent>()?.timestamp
                            ?: state.timestamp,
                        modification = effect.modification,
                        affectedEntities = filterResolver.resolveAffectedEntities(state, entityId, effectiveFilter, projectedValues),
                        sourceCondition = effect.sourceCondition,
                        affectsFilter = effectiveFilter
                    )
                })
            }
        }

        // 2. Collect floating effects (from resolved spells like Giant Growth)
        for (floating in state.floatingEffects) {
            if (floating.duration is Duration.WhileSourceTapped) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId) ||
                    state.getEntity(sourceId)?.has<TappedComponent>() != true) {
                    continue
                }
            }
            if (floating.duration is Duration.WhileSourceTappedAndAffectedPowerAtMostSource) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId) ||
                    state.getEntity(sourceId)?.has<TappedComponent>() != true) {
                    continue
                }
            }
            if (floating.duration is Duration.WhileSourceOnBattlefield) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId)) {
                    continue
                }
            }
            if (floating.duration is Duration.WhileYouControlSource) {
                // Battlefield half of the gate; the source-controller half is deferred to a
                // post-Layer-2 sweep ([sourceControllerGate]) because Layer 2 hasn't run yet
                // at collection time.
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId)) {
                    continue
                }
            }
            // "for as long as [the source Aura/Equipment] remains attached to it" — gate the whole
            // effect on the source still being on the battlefield (CR 611.2b). The per-affected
            // "still attached to *this* entity" half is applied below.
            if (floating.duration is Duration.WhileSourceAttachedToAffected) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId)) {
                    continue
                }
            }

            var validAffectedEntities = if (floating.effect.dynamicGroupFilter != null) {
                // Rule 611.2c: re-evaluate filter dynamically to include entities that entered later
                filterResolver.resolveAffectedEntities(
                    state,
                    floating.sourceId ?: EntityId("floating-${floating.id}"),
                    AffectsFilter.Generic(floating.effect.dynamicGroupFilter),
                    projectedValues
                )
            } else {
                // Floating effects normally target battlefield permanents, but a color-change
                // effect can target a spell on the stack (Blind Seer). Stack entities are valid
                // affected targets so the recolor projects onto the spell during resolution; an
                // entity that has since left both zones is dropped (its effect is now inert).
                floating.effect.affectedEntities.filter { entityId ->
                    state.getBattlefield().contains(entityId) || state.stack.contains(entityId)
                }.toSet()
            }

            // Per-affected "still attached to this entity" half of
            // [Duration.WhileSourceAttachedToAffected]: drop any affected entity the source is no
            // longer attached to (source moved to a different host, or detached). The latch-off is
            // handled by EndedDurationExpiryCheck so a re-attach can't resurrect it.
            if (floating.duration is Duration.WhileSourceAttachedToAffected) {
                val attachedTo = floating.sourceId?.let {
                    state.getEntity(it)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        ?.targetId
                }
                validAffectedEntities = validAffectedEntities.filterTo(LinkedHashSet()) { it == attachedTo }
            }

            // The per-affected-entity power gate for
            // [Duration.WhileSourceTappedAndAffectedPowerAtMostSource] is deferred to a
            // post-Layer-7 fix-up in [project] — we can't compare projected power here
            // because Layer 7 hasn't run yet (and reading `state.projectedState` would
            // recurse). Include the effect now; the fix-up reverts control for any affected
            // entity whose final projected power exceeds the source's final projected power.

            if (validAffectedEntities.isNotEmpty()) {
                effects.add(
                    ContinuousEffect(
                        sourceId = floating.sourceId ?: EntityId("floating-${floating.id}"),
                        timestamp = floating.timestamp,
                        modification = floating.effect.modification.toModification(),
                        affectedEntities = validAffectedEntities,
                        // "for as long as you control it" (e.g. suspend haste — CR 702.62g):
                        // the gate is applied after Layer 2, against the projected controller,
                        // so the effect drops the instant another player gains control.
                        controllerGate = if (floating.duration is Duration.WhileControlledByController) {
                            floating.controllerId
                        } else {
                            null
                        },
                        // "for as long as you control this [source]" (Aladdin, Scroll of Isildur I).
                        // Source-side mirror of [controllerGate]: drops the entire effect after Layer 2
                        // if the source's projected controller drifts (Threaten-style steal of the
                        // source, static control Aura, etc.). One-way: EndedDurationExpiryCheck removes
                        // the floating effect itself so regaining control does not re-apply.
                        sourceControllerGate = if (floating.duration is Duration.WhileYouControlSource) {
                            floating.controllerId
                        } else {
                            null
                        }
                    )
                )
            }
        }

        return effects
    }

    /**
     * Post-Layer-7 gate for [Duration.WhileSourceTappedAndAffectedPowerAtMostSource]:
     * walk the floating effects with this duration, compare each affected entity's
     * final projected power to the source's final projected power, and revert the
     * controller to the entity's base [ControllerComponent] if the affected entity is
     * now stronger. Reverting to the base controller (rather than the previously-projected
     * controller) is correct for the single-control-changer case Old Man of the Sea covers;
     * if another control effect targets the same entity simultaneously, this gate hides it,
     * which is acceptable for the current SDK surface.
     *
     * This gate is the *instantaneous* view only — it hides the effect while the condition is
     * false but would re-apply it if power dropped back. The one-way half of CR 611.2b is
     * supplied by [com.wingedsheep.engine.mechanics.sba.permanent.EndedDurationExpiryCheck],
     * which physically removes the floating effect the moment the gate fails so it can never
     * restart.
     */
    private fun applyAffectedPowerAtMostSourceGate(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (floating in state.floatingEffects) {
            if (floating.duration !is Duration.WhileSourceTappedAndAffectedPowerAtMostSource) continue
            val sourceId = floating.sourceId ?: continue
            if (!state.getBattlefield().contains(sourceId)) continue
            if (state.getEntity(sourceId)?.has<TappedComponent>() != true) continue

            val sourcePower = projectedValues[sourceId]?.power ?: continue

            for (affectedId in floating.effect.affectedEntities) {
                if (!state.getBattlefield().contains(affectedId)) continue
                val affectedPower = projectedValues[affectedId]?.power ?: continue
                if (affectedPower > sourcePower) {
                    val baseController = state.getEntity(affectedId)
                        ?.get<ControllerComponent>()
                        ?.playerId
                    projectedValues[affectedId]?.controllerId = baseController
                }
            }
        }
    }

    private fun resolveCDAs(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>,
        dynamicStatEntities: List<Pair<EntityId, CardComponent>>
    ) {
        if (dynamicStatEntities.isEmpty()) return

        val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
        for ((entityId, cardComponent) in dynamicStatEntities) {
            val values = projectedValues[entityId] ?: continue
            val controllerId = values.controllerId ?: continue
            val context = EffectContext(
                sourceId = entityId,
                controllerId = controllerId,
            )
            val baseStats = cardComponent.baseStats ?: continue
            val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()

            fun resolveDynamicAmount(source: DynamicAmount): Int {
                val effective = if (textReplacement != null) {
                    source.applyTextReplacement(textReplacement)
                } else {
                    source
                }
                return dynamicAmountEvaluator.evaluate(state, effective, context, intermediateProjected)
            }

            when (val power = baseStats.power) {
                is CharacteristicValue.Dynamic ->
                    values.power = resolveDynamicAmount(power.source)
                is CharacteristicValue.DynamicWithOffset ->
                    values.power = resolveDynamicAmount(power.source) + power.offset
                is CharacteristicValue.Fixed -> {}
            }
            when (val toughness = baseStats.toughness) {
                is CharacteristicValue.Dynamic ->
                    values.toughness = resolveDynamicAmount(toughness.source)
                is CharacteristicValue.DynamicWithOffset ->
                    values.toughness = resolveDynamicAmount(toughness.source) + toughness.offset
                is CharacteristicValue.Fixed -> {}
            }
        }
    }

    /**
     * Scan for permanents with GrantCantBeBlockedToSmallCreaturesComponent and
     * add CANT_BE_BLOCKED to creatures they control whose projected power or
     * toughness is at most the threshold.
     */
    private fun applyGrantCantBeBlockedToSmallCreatures(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        // Collect all grant sources: (controllerId, maxValue)
        val sources = mutableListOf<Pair<EntityId, Int>>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val grant = container.get<GrantCantBeBlockedToSmallCreaturesComponent>() ?: continue
            val controllerId = projectedValues[entityId]?.controllerId ?: continue
            sources.add(controllerId to grant.maxValue)
        }
        if (sources.isEmpty()) return

        // For each creature on the battlefield, check if any source applies
        for (entityId in state.getBattlefield()) {
            val values = projectedValues[entityId] ?: continue
            if (!values.types.contains("CREATURE")) continue
            val power = values.power ?: continue
            val toughness = values.toughness ?: continue
            val controllerId = values.controllerId ?: continue

            for ((sourceController, maxValue) in sources) {
                if (sourceController == controllerId && (power <= maxValue || toughness <= maxValue)) {
                    values.keywords.add(AbilityFlag.CANT_BE_BLOCKED.name)
                    break
                }
            }
        }
    }
}
