package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.enumerators.*
import com.wingedsheep.engine.mechanics.SplitSecond
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.forcedPlayFor
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Coordinator that enumerates all legal actions for a player.
 *
 * This is the engine-level equivalent of the game-server's LegalActionsCalculator.
 * It delegates to specialized ActionEnumerators for each action category, mirrors
 * the ActionProcessor/ActionHandlerRegistry pattern for enumeration.
 */
class LegalActionEnumerator(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator,
    private val turnManager: TurnManager
) {
    private val combatEnumerator = CombatEnumerator()

    private val enumerators: List<ActionEnumerator> = listOf(
        PassPriorityEnumerator(),
        PlayerActionEnumerator(),
        PlayLandEnumerator(),
        MorphCastEnumerator(),
        CastSpellEnumerator(predicateEvaluator = predicateEvaluator),
        SneakCastEnumerator(),
        EmergeCastEnumerator(),
        AnnouncedCharacteristicsCastEnumerator(CharacteristicsAnnouncement.BESTOW),
        AnnouncedCharacteristicsCastEnumerator(CharacteristicsAnnouncement.PROTOTYPE),
        WebSlingingCastEnumerator(),
        CyclingEnumerator(),
        PlotEnumerator(),
        ForetellEnumerator(),
        SuspendEnumerator(),
        CastFromZoneEnumerator(predicateEvaluator = predicateEvaluator),
        ManaAbilityEnumerator(predicateEvaluator = predicateEvaluator),
        TurnFaceUpEnumerator(predicateEvaluator = predicateEvaluator),
        UnlockRoomDoorEnumerator(),
        ActivatedAbilityEnumerator(predicateEvaluator = predicateEvaluator),
        CrewEnumerator(),
        SaddleEnumerator(),
        ZoneActivatedAbilityEnumerator(Zone.GRAVEYARD, predicateEvaluator = predicateEvaluator),
        ZoneActivatedAbilityEnumerator(Zone.HAND, predicateEvaluator = predicateEvaluator),
        CommandZoneAbilityEnumerator()
    )

    /**
     * Enumerate all legal actions for the given player in the given state.
     *
     * @param state The current game state
     * @param playerId The player to enumerate actions for
     * @param mode Controls what data is computed. [EnumerationMode.ACTIONS_ONLY] skips
     *   auto-tap preview computation for simulation/MCTS use.
     * @return All legal actions (including unaffordable ones marked with affordable=false)
     */
    fun enumerate(
        state: GameState,
        playerId: EntityId,
        mode: EnumerationMode = EnumerationMode.FULL
    ): List<LegalAction> {
        if (state.continuationStack.any { it is com.wingedsheep.engine.core.FinishForcedPlayContinuation } &&
            state.pendingDecision != null && (state.pendingDecision !is com.wingedsheep.engine.core.PlayCardDecision ||
                state.pendingDecision?.playerId != playerId)) return emptyList()
        val context = EnumerationContext(
            state = state,
            playerId = playerId,
            cardRegistry = cardRegistry,
            manaSolver = manaSolver,
            costCalculator = costCalculator,
            predicateEvaluator = predicateEvaluator,
            conditionEvaluator = conditionEvaluator,
            turnManager = turnManager,
            mode = mode
        )

        val forced = state.forcedPlayFor(playerId)
        // A resolving instruction can play its card during a combat declaration step.
        if (forced == null && combatEnumerator.isCombatDeclarationStep(context)) {
            return combatEnumerator.enumerate(context)
        }

        // Normal priority: enumerate all action categories
        val offers = (enumerators.flatMap { it.enumerate(context) } + optionalPaymentOffers(context)).filter { offer ->
            val cast = offer.action as? com.wingedsheep.engine.core.CastSpell
            if (cast != null && context.castPermissionUtils.blockedByResolvedCastRestriction(state, cast)) {
                return@filter false
            }
            forced == null || when (val action = offer.action) {
                is com.wingedsheep.engine.core.CastSpell -> action.cardId == forced.card.entityId
                is com.wingedsheep.engine.core.PlayLand -> action.cardId == forced.card.entityId
                else -> false
            }
        }
        // Split second (CR 702.61): while a spell with it is on the stack, withhold every spell and
        // non-mana activated ability — the same verdict ActionProcessor.validate reaches.
        val permitted = if (SplitSecond.isLocked(state, cardRegistry)) {
            offers.filterNot { SplitSecond.forbids(it.action, it.isManaAbility) }
        } else {
            offers
        }
        return com.wingedsheep.engine.legalactions.enumerators.AdditionalManaForCountersOffer
            .annotate(context, permitted.map(::capXAtExactTargetCount), predicateEvaluator = predicateEvaluator)
    }

    /** Reuse every casting rail under an immutable declared-cost pricing context. */
    private fun optionalPaymentOffers(context: EnumerationContext): List<LegalAction> {
        val payments = costCalculator.optionalPayments(context.state, context.playerId)
        if (payments.isEmpty()) return emptyList()
        val groups = payments.groupBy { costCalculator.optionalModifier(context.state, it) }.values
        // Identical instances have identical outcomes; offer each count, not every permutation.
        var combinations = listOf(emptyList<com.wingedsheep.engine.core.CostModifierPayment>())
        for (group in groups) combinations = combinations.flatMap { selected ->
            (0..group.size).map { selected + group.take(it) }.filter { chosen ->
                context.state.canPayLife(context.playerId, chosen.sumOf {
                    costCalculator.optionalModifier(context.state, it)?.optionalLifePayment ?: 0
                })
            }
        }
        return combinations.filter { it.isNotEmpty() }.flatMap { chosen ->
            val life = chosen.sumOf { costCalculator.optionalModifier(context.state, it)?.optionalLifePayment ?: 0 }
            val pricing = costCalculator.withOptionalPayments(chosen)
            val variant = EnumerationContext(context.state, context.playerId, cardRegistry,
                manaSolver.reservingLife(life), pricing, predicateEvaluator, conditionEvaluator, turnManager, context.mode)
            enumerators.asSequence().filter {
                it is CastSpellEnumerator || it is CastFromZoneEnumerator || it is SneakCastEnumerator ||
                    it is EmergeCastEnumerator || it is AnnouncedCharacteristicsCastEnumerator || it is WebSlingingCastEnumerator
            }.flatMap { it.enumerate(variant) }.mapNotNull { offer ->
                val action = offer.action as? com.wingedsheep.engine.core.CastSpell ?: return@mapNotNull null
                if (action.castFaceDown) return@mapNotNull null
                val component = context.state.getEntity(action.cardId)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>() ?: return@mapNotNull null
                val printed = cardRegistry.getCard(component.cardDefinitionId) ?: return@mapNotNull null
                val spell = pricing.optionalPaymentDefinition(context.state, action, printed)
                val zone = context.state.logicalZone(action.cardId)?.zoneType
                if (chosen.any { !pricing.optionalPaymentApplies(context.state, it, spell, context.playerId, zone,
                    declaredCostSlot = action.declaredCostSlot) }) return@mapNotNull null
                offer.copy(action = action.copy(optionalCostPayments = chosen), description = "${offer.description} — pay $life life")
            }.toList()
        }
    }

    /**
     * "X target creatures" needs X distinct legal targets (CR 601.2c, 115.3), so an X larger than
     * the legal targets can't be cast: cap [LegalAction.maxAffordableX] at the smallest such
     * requirement's legal-target count, and the client's X picker never offers an X the cast would
     * then be rejected for. The enumerated target list is permissive where an X-relative filter
     * narrows it later, so this is an upper bound — the validator still has the last word.
     */
    private fun capXAtExactTargetCount(action: LegalAction): LegalAction {
        val maxX = action.maxAffordableX ?: return action
        val requirementCaps = action.targetRequirements.orEmpty()
            .filter { it.xConstrainsCountExactly }
            .map { it.validTargets.size }
        val flatCap = if (action.xConstrainsTargetCountExactly) action.validTargets?.size else null
        val cap = (requirementCaps + listOfNotNull(flatCap)).minOrNull() ?: return action
        return if (cap < maxX) action.copy(maxAffordableX = cap) else action
    }

    /**
     * Enumerate only [playerId]'s mana abilities, ignoring priority.
     *
     * This is the CR 605.3a surface: while a rule or effect is asking a player for a mana payment
     * they hold no priority, but they may still activate mana abilities to produce the mana. See
     * [com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow].
     */
    fun enumerateManaAbilities(
        state: GameState,
        playerId: EntityId,
        mode: EnumerationMode = EnumerationMode.FULL
    ): List<LegalAction> {
        val context = EnumerationContext(
            state, playerId, cardRegistry, manaSolver, costCalculator,
            predicateEvaluator, conditionEvaluator, turnManager, mode,
        )
        return ManaAbilityEnumerator(predicateEvaluator = predicateEvaluator).enumerate(context) +
            PlayerActionEnumerator(manaOnly = true).enumerate(context)
    }

    companion object {
        /**
         * A standalone enumerator for a caller that holds only a [CardRegistry], backed by an engine
         * graph of its own. A caller that already has an [EngineServices] uses its
         * [EngineServices.legalActionEnumerator] instead, so the enumerator shares that engine's
         * turn manager and evaluators.
         */
        fun create(cardRegistry: CardRegistry): LegalActionEnumerator =
            EngineServices(cardRegistry).legalActionEnumerator
    }
}
