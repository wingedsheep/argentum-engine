package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.*
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.mechanics.mana.spellPaymentContextFor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot

/**
 * Thin mapping layer from engine [LegalAction] to server [LegalActionInfo] DTO.
 *
 * Adds presentation-only data: mana source info for the pre-cast UI.
 * The client protocol (LegalActionInfo) remains unchanged.
 */
class LegalActionEnricher(
    private val manaSolver: ManaSolver,
    private val cardRegistry: CardRegistry
) {
    fun enrich(actions: List<LegalAction>, state: GameState, playerId: EntityId): List<LegalActionInfo> {
        val manaSourceInfos = buildManaSourceInfos(state, playerId)
        val restrictedMana = state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList()
        return actions.map { action ->
            toLegalActionInfo(
                action,
                manaSourceInfos,
                eligibleRestrictedMana = if (restrictedMana.isEmpty() || !shouldExposeManaSources(action)) null
                else buildEligibleRestrictedMana(state, action, restrictedMana)
            )
        }
    }

    /**
     * The subset of [restrictedMana] whose restriction is satisfied by this action's payment —
     * the mana the client may count as spendable while it does its own cost math (convoke bar,
     * waterbend/harmonize selectors). Returns null when the action isn't a cast/activation we can
     * build a payment context for, so the client falls back to unrestricted mana only.
     */
    private fun buildEligibleRestrictedMana(
        state: GameState,
        action: LegalAction,
        restrictedMana: List<RestrictedManaEntry>
    ): List<ClientRestrictedManaEntry>? {
        val paymentContext = when (val gameAction = action.action) {
            // CR 708.2 — a face-down cast is a nameless 2/2 creature spell, so it never carries
            // the printed card's characteristics into the restriction check.
            is CastSpell -> if (gameAction.castFaceDown) {
                SpellPaymentContext.faceDownCast(
                    isFromHand = action.sourceZone == null || action.sourceZone == "HAND"
                )
            } else state.getEntity(gameAction.cardId)?.get<CardComponent>()?.let { card ->
                spellPaymentContextFor(
                    card,
                    isKicked = gameAction.declaredCostSlot == ChoiceSlot.KICKED,
                    isFromExile = action.sourceZone == "EXILE",
                    // Null sourceZone means the standard hand cast.
                    isFromHand = action.sourceZone == null || action.sourceZone == "HAND"
                )
            }
            is ActivateAbility -> state.getEntity(gameAction.sourceId)?.get<CardComponent>()?.let { card ->
                // Best-effort ability lookup: this is a presentation-only hint about which
                // restricted mana the client may show as spendable. An ability granted at runtime
                // isn't on the printed script, so the lookup can miss and the equip fact reads
                // false — the client then under-reports spendable mana rather than over-reporting
                // it, and the server's own payment check (which always has the ability) decides.
                val ability = cardRegistry.getCard(card.cardDefinitionId)
                    ?.script?.activatedAbilities?.find { it.id == gameAction.abilityId }
                buildAbilityPaymentContext(card, state.projectedState, gameAction.sourceId, ability)
            }
            else -> null
        } ?: return null

        return restrictedMana
            .filter { it.restriction.isSatisfiedBy(paymentContext) }
            .map { ClientRestrictedManaEntry(it.color?.symbol?.toString(), it.restriction.description) }
    }

    private fun toLegalActionInfo(
        action: LegalAction,
        manaSourceInfos: List<ManaSourceInfo>?,
        eligibleRestrictedMana: List<ClientRestrictedManaEntry>?
    ): LegalActionInfo {
        return LegalActionInfo(
            actionType = action.actionType,
            description = action.description,
            action = action.action,
            isAffordable = action.affordable,
            validTargets = action.validTargets,
            requiresTargets = action.requiresTargets,
            targetCount = action.targetCount,
            minTargets = action.minTargets,
            targetDescription = action.targetDescription,
            targetRequirements = action.targetRequirements?.map { it.toDto() },
            xConstrainsTargetManaValue = action.xConstrainsTargetManaValue,
            xConstrainsTargetManaValueExactly = action.xConstrainsTargetManaValueExactly,
            xConstrainsTargetPower = action.xConstrainsTargetPower,
            xConstrainsTargetCount = action.xConstrainsTargetCount,
            validAttackers = action.validAttackers,
            mandatoryAttackers = action.mandatoryAttackers,
            validAttackTargets = action.validAttackTargets,
            validBlockers = action.validBlockers,
            blockerMaxBlockCounts = action.blockerMaxBlockCounts,
            mandatoryBlockerAssignments = action.mandatoryBlockerAssignments,
            hasXCost = action.hasXCost,
            maxAffordableX = action.maxAffordableX,
            minX = action.minX,
            isManaAbility = action.isManaAbility,
            requiresManaColorChoice = action.requiresManaColorChoice,
            availableManaColors = action.availableManaColors?.map { it.name },
            additionalCostInfo = action.additionalCostInfo?.toDto(),
            hasConvoke = action.hasConvoke,
            validConvokeCreatures = action.convokeCreatures?.map { it.toDto() },
            hasTapForGeneric = action.hasTapForGeneric,
            validTapForGenericPermanents = action.tapForGenericPermanents?.map { it.toDto() },
            tapForGenericAmount = action.tapForGenericAmount,
            tapForGenericLabel = action.tapForGenericLabel,
            hasDelve = action.hasDelve,
            validDelveCards = action.delveCards?.map { it.toDto() },
            minDelveNeeded = action.minDelveNeeded,
            hasHarmonize = action.hasHarmonize,
            validHarmonizeCreatures = action.harmonizeCreatures?.map { it.toDto() },
            manaCostString = action.manaCostString,
            manaCostPerExtraTarget = action.manaCostPerExtraTarget,
            minimumManaCostString = minimumManaCostString(action),
            requiresDamageDistribution = action.requiresDamageDistribution,
            totalDamageToDistribute = action.totalDamageToDistribute,
            minDamagePerTarget = action.minDamagePerTarget,
            autoTapPreview = action.autoTapPreview,
            availableManaSources = if (shouldExposeManaSources(action)) manaSourceInfos else null,
            eligibleRestrictedMana = eligibleRestrictedMana,
            sourceZone = action.sourceZone,
            tapForPower = action.tapForPower,
            tapForPowerRequired = action.tapForPowerRequired,
            tapForPowerCreatures = action.tapForPowerCreatures?.map { it.toDto() },
            maxRepeatableActivations = action.maxRepeatableActivations,
            modalEnumeration = action.modalEnumeration?.toDto(),
            holdPriority = action.holdPriority
        )
    }

    /**
     * The floor of [action]'s mana cost once every alternative payment it already carries is spent
     * to the maximum — see [LegalActionInfo.minimumManaCostString] for what the client does with it.
     * Null when the action offers none of those keywords, or when none of them can move this
     * particular cost (an all-colored cost with no convoke creature of a matching color, say).
     *
     * Derived here rather than at each of the dozen `LegalAction(...)` emission sites because it is a
     * pure function of fields the action already carries: every enumerator that offers one of these
     * keywords — cast from hand, cast from graveyard, the modal per-mode variants — gets it for free,
     * and there is no second copy of the arithmetic to drift.
     */
    private fun minimumManaCostString(action: LegalAction): String? {
        val printed = action.manaCostString?.let { ManaCost.parse(it) } ?: return null
        var floor = printed

        // Convoke (CR 702.51a): each untapped creature pays either one generic mana or one pip of a
        // color that creature is. `reduceByConvoke` is the rule; the colors decide how far it goes.
        action.convokeCreatures?.takeIf { it.isNotEmpty() }?.let { creatures ->
            floor = floor.reduceByConvoke(creatures.map { it.colors })
        }
        // Delve (CR 702.66a): each card exiled from the graveyard pays one generic mana.
        action.delveCards?.takeIf { it.isNotEmpty() }?.let { cards ->
            floor = floor.reduceGeneric(cards.size)
        }
        // Tap-for-generic (improvise CR 702.126a, waterbend): each tapped permanent pays {1} of the
        // generic, capped at `tapForGenericAmount` where one applies (waterbend {N}). A null cap is
        // improvise or the "waterbend {X}" shape, whose bound is just the generic in the cost —
        // every tappable permanent counts toward the floor there.
        action.tapForGenericPermanents?.takeIf { it.isNotEmpty() }?.let { permanents ->
            floor = floor.reduceGeneric(action.tapForGenericAmount?.coerceAtMost(permanents.size) ?: permanents.size)
        }
        // Harmonize: at most one creature may be tapped, so the floor comes from the best power on
        // offer, not from their sum.
        action.harmonizeCreatures?.takeIf { it.isNotEmpty() }?.let { creatures ->
            floor = floor.reduceGeneric(creatures.maxOf { it.power }.coerceAtLeast(0))
        }

        // The reductions don't preserve symbol order, so an unchanged cost can still compare unequal
        // — mana value is what says whether anything actually moved. A cost reduced away entirely
        // renders as "{0}" rather than the empty string the symbol list would produce.
        if (floor.cmc >= printed.cmc) return null
        return floor.toString().ifEmpty { "{0}" }
    }

    private fun shouldExposeManaSources(action: LegalAction): Boolean =
        action.autoTapPreview != null ||
            (action.hasXCost && action.manaCostString != null) ||
            action.hasConvoke ||
            action.hasTapForGeneric ||
            action.hasDelve ||
            action.hasHarmonize

    private fun ModalLegalEnumeration.toDto() = ModalLegalEnumerationInfo(
        chooseCount = chooseCount,
        minChooseCount = minChooseCount,
        allowRepeat = allowRepeat,
        additionalManaCostPerExtraMode = additionalManaCostPerExtraMode,
        additionalCostPerExtraMode = additionalCostPerExtraMode?.toDto(),
        modes = modes.map { it.toDto() },
        unavailableIndices = unavailableIndices
    )

    private fun ModalEnumerationMode.toDto() = ModalEnumerationModeInfo(
        index = index,
        description = description,
        available = available,
        additionalManaCost = additionalManaCost,
        additionalCostInfo = additionalCostInfo?.toDto(),
        targetRequirements = targetRequirements.map { it.toDto() }
    )

    private fun buildManaSourceInfos(state: GameState, playerId: EntityId): List<ManaSourceInfo>? {
        // Sacrifice-requiring sources (e.g. Treasure tokens) are excluded from the
        // spell-cast pre-cast picker because the cast/payment pipeline doesn't yet
        // sacrifice on selection. They remain selectable in resolution-time mana
        // menus (ward, may-pay), which do sacrifice — see ManaPaymentContinuationResumer.
        val sources = manaSolver.findAvailableManaSources(state, playerId)
            .filter { !it.requiresSacrifice }
        if (sources.isEmpty()) return null
        return sources.map { source ->
            val card = state.getEntity(source.entityId)?.get<CardComponent>()
            // The entity's own imageUri carries the printing the player actually put in their deck
            // (stamped by CardEntityFactory); the definition lookup is only a fallback for entities
            // with no image stamped. Deriving it from the definition alone would show the original
            // printing's art for a reprint, so the mana picker wouldn't match the battlefield.
            val imageUri = card?.imageUri
                ?: card?.let { cardRegistry.getCard(it.cardDefinitionId)?.metadata?.imageUri }
            ManaSourceInfo(
                entityId = source.entityId,
                name = source.name,
                imageUri = imageUri,
                producesColors = source.producesColors.map { it.symbol.toString() },
                producesColorless = source.producesColorless,
                manaAmount = source.manaAmount
            )
        }
    }

    // Extension functions for DTO conversion

    private fun TargetInfo.toDto() = LegalActionTargetInfo(
        index = index,
        description = description,
        minTargets = minTargets,
        maxTargets = maxTargets,
        validTargets = validTargets,
        targetZone = targetZone,
        xConstrainsManaValue = xConstrainsManaValue,
        xConstrainsManaValueExactly = xConstrainsManaValueExactly,
        xConstrainsPower = xConstrainsPower,
        xConstrainsCount = xConstrainsCount
    )

    private fun AdditionalCostData.toDto() = AdditionalCostInfo(
        description = description,
        costType = costType,
        validSacrificeTargets = validSacrificeTargets,
        sacrificeCount = sacrificeCount,
        costAfterSacrifice = costAfterSacrifice,
        validTapTargets = validTapTargets,
        tapCount = tapCount,
        tapBatchMaxActivations = tapBatchMaxActivations,
        validDiscardTargets = validDiscardTargets,
        discardCount = discardCount,
        validBounceTargets = validBounceTargets,
        bounceCount = bounceCount,
        validExileTargets = validExileTargets,
        exileMinCount = exileMinCount,
        exileMaxCount = exileMaxCount,
        exileMinTotalWeight = exileMinTotalWeight,
        exileCardWeights = exileCardWeights,
        exileWeightUnit = exileWeightUnit,
        exileWeightPerTarget = exileWeightPerTarget,
        validBeholdTargets = validBeholdTargets,
        beholdCount = beholdCount,
        counterRemovalCreatures = counterRemovalCreatures.map { it.toDto() },
        validBlightTargets = validBlightTargets,
        blightAmount = blightAmount,
        blightVariableMaxX = blightVariableMaxX,
        payXLifeMaxX = payXLifeMaxX,
        distributedCounterRemovalTotal = distributedCounterRemovalTotal,
        validCraftMaterials = validCraftMaterials,
        craftMinCount = craftMinCount,
        craftMaxCount = craftMaxCount,
        tapForPowerCreatures = tapForPowerCreatures.map { it.toDto() },
        tapForPowerRequired = tapForPowerRequired
    )

    private fun ConvokeCreatureData.toDto() = ConvokeCreatureInfo(
        entityId = entityId,
        name = name,
        colors = colors
    )

    private fun TapForGenericPermanentData.toDto() = TapForGenericPermanentInfo(
        entityId = entityId,
        name = name,
        isCreature = isCreature
    )

    private fun DelveCardData.toDto() = DelveCardInfo(
        entityId = entityId,
        name = name,
        imageUri = imageUri
    )

    private fun HarmonizeCreatureData.toDto() = HarmonizeCreatureInfo(
        entityId = entityId,
        name = name,
        power = power
    )

    private fun TapForPowerCreatureData.toDto() = TapForPowerCreatureInfo(
        entityId = entityId,
        name = name,
        power = power,
        canAttack = canAttack
    )

    private fun CounterRemovalCreatureData.toDto() = CounterRemovalCreatureInfo(
        entityId = entityId,
        name = name,
        availableCounters = availableCounters,
        availableCountersByType = availableCountersByType,
        imageUri = imageUri
    )
}
