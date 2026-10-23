package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.components.battlefield.chosenLandType
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.handlers.ConditionEvaluationContext
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.KeywordCardPool
import com.wingedsheep.sdk.scripting.KeywordFamily
import com.wingedsheep.sdk.model.EntityId

/**
 * Applies continuous effects and counters to mutable projected values.
 */
internal class EffectApplicator(
    /**
     * Source conditions are evaluated while the projection is still being built, so this is
     * [StateProjector]'s non-reentrant evaluator: reaching for the canonical lazy
     * GameState.projectedState here would re-enter the projector's initializer and recurse until
     * the stack overflows (e.g. a Layer-7 conditional P/T static ability whose source condition
     * counts creatures via a battlefield aggregate). Its empty default projection falls back to
     * base CardComponent values, exactly as CDA resolution does.
     */
    private val conditionEvaluator: ConditionEvaluator
) {
    private val dynamicAmountEvaluator = conditionEvaluator.amounts

    /**
     * Adds a granted keyword string. A numeric grant `<KEYWORD>_<n>` (granted toxic, granted
     * bushido) is *summed* into any `<KEYWORD>_<m>` already there rather than added beside it: the
     * projected keyword set can't hold two equal strings, so two "gains toxic 1" grants — or a
     * printed toxic 2 plus a granted toxic 2 — would otherwise collapse to one and lose the N the
     * readers sum (CR 702.164b's total toxic value, `KeywordValue`).
     *
     * Crew is the exception: each `CREW_<n>` is a separate crew ability, not a share of one total
     * (a Vehicle with crew 3 that gains crew 1 can be crewed for 1, not 4), so distinct Ns sit side
     * by side and the crew readers take the cheapest.
     */
    private fun addKeyword(keywords: MutableSet<String>, keyword: String) {
        val split = keyword.lastIndexOf('_')
        val n = if (split > 0) keyword.substring(split + 1).toIntOrNull() else null
        if (n == null || keyword.startsWith(CREW_PREFIX)) {
            keywords.add(keyword)
            return
        }
        val prefix = keyword.substring(0, split + 1)
        val existing = keywords.firstOrNull { it.startsWith(prefix) && it.substring(prefix.length).toIntOrNull() != null }
        if (existing == null) {
            keywords.add(keyword)
        } else {
            keywords.remove(existing)
            keywords.add(prefix + (existing.substring(prefix.length).toInt() + n))
        }
    }

    fun applyEffect(
        effect: ContinuousEffect,
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>,
        restrictionSurvivesSourceAbilityRemoval: Boolean = false
    ) {
        val sourceCondition = effect.sourceCondition
        if (sourceCondition != null) {
            val sourceValues = projectedValues[effect.sourceId]
            val ctx = ConditionEvaluationContext.Projection(
                sourceId = effect.sourceId,
                sourceValues = sourceValues,
                projectedValues = projectedValues,
                fallbackControllerId = effect.controllerId
            )
            if (!conditionEvaluator.evaluate(state, sourceCondition, ctx)) return
        }

        for (entityId in effect.affectedEntities) {
            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            when (val mod = effect.modification) {
                is Modification.SetPowerToughness -> {
                    values.power = mod.power
                    values.toughness = mod.toughness
                }
                is Modification.SetPowerToughnessDynamic -> {
                    // The dynamic value *is* the base P/T, recomputed on every projection pass:
                    // either an effect-granted "base power is equal to …" (CR 613.4b, layer 7b) or
                    // a star/star CDA (CR 604.3), which CR 613.4a actually puts in layer 7a but
                    // which this engine lowers into SET_VALUES alongside the 7b sets — see the
                    // KDoc on Modification.SetPowerToughnessDynamic.
                    // Fall back to the effect's captured controller when the source has left the
                    // battlefield (its ControllerComponent is gone) — Titania's Song's until-EOT
                    // linger keeps animating after the enchantment leaves.
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                        ?: effect.controllerId
                    // CR 208.3a: an effect setting the base P/T of a *noncreature* permanent is
                    // still created, it just "doesn't do anything unless that permanent becomes a
                    // creature". Layer 4 has already been applied into `values.types` by the time
                    // we get here and the gate is re-asked on every pass, so a Vehicle crewed later
                    // in the turn does pick the value up. Note the fixed SetPower / SetToughness /
                    // SetPowerToughness branches below write unconditionally — this is the one
                    // place the dynamic and fixed base-P/T sets genuinely diverge.
                    if (controllerId != null && "CREATURE" in values.types) {
                        val context = EffectContext(
                            sourceId = effect.sourceId,
                            controllerId = controllerId,
                            affectedEntityId = entityId
                        )
                        val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
                        // A null half means "leave that stat alone" — the same semantics as
                        // SetBaseStatsEffect's null power/toughness, so "base power is equal to X"
                        // does not silently zero the printed toughness.
                        mod.power?.let {
                            values.power = dynamicAmountEvaluator.evaluate(state, it, context, intermediateProjected)
                        }
                        mod.toughness?.let {
                            values.toughness = dynamicAmountEvaluator.evaluate(state, it, context, intermediateProjected)
                        }
                    }
                }
                is Modification.SetPower -> {
                    values.power = mod.power
                }
                is Modification.SetToughness -> {
                    values.toughness = mod.toughness
                }
                is Modification.ModifyPowerToughness -> {
                    // CR 208.3: noncreature permanents have no power/toughness, even if printed
                    // values appear on them (e.g., a Vehicle that isn't currently a creature).
                    // P/T modifications apply only while the affected permanent is a creature.
                    if ("CREATURE" in values.types) {
                        values.power = (values.power ?: 0) + mod.powerMod
                        values.toughness = (values.toughness ?: 0) + mod.toughnessMod
                    }
                }
                is Modification.SwitchPowerToughness -> {
                    val p = values.power
                    val t = values.toughness
                    values.power = t
                    values.toughness = p
                }
                is Modification.SetName -> {
                    values.name = mod.name
                }
                is Modification.GrantKeyword -> {
                    addKeyword(values.keywords, mod.keyword)
                    // Changeling grants all creature types (Rule 702.73)
                    if (mod.keyword == Keyword.CHANGELING.name) {
                        values.subtypes.addAll(com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES)
                    }
                }
                is Modification.PreventEnchantment -> {
                    values.enchantmentRestrictions.add(ActiveEnchantmentRestriction(
                        effect.sourceId, mod.auras, mod.exceptSource, restrictionSurvivesSourceAbilityRemoval
                    ))
                }
                is Modification.RemoveKeyword -> {
                    values.keywords.remove(mod.keyword)
                    // CR 702.22b: losing banding loses every "bands with other" ability too, and
                    // the family name itself means "loses all 'bands with other' abilities".
                    if (mod.keyword == Keyword.BANDING.name ||
                        mod.keyword == KeywordAbility.BandsWithOther.FAMILY_KEYWORD
                    ) {
                        values.keywords.removeAll { KeywordAbility.BandsWithOther.isBandsWithOther(it) }
                    }
                }
                is Modification.GrantLandwalkFromChosen -> {
                    val chosenLandType = state.getEntity(effect.sourceId)
                        ?.chosenLandType()
                    val landwalk = when (chosenLandType) {
                        com.wingedsheep.sdk.core.Subtype.PLAINS.value -> Keyword.PLAINSWALK
                        com.wingedsheep.sdk.core.Subtype.ISLAND.value -> Keyword.ISLANDWALK
                        com.wingedsheep.sdk.core.Subtype.SWAMP.value -> Keyword.SWAMPWALK
                        com.wingedsheep.sdk.core.Subtype.MOUNTAIN.value -> Keyword.MOUNTAINWALK
                        com.wingedsheep.sdk.core.Subtype.FOREST.value -> Keyword.FORESTWALK
                        else -> null
                    }
                    if (landwalk != null) {
                        values.keywords.add(landwalk.name)
                    }
                }
                is Modification.ChangeColor -> {
                    values.colors.clear()
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddColor -> {
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddChosenColor -> {
                    val chosenColor = state.getEntity(effect.sourceId)
                        ?.chosenColor()
                    if (chosenColor != null) {
                        values.colors.add(chosenColor.name)
                    }
                }
                is Modification.AddType -> {
                    values.types.add(mod.type)
                }
                is Modification.AddSubtype -> {
                    values.types.add(mod.subtype)
                    values.subtypes.add(mod.subtype)
                }
                is Modification.AddChosenSubtype -> {
                    // The cross-zone flags on the modification are read by StateProjector to build
                    // ProjectedState.crossZoneSubtypeGrants; Layer 4 projection here only ever
                    // touches battlefield permanents, so they're irrelevant to this branch.
                    // "The chosen type" is whichever type the source chose as it entered: a
                    // creature type (Adaptive Automaton) or a basic land type (Thran Portal, whose
                    // new land type then carries its intrinsic mana ability, CR 305.6).
                    val source = state.getEntity(effect.sourceId)
                    val chosenType = source?.chosenCreatureType() ?: source?.chosenLandType()
                    if (chosenType != null) {
                        values.types.add(chosenType)
                        values.subtypes.add(chosenType)
                    }
                }
                is Modification.AddAllCreatureTypes -> {
                    values.subtypes.addAll(com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES)
                }
                is Modification.RemoveType -> {
                    values.types.remove(mod.type)
                    values.removedCardTypes.add(mod.type)
                }
                is Modification.SetCreatureSubtypes -> {
                    val creatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES.toSet()
                    values.subtypes.removeAll { it in creatureTypes }
                    values.types.removeAll { it in creatureTypes }
                    values.subtypes.addAll(mod.subtypes)
                    values.types.addAll(mod.subtypes)
                }
                is Modification.SetCreatureSubtypesFrom -> {
                    // "Has the creature types of [that object]" (Duplicant). Same replace-only-the-
                    // creature-types semantics as SetCreatureSubtypes above (CR 205.1b — non-creature
                    // subtypes are untouched), but the set is read from the referenced object on every
                    // projection pass instead of being baked in at conversion time.
                    //
                    // The reference is resolved against a bare EffectContext rooted at the *effect's*
                    // source, which is all LinkedExiledCard needs — see ProjectionScopedAmounts for
                    // why the context-scoped references can't be used here. The referenced card is in
                    // exile and has no projection entry, so its printed creature types are the right
                    // (and only) read.
                    val creatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES.toSet()
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                        ?: effect.controllerId
                    val donorId = controllerId?.let {
                        com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveEntity(
                            mod.source,
                            EffectContext(
                                sourceId = effect.sourceId,
                                controllerId = it,
                                affectedEntityId = entityId
                            ),
                            state
                        )
                    }
                    val donorTypes = donorId
                        ?.let { state.getEntity(it)?.get<CardComponent>() }
                        ?.typeLine?.subtypes.orEmpty()
                        .map { it.value }
                        .filter { it in creatureTypes }
                    val newTypes = donorTypes.toSet() + mod.retainedTypes
                    values.subtypes.removeAll { it in creatureTypes }
                    values.types.removeAll { it in creatureTypes }
                    values.subtypes.addAll(newTypes)
                    values.types.addAll(newTypes)
                }
                is Modification.SetBasicLandTypes -> {
                    val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                    values.subtypes.removeAll { it in basicLandTypes }
                    values.types.removeAll { it in basicLandTypes }
                    values.subtypes.addAll(mod.subtypes)
                    values.types.addAll(mod.subtypes)
                    // The new type's intrinsic mana ability survives a same-source lose-all-
                    // abilities effect (Blood Moon / Zhao). See ProjectedValues.basicLandTypesSetByEffect.
                    values.basicLandTypesSetByEffect = true
                }
                is Modification.SetBasicLandTypesFromChosen -> {
                    val chosenLandType = state.getEntity(effect.sourceId)
                        ?.chosenLandType()
                    if (chosenLandType != null) {
                        val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                        values.subtypes.removeAll { it in basicLandTypes }
                        values.types.removeAll { it in basicLandTypes }
                        values.subtypes.add(chosenLandType)
                        values.types.add(chosenLandType)
                        values.basicLandTypesSetByEffect = true
                    }
                }
                is Modification.SetCardTypes -> {
                    val cardTypeStrings = setOf("CREATURE", "ARTIFACT", "ENCHANTMENT", "LAND", "PLANESWALKER", "INSTANT", "SORCERY", "BATTLE", "KINDRED")
                    values.types.removeAll { it in cardTypeStrings }
                    values.types.addAll(mod.types)
                }
                is Modification.SetAllSubtypes -> {
                    values.subtypes.clear()
                    values.subtypes.addAll(mod.subtypes)
                }
                is Modification.ChangeController -> {
                    values.controllerId = mod.newControllerId
                }
                is Modification.ChangeControllerToSourceController -> {
                    val sourceController = state.getEntity(effect.sourceId)
                        ?.get<ControllerComponent>()?.playerId
                    if (sourceController != null) {
                        values.controllerId = sourceController
                    }
                }
                is Modification.GrantProtectionFromColor -> {
                    values.keywords.add("PROTECTION_FROM_${mod.color}")
                }
                is Modification.GrantProtectionFromCardType -> {
                    values.keywords.add("PROTECTION_FROM_CARDTYPE_${mod.cardType.uppercase()}")
                }
                is Modification.GrantProtectionFromChosenColor -> {
                    val chosenColor = state.getEntity(effect.sourceId)
                        ?.chosenColor()
                    if (chosenColor != null) {
                        values.keywords.add("PROTECTION_FROM_${chosenColor.name}")
                    }
                }
                is Modification.GrantHexproofFromOwnColors -> {
                    for (colorName in values.colors) {
                        values.keywords.add("HEXPROOF_FROM_$colorName")
                    }
                }
                is Modification.GrantHexproofFrom -> {
                    values.keywords.addAll(mod.keywords)
                }
                is Modification.GrantProtectionFrom -> {
                    values.keywords.addAll(mod.keywords)
                }
                is Modification.GrantProtectionFromControlledColors -> {
                    // Protection from the colors of permanents the source's controller controls
                    // (e.g., Pledge of Loyalty). Read the projected controller + projected colors
                    // so color-changing effects (Layer 5) are reflected; colorless permanents add
                    // no color.
                    val sourceController = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    if (sourceController != null) {
                        for (other in projectedValues.values) {
                            if (other.controllerId != sourceController) continue
                            for (colorName in other.colors) {
                                values.keywords.add("PROTECTION_FROM_$colorName")
                            }
                        }
                    }
                }
                is Modification.GrantProtectionFromLinkedExiledCardTypes -> {
                    // Protection from each card type of the cards exiled with the source (Imprint,
                    // CR 702.15 — Mirror Golem). The card-type twin of the controlled-colors branch
                    // above: the *set* of qualities comes from game state rather than the card text.
                    // Card types are read off the exiled card's own type line — an exiled card is
                    // never on the battlefield, so there is no projection entry to consult, and no
                    // layer rewrites a card's types in exile.
                    for (exiledId in LinkedExileLookup.exiledCards(state, effect.sourceId)) {
                        val typeLine = state.getEntity(exiledId)?.get<CardComponent>()?.typeLine ?: continue
                        for (cardType in typeLine.cardTypes) {
                            values.keywords.add("PROTECTION_FROM_CARDTYPE_${cardType.name}")
                        }
                    }
                }
                is Modification.GrantKeywordsOfCards -> {
                    // Cairn Wanderer / Eater of Virtue. A card in a graveyard or in exile has no
                    // projection entry and nothing on the battlefield can grant it an ability, so
                    // its keywords are its printed ones: the same base keywords and printed
                    // protections the projector starts a permanent from.
                    val pool: List<EntityId> = when (mod.cards) {
                        KeywordCardPool.CREATURE_CARDS_IN_GRAVEYARDS -> state.turnOrder.flatMap { playerId ->
                            state.getGraveyard(playerId).filter {
                                state.getEntity(it)?.get<CardComponent>()?.typeLine?.isCreature == true
                            }
                        }
                        KeywordCardPool.CARDS_EXILED_WITH_SOURCE ->
                            LinkedExileLookup.exiledCards(state, effect.sourceId)
                    }
                    val landwalk = KeywordFamily.LANDWALK in mod.families
                    val hexproof = KeywordFamily.HEXPROOF in mod.families
                    for (cardId in pool) {
                        val container = state.getEntity(cardId) ?: continue
                        val card = container.get<CardComponent>() ?: continue
                        for (keyword in card.baseKeywords) {
                            if (keyword.name in mod.keywords ||
                                (landwalk && keyword in Keyword.LANDWALK) ||
                                (hexproof && keyword == Keyword.HEXPROOF)
                            ) {
                                values.keywords.add(keyword.name)
                            }
                        }
                        if (KeywordFamily.PROTECTION in mod.families) {
                            PrintedKeywordQualities.protections(container, values.keywords)
                        }
                        if (hexproof) PrintedKeywordQualities.hexproofFroms(container, values.keywords)
                    }
                }
                is Modification.CanAttackAsThoughHasty -> {
                    values.canAttackAsThoughHasty = true
                }
                is Modification.SetCantAttack -> {
                    values.cantAttack = true
                }
                is Modification.SetAttacksWithoutTapping -> {
                    values.attacksWithoutTapping = true
                }
                is Modification.SetSuspected -> {
                    values.isSuspected = true
                }
                is Modification.SetCantBlock -> {
                    values.cantBlock = true
                }
                is Modification.SetCantBeTurnedFaceUp -> {
                    values.cantBeTurnedFaceUp = true
                }
                is Modification.SetMustAttack -> {
                    values.mustAttack = true
                }
                is Modification.SetMustAttackPlayer -> {
                    values.mustAttack = true
                    values.mustAttackPlayer = true
                }
                is Modification.SetMustBlock -> {
                    values.mustBlock = true
                }
                is Modification.CanBlockAdditional -> {
                    values.additionalBlockCount += mod.count
                }
                is Modification.ManaAbilitiesCostLife -> {
                    values.manaAbilityLifeTax += mod.amount
                }
                is Modification.CantBeBlockedExceptBy -> {
                    values.cantBeBlockedExceptByFilters.add(mod.blockerFilter)
                }
                is Modification.CanOnlyBlockCreaturesWith -> {
                    values.canOnlyBlockCreaturesWithFilters.add(mod.blockerFilter)
                }
                is Modification.ModifyPowerToughnessDynamic -> {
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    // See ModifyPowerToughness above: P/T mods apply only to creatures.
                    if (controllerId != null && "CREATURE" in values.types) {
                        val context = EffectContext(
                            sourceId = effect.sourceId,
                            controllerId = controllerId,
                            affectedEntityId = entityId
                        )
                        val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
                        val powerMod = dynamicAmountEvaluator.evaluate(state, mod.powerBonus, context, intermediateProjected)
                        val toughnessMod = dynamicAmountEvaluator.evaluate(state, mod.toughnessBonus, context, intermediateProjected)
                        values.power = (values.power ?: 0) + powerMod
                        values.toughness = (values.toughness ?: 0) + toughnessMod
                    }
                }
                is Modification.GrantStaticAbility -> {
                    values.staticAbilities.add(mod.ability)
                    values.staticAbilitySourceIds.add(effect.sourceId)
                }
                is Modification.RemoveAllAbilities -> {
                    values.keywords.clear()
                    values.staticAbilities.clear()
                    values.staticAbilitySourceIds.clear()
                    values.manaAbilityLifeTax = 0
                    values.lostAllAbilities = true
                    values.hasExileOnDeathAbility = false
                }
                is Modification.GrantExileOnDeathAbility -> {
                    values.hasExileOnDeathAbility = true
                }
                is Modification.NoOp -> {
                    // No-op: effect doesn't modify projected state
                }
            }
        }
    }

    fun applyCounters(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            // Stat counters (CR 122.1a — a +X/+Y counter adds X to power and Y to toughness; layer
            // 7c per CR 613.4c). Every printed size sums the same way, so read each kind's delta off
            // its id rather than enumerating them: -0/-2 is a distinct kind from two -0/-1s.
            var powerMod = 0
            var toughnessMod = 0
            for ((type, count) in counters.counters) {
                val (power, toughness) = type.ptDelta ?: continue
                powerMod += power * count
                toughnessMod += toughness * count
            }

            if (powerMod != 0) {
                values.power = (values.power ?: 0) + powerMod
            }
            if (toughnessMod != 0) {
                values.toughness = (values.toughness ?: 0) + toughnessMod
            }
        }
    }
}

/** Prefix of a granted crew keyword ([com.wingedsheep.sdk.scripting.KeywordAbility.grantedCrew]); see `addKeyword`. */
private val CREW_PREFIX = "${com.wingedsheep.sdk.core.Keyword.CREW.name}_"
