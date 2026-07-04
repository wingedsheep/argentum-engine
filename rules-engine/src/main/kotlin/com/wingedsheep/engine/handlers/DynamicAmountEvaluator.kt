package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.handlers.effects.LkiPolicy
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.lkiPolicyFor
import com.wingedsheep.engine.handlers.effects.lkiSnapshotFor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.blightAmountChoice
import com.wingedsheep.engine.state.components.battlefield.numberChoice
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsStationUsingToughnessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.AttachmentKind
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.TurnTracker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.engine.handlers.effects.permanent.counters.counterTypeToString
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.math.max
import kotlin.math.min

private val BASIC_LAND_SUBTYPES: Set<String> = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

/**
 * The names of every true card type (CR 205.2a). Used to count "card types" via
 * [Aggregation.DISTINCT_TYPES] without including supertypes/subtypes, which
 * [ProjectedState.getTypes] folds into the same set.
 */
private val CARD_TYPE_NAMES: Set<String> =
    com.wingedsheep.sdk.core.CardType.entries.mapTo(mutableSetOf()) { it.name }

/**
 * Every official creature type (CR 205.3m), proper-cased to match the subtype strings carried in
 * [ProjectedState.getSubtypes]. Used by [DynamicAmount.LargestSharedCreatureTypeCount] to keep
 * non-creature subtypes (Equipment, Vehicle, basic land types, …) from polluting the tribe tally.
 */
private val CREATURE_TYPE_NAMES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()

/**
 * Evaluates DynamicAmount values against the current game state.
 *
 * DynamicAmount represents values that depend on game state, like
 * "the number of creatures you control" or "your life total".
 */
class DynamicAmountEvaluator(
    private val conditionEvaluator: ConditionEvaluator? = null,
    /**
     * Projection used for battlefield reads when the caller doesn't pass one. Default reads
     * the canonical [GameState.projectedState]; the [com.wingedsheep.engine.mechanics.layers.StateProjector]
     * swaps in an empty projection so its internal evaluator never re-enters its own lazy
     * initializer. Only invoked from branches that actually need projection — never eagerly
     * at function entry, otherwise resolution-time `Compare` conditions evaluated mid-layer
     * would recurse.
     */
    private val defaultProjection: (GameState) -> ProjectedState = { it.projectedState }
) {

    /**
     * Resolve the projection for branches that need battlefield reads. Lazy by design:
     * called only inside aggregate / entity-property branches, never at evaluator entry.
     */
    private fun resolveProjection(state: GameState, explicit: ProjectedState?): ProjectedState =
        explicit ?: defaultProjection(state)

    /**
     * Evaluate a DynamicAmount to get an actual integer value.
     *
     * @param projectedState Optional pre-computed projected state for battlefield reads.
     *   When provided, takes priority over [defaultProjection]. Mid-projection callers
     *   ([com.wingedsheep.engine.mechanics.layers.EffectApplicator]) pass their intermediate
     *   snapshot so layer-by-layer changes are visible to nested aggregates.
     */
    fun evaluate(
        state: GameState,
        amount: DynamicAmount,
        context: EffectContext,
        projectedState: ProjectedState? = null
    ): Int {
        return when (amount) {
            is DynamicAmount.Fixed -> amount.amount

            is DynamicAmount.XValue -> context.xValue ?: 0

            // Counters the source had the moment its self-exile / self-sacrifice cost wiped them
            // (CR 112.7a). Snapshotted into the resolution context at cost-payment time so the
            // resolving effect reads the pre-cost count rather than zero (Lost Isle Calling).
            is DynamicAmount.LastKnownSourceCounters -> {
                val snapshot = context.lastKnownSourceCounters
                when (val filter = amount.counterType) {
                    is CounterTypeFilter.Any -> snapshot.values.sum()
                    else -> snapshot[counterTypeToString(resolveCounterType(filter))] ?: 0
                }
            }

            // The {X} this object was cast with, read off the current object regardless of zone.
            // Reads, in order: the durable CastChoicesComponent on the battlefield permanent (and
            // for a later activated ability); the SpellOnStackComponent while the object is still
            // on the stack (its own resolution and a "when you cast this spell" trigger, since the
            // trigger source is the spell entity); then the resolution context's xValue, which
            // carries the cast X as last-known information into dies/leaves triggers (from the
            // leave ZoneChangeEvent).
            is DynamicAmount.CastX -> {
                val source = context.sourceId?.let { state.getEntity(it) }
                source?.get<CastChoicesComponent>()?.x
                    ?: source?.get<SpellOnStackComponent>()?.xValue
                    ?: context.xValue
                    ?: 0
            }

            // A numeric value locked in for a ChoiceSlot, read off the durable cast-choices bag on
            // the source, falling back to the resolution context so an instant/sorcery that never
            // becomes a permanent still resolves it from what was paid at cast.
            is DynamicAmount.CastChoice -> {
                val source = context.sourceId?.let { state.getEntity(it) }
                when (amount.slot) {
                    com.wingedsheep.sdk.scripting.ChoiceSlot.BLIGHT_AMOUNT ->
                        source?.blightAmountChoice() ?: context.additionalCostBlightAmount
                    // Any other numeric slot (e.g. CHOSEN_NUMBER for Shapeshifter) is read
                    // generically off the durable cast-choices bag as a NumberChoice.
                    else -> source?.numberChoice(amount.slot) ?: 0
                }
            }

            is DynamicAmount.TotalManaSpent -> context.totalManaSpent

            // Distinct colors of mana spent to cast the source (Converge / Sunburst). Read off
            // the source entity's recorded payment via the shared reader so it resolves at ETB
            // (EntersWithDynamicCounters) as well as mid-resolution; falls through to the context
            // total being irrelevant — color breakdown only lives on the entity's components.
            is DynamicAmount.DistinctColorsManaSpent ->
                context.sourceId?.let { ManaSpentReader.distinctColorsSpent(state, it) } ?: 0

            is DynamicAmount.ManaSpentOnX -> context.manaSpentOnXByColor[amount.color] ?: 0

            is DynamicAmount.YourLifeTotal -> {
                // CR 810.9a — an individual player's life total reads the team's shared total.
                state.lifeTotal(context.controllerId)
            }

            is DynamicAmount.LifeTotal -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.lifeTotal(playerId)
            }

            is DynamicAmount.StartingLifeTotal -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.getEntity(playerId)?.get<PlayerComponent>()?.startingLifeTotal ?: 20
            }

            // Total unspent mana in the player's pool (Ozai, the Phoenix King's "six or more
            // unspent mana"). Reads the base-state ManaPoolComponent.total, which is unaffected by
            // continuous projection.
            is DynamicAmount.UnspentMana -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val playerId = playerIds.firstOrNull() ?: return 0
                state.getEntity(playerId)?.get<ManaPoolComponent>()?.total ?: 0
            }

            // Unlocked doors among Rooms the player controls (CR 709.5). Reads per-face door
            // state off each Room's RoomComponent — a single Room entity can contribute two
            // doors, so this can't go through the entity-level AggregateBattlefield. Controller
            // is read from projection so control-changing effects move a Room's doors with it.
            is DynamicAmount.UnlockedDoors -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context).toSet()
                val projection = resolveProjection(state, projectedState)
                val rooms = state.getBattlefield().mapNotNull { entityId ->
                    val room = state.getEntity(entityId)?.get<RoomComponent>() ?: return@mapNotNull null
                    if (controllerOf(state, projection, entityId) in playerIds) room else null
                }
                if (amount.distinctNames) {
                    rooms.flatMapTo(mutableSetOf()) { room ->
                        room.faces.filter { it.id in room.unlocked }.map { it.name }
                    }.size
                } else {
                    rooms.sumOf { it.unlocked.size }
                }
            }

            is DynamicAmount.VariableReference -> {
                val name = amount.variableName
                if (name.endsWith("_count")) {
                    val collectionName = name.removeSuffix("_count")
                    context.pipeline.storedCollections[collectionName]?.size ?: 0
                } else {
                    context.pipeline.storedNumbers[name] ?: 0
                }
            }

            is DynamicAmount.StoredCardManaValue -> {
                val cards = context.pipeline.storedCollections[amount.collectionName] ?: return 0
                val cardId = cards.firstOrNull() ?: return 0
                state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0
            }

            is DynamicAmount.DistinctEntitiesInCollections -> {
                amount.collections
                    .flatMap { context.pipeline.storedCollections[it].orEmpty() }
                    .distinct()
                    .size
            }

            is DynamicAmount.DistinctCardTypesInCollections -> {
                // Union the card types across every card in the named collections (read by entity
                // id, so still correct after the cards moved to a graveyard). Mirrors the
                // LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT counting logic.
                val cardTypes = mutableSetOf<com.wingedsheep.sdk.core.CardType>()
                for (collectionName in amount.collections) {
                    for (cardId in context.pipeline.storedCollections[collectionName].orEmpty()) {
                        val card = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                        cardTypes.addAll(card.typeLine.cardTypes)
                    }
                }
                cardTypes.size
            }

            is DynamicAmount.ManaValueSumOfCollection -> {
                val cards = context.pipeline.storedCollections[amount.collectionName] ?: return 0
                cards.sumOf { cardId ->
                    state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0
                }
            }

            // Math operations — propagate [projectedState] so a mid-projection caller's
            // intermediate snapshot survives nested aggregates. Arithmetic is saturating
            // (GameLimits.*Clamped): a "twice the number of X" / doubling chain must clamp at
            // MAX_QUANTITY, never silently overflow `Int` to a negative value.
            is DynamicAmount.Add ->
                GameLimits.addClamped(
                    evaluate(state, amount.left, context, projectedState),
                    evaluate(state, amount.right, context, projectedState)
                )

            is DynamicAmount.Subtract ->
                GameLimits.subClamped(
                    evaluate(state, amount.left, context, projectedState),
                    evaluate(state, amount.right, context, projectedState)
                )

            is DynamicAmount.Multiply ->
                GameLimits.mulClamped(
                    evaluate(state, amount.amount, context, projectedState),
                    amount.multiplier
                )

            is DynamicAmount.Power ->
                GameLimits.powClamped(
                    amount.base,
                    evaluate(state, amount.exponent, context, projectedState)
                )

            is DynamicAmount.IfPositive ->
                max(0, evaluate(state, amount.amount, context, projectedState))

            is DynamicAmount.Max ->
                max(evaluate(state, amount.left, context, projectedState), evaluate(state, amount.right, context, projectedState))

            is DynamicAmount.Min ->
                min(evaluate(state, amount.left, context, projectedState), evaluate(state, amount.right, context, projectedState))

            is DynamicAmount.ContextProperty -> evaluateContextProperty(state, amount.key, context)

            // Battlefield / zone aggregates — propagate the explicit snapshot (may be null);
            // each helper decides whether it actually needs to reach for [defaultProjection].
            // Doing this eagerly here would touch [state.projectedState] for non-battlefield
            // zones (e.g., GRAVEYARD `Count`), which infinitely recurses if a mid-projection
            // caller (a `ConditionalStaticAbility` condition evaluated inside [EffectApplicator])
            // reaches this branch through a default-constructed [ConditionEvaluator].
            is DynamicAmount.Count ->
                evaluateUnifiedCount(state, amount.player, amount.zone, amount.filter, context, projectedState)

            is DynamicAmount.AggregateBattlefield ->
                evaluateBattlefieldAggregate(state, amount, context, projectedState)

            is DynamicAmount.AggregateZone ->
                evaluateZoneAggregate(state, amount, context, projectedState)

            // Devotion (CR 700.5): the number of mana symbols of the named colors among the mana
            // costs of permanents the player controls. Hybrid ({W/U}), monocolored hybrid ({2/B}),
            // and Phyrexian ({B/P}) symbols each count toward their color(s); a symbol matching more
            // than one of the requested colors is still counted once. Controller is read from
            // projection so control-changing effects are honored (700.5a). Face-down permanents have
            // no mana cost (CR 711.4) and contribute nothing.
            is DynamicAmount.DevotionTo -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context).toSet()
                if (playerIds.isEmpty()) return 0
                val projection = resolveProjection(state, projectedState)
                val wanted = amount.colors.toSet()
                state.getBattlefield().sumOf { entityId ->
                    if (controllerOf(state, projection, entityId) !in playerIds) return@sumOf 0
                    val entity = state.getEntity(entityId) ?: return@sumOf 0
                    if (entity.has<FaceDownComponent>()) return@sumOf 0
                    val cost = entity.get<CardComponent>()?.manaCost ?: return@sumOf 0
                    cost.symbols.count { symbol -> manaSymbolColors(symbol).any { it in wanted } }
                }
            }

            is DynamicAmount.Conditional -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val met = eval.evaluate(state, amount.condition, context)
                if (met) evaluate(state, amount.ifTrue, context, projectedState)
                else evaluate(state, amount.ifFalse, context, projectedState)
            }

            is DynamicAmount.CountPlayersWith -> {
                val eval = conditionEvaluator ?: ConditionEvaluator()
                val playerIds = resolveUnifiedPlayerIds(state, amount.scope, context)
                playerIds.count { playerId ->
                    eval.evaluate(state, amount.condition, context.copy(controllerId = playerId))
                }
            }

            // Composable entity property — replaces SourcePower, TargetPower, CountersOnSelf, etc.
            is DynamicAmount.EntityProperty -> {
                val entityId = TargetResolutionUtils.resolveEntityReference(amount.entity, context, state)
                // Enchanted-creature power reads use last-known information when the source aura
                // has detached: the enchanted creature (and the aura) can leave the battlefield
                // before the ability resolves — e.g. removed in response to the aura's ETB
                // trigger — and "deals damage equal to its power" must use the power as it last
                // existed on the battlefield (CR 608.2h). Captured at trigger time.
                if (amount.entity is EntityReference.EnchantedCreature &&
                    amount.numericProperty is EntityNumericProperty.Power &&
                    (entityId == null || entityId !in state.getBattlefield())
                ) {
                    return context.enchantedCreatureLastKnownPower ?: 0
                }
                if (entityId == null) return 0
                // Last-known-information fallback (CR 112.7a / 603.10 / 608.2h): one rule for every
                // reference that reads a permanent after it has left the battlefield — a
                // self-sacrificing source, or a sacrificed / tapped / chosen cost permanent. When
                // such a reference resolves off the battlefield, read its captured snapshot's P/T
                // before falling through to base characteristics. [LkiPolicy.LIVE_ONLY] references
                // (targets, iteration, …) skip this and read the live board. Off the battlefield the
                // projected P/T is null, so the final resolveNumericProperty yields base
                // characteristics anyway — this replaces the former per-reference
                // `useProjected = false` branches (Ghitu Fire-Eater, Heart-Piercer Manticore, …).
                if (lkiPolicyFor(amount.entity) == LkiPolicy.LIVE_THEN_LKI &&
                    entityId !in state.getBattlefield()
                ) {
                    val snapshot = context.lkiSnapshotFor(amount.entity, entityId)
                    when (amount.numericProperty) {
                        is EntityNumericProperty.Power -> snapshot?.power?.let { return it }
                        is EntityNumericProperty.Toughness -> snapshot?.toughness?.let { return it }
                        else -> { /* fall through to base characteristics */ }
                    }
                }
                resolveNumericProperty(state, entityId, amount.numericProperty, context, useProjected = true, explicitProjected = projectedState)
            }

            // Station charge amount (CR 702.184a): charge counters equal to the power of the
            // creature tapped to pay the station cost. CR 702.184c lets a static ability change
            // which characteristic is counted; Tapestry Warden's [GrantsStationUsingToughnessComponent]
            // substitutes toughness when toughness > power. Reads with last-known information if the
            // tapped creature has left the battlefield (CR 112.7a). Keeping this on its own node
            // confines the substitution to station abilities.
            is DynamicAmount.StationCharge -> {
                val entityId = context.tappedPermanents.firstOrNull() ?: return 0
                val snapshot = context.tappedEntitySnapshots.firstOrNull { it.entityId == entityId }
                val useSnapshot = snapshot != null && entityId !in state.getBattlefield()
                val power = if (useSnapshot) snapshot.power ?: 0
                    else resolveNumericProperty(state, entityId, EntityNumericProperty.Power, context, useProjected = true, explicitProjected = projectedState)
                val toughness = if (useSnapshot) snapshot.toughness ?: 0
                    else resolveNumericProperty(state, entityId, EntityNumericProperty.Toughness, context, useProjected = true, explicitProjected = projectedState)
                if (toughness > power &&
                    controllerHasStationUsingToughness(state, entityId, snapshot?.controllerId)
                ) toughness else power
            }

            is DynamicAmount.Divide -> {
                val num = evaluate(state, amount.numerator, context, projectedState)
                val den = evaluate(state, amount.denominator, context, projectedState)
                if (den == 0) return 0
                if (amount.roundUp) {
                    (num + den - 1) / den
                } else {
                    num / den
                }
            }

            is DynamicAmount.TurnTracking -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                when (amount.tracker) {
                    TurnTracker.CREATURES_DIED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.NONTOKEN_CREATURES_DIED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.OPPONENT_CREATURES_EXILED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.OPPONENTS_WHO_LOST_LIFE -> {
                        val controllerId = context.controllerId
                        state.turnOrder
                            .filter { it != controllerId }
                            .count { opponentId ->
                                state.getEntity(opponentId)
                                    ?.get<com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent>() != null
                            }
                    }
                    TurnTracker.DAMAGE_RECEIVED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent>()
                            ?.amount ?: 0
                    }
                    TurnTracker.DAMAGE_RECEIVED_FROM_ARTIFACTS -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.DamageReceivedFromArtifactsThisTurnComponent>()
                            ?.amount ?: 0
                    }
                    TurnTracker.LIFE_GAINED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent>()
                            ?.amount ?: 0
                    }
                    TurnTracker.LIFE_LOST -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent>() == true
                    }
                    TurnTracker.PLAYER_ATTACKED -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent>() == true
                    }
                    TurnTracker.DEALT_COMBAT_DAMAGE -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent>() == true
                    }
                    TurnTracker.DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.WasDealtCombatDamageByLegendaryCreatureThisTurnComponent>() == true
                    }
                    TurnTracker.COUNTERS_PUT_ON_CREATURE -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent>() == true
                    }
                    TurnTracker.LANDS_PLAYED -> playerIds.sumOf { playerId ->
                        val landDrops = state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LandDropsComponent>()
                            ?: return@sumOf 0
                        landDrops.maxPerTurn - landDrops.remaining
                    }
                    TurnTracker.LANDS_ENTERED_UNDER_CONTROL -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.LandsEnteredUnderControlThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.FOOD_SACRIFICED -> playerIds.count { playerId ->
                        state.getEntity(playerId)
                            ?.has<com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent>() == true
                    }
                    TurnTracker.CARDS_LEFT_GRAVEYARD -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.DESCENDED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.CARDS_DRAWN -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.CARDS_PUT_INTO_EXILE -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.CardsPutIntoExileThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.PERMANENTS_SACRIFICED -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.PermanentsSacrificedThisTurnComponent>()
                            ?.count ?: 0
                    }
                    TurnTracker.DISTINCT_BENDS -> playerIds.sumOf { playerId ->
                        state.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.player.BendsThisTurnComponent>()
                            ?.types?.size ?: 0
                    }
                }
            }

            is DynamicAmount.SpellsCastThisTurn -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                // excludeSelf drops the resolving spell's own record, matched by the spell's
                // stack entity id (CastSpellRecord.sourceEntityId == context.sourceId).
                val selfId = if (amount.excludeSelf) context.sourceId else null
                fun matches(record: com.wingedsheep.engine.state.CastSpellRecord) =
                    (selfId == null || record.sourceEntityId != selfId) &&
                        // Zone qualifier is checked independently of the filter (see condition note).
                        (amount.fromZone == null || record.castFromZone == amount.fromZone) &&
                        predicateEvaluator.matchesFilter(record, amount.filter)
                if (amount.countDistinctCardTypes) {
                    // "for each card type among spells you've cast this turn" — union the card types
                    // across every matching record (an artifact creature spell counts for both).
                    playerIds
                        .flatMap { state.spellsCastThisTurnByPlayer[it] ?: emptyList() }
                        .filter { matches(it) }
                        .flatMap { it.typeLine.cardTypes }
                        .toSet()
                        .size
                } else {
                    playerIds.sumOf { playerId ->
                        (state.spellsCastThisTurnByPlayer[playerId] ?: emptyList()).count { matches(it) }
                    }
                }
            }

            is DynamicAmount.CraftedMaterialsTotalPower -> {
                val sourceId = context.sourceId
                if (sourceId == null) 0 else {
                    val materials = state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent>()
                    if (materials == null) 0 else {
                        var total = 0
                        for (exiledId in materials.exiledIds) {
                            total += basePowerOfPrintedCard(state, exiledId)
                        }
                        total
                    }
                }
            }

            is DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn -> {
                val sourceId = context.sourceId
                if (sourceId == null) 0 else {
                    state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent>()
                        ?.creatureIds?.size ?: 0
                }
            }

            is DynamicAmount.SubtypeEnteredUnderControlThisTurn -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
                val wanted = amount.subtype.value
                val excludeId = if (amount.excludeTriggeringEntity) context.triggeringEntityId else null
                playerIds.sumOf { playerId ->
                    val entries = state.getEntity(playerId)
                        ?.get<com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent>()
                        ?.entries
                        ?: emptyList()
                    entries.count { rec ->
                        rec.entityId != excludeId &&
                            rec.subtypes.any { it.equals(wanted, ignoreCase = true) }
                    }
                }
            }

            is DynamicAmount.PermanentsSacrificedThisWay -> context.sacrificedPermanents.size

            // "The greatest number of creatures you control that have a creature type in common"
            // (White Lotus Tile). For every creature type present among the player's creatures,
            // tally how many of those creatures have it, then take the max. A creature with several
            // creature types feeds each of its tribes (a Bird Soldier adds to both the Bird and the
            // Soldier tally); a Changeling — projected to ALL_CREATURE_TYPES (StateProjector) — feeds
            // every tribe. Reads projected subtypes so type-changing effects and Changeling are
            // honored (CLAUDE.md battlefield-projection rule), restricting to actual creature types so
            // artifact/land subtypes can't inflate the count. Zero when no creature shares a type.
            is DynamicAmount.LargestSharedCreatureTypeCount -> {
                val playerIds = resolveUnifiedPlayerIds(state, amount.player, context).toSet()
                if (playerIds.isEmpty()) return 0
                val projection = resolveProjection(state, projectedState)
                val tally = HashMap<String, Int>()
                for (entityId in state.getBattlefield()) {
                    if (controllerOf(state, projection, entityId) !in playerIds) continue
                    if (!projection.isCreature(entityId)) continue
                    val subtypes = projection.getSubtypes(entityId).ifEmpty {
                        state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes
                            ?.map { it.value }?.toSet() ?: emptySet()
                    }
                    for (subtype in subtypes) {
                        if (subtype in CREATURE_TYPE_NAMES) {
                            tally[subtype] = (tally[subtype] ?: 0) + 1
                        }
                    }
                }
                tally.values.maxOrNull() ?: 0
            }

        }
    }

    // =========================================================================
    // Context Property Evaluation
    // =========================================================================

    /**
     * Resolve a [ContextPropertyKey] against the current resolution [context].
     *
     * The trigger amount keys (damage / life-gained / life-lost) all read the same
     * `triggerDamageAmount` field — `LifeChangedEvent` populates it with the absolute
     * amount of life moved, regardless of direction.
     */
    private fun evaluateContextProperty(
        state: GameState,
        key: ContextPropertyKey,
        context: EffectContext
    ): Int = when (key) {
        ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT,
        ContextPropertyKey.PREVENTED_DAMAGE_AMOUNT,
        ContextPropertyKey.TRIGGER_LIFE_GAINED,
        ContextPropertyKey.TRIGGER_LIFE_LOST -> context.triggerDamageAmount ?: 0

        ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT,
        ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT -> context.triggerCounterCount ?: 0
        ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT -> context.triggerTotalCounterCount ?: 0

        ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT -> context.exiledCardCount

        ContextPropertyKey.TARGET_COUNT -> context.targets.size

        ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL -> context.triggerModesChosenCount ?: 0

        ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL -> context.triggerManaSpentOnTriggeringSpell ?: 0

        ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL -> context.triggerColorsSpentOnTriggeringSpell ?: 0

        ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE -> context.triggerManaValueOfTriggeringSpell ?: 0

        ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL -> context.triggerXValueOfTriggeringSpell ?: 0

        ContextPropertyKey.TRIGGER_SCRY_COUNT -> context.triggerScryCount ?: 0

        ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT -> context.triggerExcessDamageAmount ?: 0

        ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS -> context.triggerRecipientToughness ?: 0

        ContextPropertyKey.LINKED_EXILE_CARD_COUNT -> {
            val sourceId = context.sourceId
            if (sourceId == null) 0 else state.getEntity(sourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                ?.exiledIds?.size ?: 0
        }

        ContextPropertyKey.LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT -> {
            val sourceId = context.sourceId
            if (sourceId == null) 0 else {
                val linkedExile = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                if (linkedExile == null) 0 else {
                    val cardTypes = mutableSetOf<com.wingedsheep.sdk.core.CardType>()
                    for (exiledId in linkedExile.exiledIds) {
                        val card = state.getEntity(exiledId)?.get<CardComponent>() ?: continue
                        cardTypes.addAll(card.typeLine.cardTypes)
                    }
                    cardTypes.size
                }
            }
        }
    }

    // =========================================================================
    // Unified Filter Evaluation
    // =========================================================================

    private val predicateEvaluator = PredicateEvaluator()

    private fun controllerOf(state: GameState, projection: ProjectedState, entityId: EntityId): EntityId? =
        projection.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

    private fun evaluateUnifiedCount(
        state: GameState,
        player: Player,
        zone: Zone,
        filter: GameObjectFilter,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, player, context)
        val zoneType = resolveUnifiedZone(zone)
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Projection only matters for battlefield reads (type/subtype/color/keyword/P/T mutated
        // by continuous effects). Non-battlefield zones use base CardComponent via an empty
        // projection — and crucially do *not* reach for [defaultProjection], which during a
        // mid-projection caller would re-enter the lazy `state.projectedState` initializer.
        val projection: ProjectedState = if (zoneType == Zone.BATTLEFIELD) {
            resolveProjection(state, explicitProjection)
        } else {
            explicitProjection ?: ProjectedState(state, emptyMap())
        }

        return playerIds.sumOf { playerId ->
            val entities = if (zoneType == Zone.BATTLEFIELD) {
                state.getBattlefield().filter { controllerOf(state, projection, it) == playerId }
            } else {
                state.getZone(ZoneKey(playerId, zoneType))
            }
            entities.count { predicateEvaluator.matches(state, projection, it, filter, predicateContext) }
        }
    }

    /**
     * Evaluate an AggregateBattlefield: collect → filter → map → aggregate.
     */
    private fun evaluateBattlefieldAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateBattlefield,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projection = resolveProjection(state, explicitProjection)

        // "Self" is the affected entity when this aggregate is evaluated for a granted effect
        // (e.g. an Aura's "for each OTHER creature that shares a type with the enchanted creature":
        // self is the enchanted creature, not the Aura source). For a creature's own CDA there is
        // no affected entity, so it falls back to the source — the creature itself.
        val selfId = context.affectedEntityId ?: context.sourceId

        val matchingEntities = playerIds.flatMap { playerId ->
            state.getBattlefield()
                .filter { entityId ->
                    // Exclude self if requested (e.g., "other creatures you control")
                    if (amount.excludeSelf && entityId == selfId) return@filter false
                    controllerOf(state, projection, entityId) == playerId
                }
                .filter { entityId ->
                    predicateEvaluator.matches(state, projection, entityId, amount.filter, predicateContext)
                }
        }

        // When a counter kind is named, the per-entity value is its count of that counter
        // (physically stored on the permanent, layer-independent), enabling "the total <kind>
        // counters among <filter> you control" (Tom Bombadil).
        val counterFilter = amount.counterType
        return when (amount.aggregation) {
            Aggregation.COUNT -> matchingEntities.size
            Aggregation.MAX -> {
                if (counterFilter != null) return matchingEntities.maxOfOrNull { counterCountOf(state, it, counterFilter) } ?: 0
                val prop = amount.property ?: return 0
                matchingEntities.maxOfOrNull { resolveCardNumericProperty(state, projection, it, prop) } ?: 0
            }
            Aggregation.MIN -> {
                if (counterFilter != null) return matchingEntities.minOfOrNull { counterCountOf(state, it, counterFilter) } ?: 0
                val prop = amount.property ?: return 0
                matchingEntities.minOfOrNull { resolveCardNumericProperty(state, projection, it, prop) } ?: 0
            }
            Aggregation.SUM -> {
                if (counterFilter != null) return matchingEntities.sumOf { counterCountOf(state, it, counterFilter) }
                val prop = amount.property ?: return 0
                matchingEntities.sumOf { resolveCardNumericProperty(state, projection, it, prop) }
            }
            // "Number of card types" (CR 205.2a) — count only true card types
            // (Artifact, Creature, Enchantment, …), never supertypes or subtypes.
            // [ProjectedState.getTypes] folds supertypes + subtypes into one set, so intersect
            // with the known [CardType] names; this still respects projection-changed types
            // (e.g. an animated land that became a Creature).
            Aggregation.DISTINCT_TYPES -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                projection.getTypes(entityId).filterTo(mutableSetOf()) { it in CARD_TYPE_NAMES }.ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }?.toSet()
                        ?: emptySet()
                }
            }.size
            Aggregation.DISTINCT_COLORS -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                projection.getColors(entityId).ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }
            }.size
            Aggregation.DISTINCT_NAMES -> matchingEntities.mapNotNullTo(mutableSetOf()) { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name
            }.size
            Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> matchingEntities.flatMapTo(mutableSetOf<String>()) { entityId ->
                projection.getSubtypes(entityId).ifEmpty {
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                }.intersect(BASIC_LAND_SUBTYPES)
            }.size
            // Counters are physically stored on the permanent (base state, not layered), so read
            // CountersComponent directly. The map only holds kinds with a positive count
            // (CountersComponent.withRemoved drops the key at zero), so every key is a present kind.
            Aggregation.DISTINCT_COUNTER_TYPES -> matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                state.getEntity(entityId)?.get<CountersComponent>()?.counters?.keys ?: emptySet()
            }.size
            Aggregation.DISTINCT_VALUES -> {
                val prop = amount.property ?: return 0
                matchingEntities.mapTo(mutableSetOf()) {
                    resolveCardNumericProperty(state, projection, it, prop)
                }.size
            }
        }
    }

    /**
     * Evaluate an AggregateZone: collect cards from a non-battlefield zone → filter → map → aggregate.
     */
    private fun evaluateZoneAggregate(
        state: GameState,
        amount: DynamicAmount.AggregateZone,
        context: EffectContext,
        explicitProjection: ProjectedState?
    ): Int {
        val playerIds = resolveUnifiedPlayerIds(state, amount.player, context)
        val predicateContext = PredicateContext.fromEffectContext(context)

        // Non-battlefield zone: avoid reaching for [defaultProjection] entirely. The predicate
        // evaluator falls back to base CardComponent for entries missing from the projection.
        val projection: ProjectedState = explicitProjection ?: ProjectedState(state, emptyMap())
        val matchingEntities = playerIds.flatMap { playerId ->
            state.getZone(ZoneKey(playerId, amount.zone))
                .filter { entityId ->
                    predicateEvaluator.matches(state, projection, entityId, amount.filter, predicateContext)
                }
        }

        // Aggregate
        return when (amount.aggregation) {
            Aggregation.COUNT -> matchingEntities.size
            Aggregation.MAX -> {
                val prop = amount.property ?: return 0
                matchingEntities.maxOfOrNull { resolveCardNumericProperty(state, null, it, prop) } ?: 0
            }
            Aggregation.MIN -> {
                val prop = amount.property ?: return 0
                matchingEntities.minOfOrNull { resolveCardNumericProperty(state, null, it, prop) } ?: 0
            }
            Aggregation.SUM -> {
                val prop = amount.property ?: return 0
                matchingEntities.sumOf { resolveCardNumericProperty(state, null, it, prop) }
            }
            Aggregation.DISTINCT_TYPES -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }?.toSet()
                        ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_COLORS -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_NAMES -> {
                matchingEntities.mapNotNullTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.name
                }.size
            }
            Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                matchingEntities.flatMapTo(mutableSetOf<String>()) { entityId ->
                    val subtypes: Set<String> = state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                    subtypes.intersect(BASIC_LAND_SUBTYPES)
                }.size
            }
            Aggregation.DISTINCT_COUNTER_TYPES -> {
                matchingEntities.flatMapTo(mutableSetOf()) { entityId ->
                    state.getEntity(entityId)?.get<CountersComponent>()?.counters?.keys ?: emptySet()
                }.size
            }
            Aggregation.DISTINCT_VALUES -> {
                val prop = amount.property ?: return 0
                matchingEntities.mapTo(mutableSetOf()) {
                    resolveCardNumericProperty(state, null, it, prop)
                }.size
            }
        }
    }

    private fun resolveUnifiedPlayerIds(
        state: GameState,
        player: Player,
        context: EffectContext
    ): List<EntityId> {
        return when (player) {
            is Player.You -> listOf(context.controllerId)
            is Player.EachOpponent -> state.getOpponents(context.controllerId)
            is Player.TargetOpponent, is Player.TargetPlayer -> listOfNotNull(
                TargetResolutionUtils.resolvePlayerRef(player, context, state)
            )
            is Player.Each -> state.activePlayers
            is Player.Any -> state.activePlayers
            is Player.ContextPlayer -> {
                val target = context.positionalTarget(player.index) ?: return emptyList()
                when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> listOf(target.playerId)
                    else -> emptyList()
                }
            }
            is Player.ControllerOf -> {
                // Would need to resolve the target and find its controller
                emptyList()
            }
            is Player.OwnerOf -> {
                // Would need to resolve the target and find its owner
                emptyList()
            }
            is Player.TriggeringPlayer -> {
                // The player associated with the trigger event (e.g. the caster for a
                // SpellCastEvent, whose triggeringEntityId is the spell, not the player).
                // Fall back to the triggering entity for legacy events that only stamp it
                // when the entity itself is the player — mirrors TargetResolutionUtils.
                listOfNotNull(context.triggeringPlayerId ?: context.triggeringEntityId)
            }
            is Player.ActivePlayerFirst -> {
                val activePlayer = state.activePlayerId ?: return state.activePlayers
                listOf(activePlayer) + state.activePlayers.filter { it != activePlayer }
            }
            is Player.Candidate -> listOfNotNull(context.candidatePlayerId)
            is Player.ChosenOpponent -> listOfNotNull(
                context.sourceId?.let { state.getEntity(it)?.chosenOpponent() }
            )
            is Player.AnOpponent, is Player.DefendingPlayer, is Player.EnchantedPlayer -> listOfNotNull(
                TargetResolutionUtils.resolvePlayerRef(player, context, state)
            )
            is Player.OwnersOfLinkedExile -> TargetResolutionUtils.linkedExileOwners(state, context)
        }
    }

    private fun resolveUnifiedZone(zone: Zone): Zone = zone

    private fun resolveCardNumericProperty(
        state: GameState,
        projected: ProjectedState?,
        entityId: EntityId,
        property: CardNumericProperty
    ): Int {
        return when (property) {
            CardNumericProperty.MANA_VALUE -> {
                val entity = state.getEntity(entityId)
                // Rule 202.3b: face-down permanents have mana value 0
                if (entity?.has<FaceDownComponent>() == true) 0
                else entity?.get<CardComponent>()?.manaValue ?: 0
            }
            CardNumericProperty.POWER -> {
                projected?.getPower(entityId)
                    ?: state.getEntity(entityId)?.get<CardComponent>()?.baseStats?.basePower
                    ?: 0
            }
            CardNumericProperty.TOUGHNESS -> {
                projected?.getToughness(entityId)
                    ?: state.getEntity(entityId)?.get<CardComponent>()?.baseStats?.baseToughness
                    ?: 0
            }
        }
    }

    // =========================================================================
    // Station Toughness Override
    // =========================================================================

    /**
     * Returns true if any permanent controlled by [entityId]'s controller has the
     * [GrantsStationUsingToughnessComponent], enabling the creature to station using
     * toughness instead of power. Reads projected controllers (Rule 613) so the
     * effect survives control-changing continuous effects on either permanent.
     *
     * [fallbackControllerId] is consulted when [entityId] is no longer on the battlefield
     * (projection has no controller) — typically the snapshot's last-known controller
     * captured at cost-payment time (Rule 112.7a).
     */
    private fun controllerHasStationUsingToughness(
        state: GameState,
        entityId: EntityId,
        fallbackControllerId: EntityId? = null
    ): Boolean {
        val projected = state.projectedState
        val controller = projected.getController(entityId) ?: fallbackControllerId ?: return false
        return state.getBattlefield().any { permanentId ->
            val perm = state.getEntity(permanentId) ?: return@any false
            perm.has<GrantsStationUsingToughnessComponent>() &&
                projected.getController(permanentId) == controller
        }
    }

    // =========================================================================
    // Entity Numeric Property Resolution
    // =========================================================================

    /**
     * Resolve a numeric property from an entity. Unified handler for [DynamicAmount.EntityProperty].
     */
    private fun resolveNumericProperty(
        state: GameState,
        entityId: EntityId,
        property: EntityNumericProperty,
        context: EffectContext,
        useProjected: Boolean,
        explicitProjected: ProjectedState? = null
    ): Int {
        return when (property) {
            is EntityNumericProperty.Power ->
                resolvePowerOrToughness(state, entityId, isPower = true, context, useProjected, explicitProjected)

            is EntityNumericProperty.Toughness ->
                resolvePowerOrToughness(state, entityId, isPower = false, context, useProjected, explicitProjected)

            is EntityNumericProperty.ManaValue ->
                state.getEntity(entityId)?.get<CardComponent>()?.manaValue ?: 0

            is EntityNumericProperty.ManaSpent -> {
                val spell = state.getEntity(entityId)?.get<SpellOnStackComponent>() ?: return 0
                spell.manaSpentWhite + spell.manaSpentBlue + spell.manaSpentBlack +
                    spell.manaSpentRed + spell.manaSpentGreen + spell.manaSpentColorless
            }

            is EntityNumericProperty.CounterCount -> {
                val countersComponent = state.getEntity(entityId)?.get<CountersComponent>() ?: return 0
                countersComponent.getCount(resolveCounterType(property.counterType))
            }

            is EntityNumericProperty.AttachmentCount -> {
                val attachedIds = state.getEntity(entityId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
                // Attachments live on the battlefield, so narrow by *projected* subtype (a continuous
                // effect can turn a permanent into/out of an Equipment or Aura), falling back to the
                // printed subtypes when projection is empty.
                val projection = resolveProjection(state, explicitProjected)
                fun hasAttachmentSubtype(id: EntityId, subtype: Subtype): Boolean {
                    val projected = projection.getSubtypes(id)
                    if (projected.isNotEmpty()) return projected.any { it.equals(subtype.value, ignoreCase = true) }
                    return state.getEntity(id)?.get<CardComponent>()?.typeLine?.hasSubtype(subtype) == true
                }
                when (property.kind) {
                    AttachmentKind.ANY -> attachedIds.size
                    AttachmentKind.EQUIPMENT -> attachedIds.count { hasAttachmentSubtype(it, Subtype.EQUIPMENT) }
                    AttachmentKind.AURA -> attachedIds.count { hasAttachmentSubtype(it, Subtype.AURA) }
                }
            }

            is EntityNumericProperty.BlockerCount ->
                state.getEntity(entityId)
                    ?.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()
                    ?.blockerIds?.size ?: 0

            // Read from projected state when available so layer-4 type-changing effects
            // (including Changeling) are honored. Falls back to base subtypes off the battlefield.
            is EntityNumericProperty.SubtypeCount ->
                resolveSubtypeCount(state, entityId, useProjected, explicitProjected)

            // Read from projected state when available so layer-5 color-changing effects are
            // honored. Falls back to the printed colors off the battlefield.
            is EntityNumericProperty.ColorCount ->
                resolveColorCount(state, entityId, useProjected, explicitProjected)

            // Excess damage (CR 120.4a) marked on the creature: max(0, marked − toughness).
            // Amount-valued twin of the TargetMarkedDamageExceedsToughness condition — read it after
            // a deal-damage step in the same composite (Hell to Pay's "excess damage dealt this
            // way"). Off the battlefield / non-creature → 0.
            is EntityNumericProperty.ExcessMarkedDamage -> {
                if (entityId !in state.getBattlefield()) return 0
                val projection = resolveProjection(state, explicitProjected)
                if (!projection.isCreature(entityId)) return 0
                val marked = state.getEntity(entityId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
                    ?.amount ?: 0
                val toughness = projection.getToughness(entityId) ?: return 0
                (marked - toughness).coerceAtLeast(0)
            }
        }
    }

    private fun resolveSubtypeCount(
        state: GameState,
        entityId: EntityId,
        useProjected: Boolean,
        explicitProjected: ProjectedState?
    ): Int {
        if (useProjected) {
            val projectedSubtypes = resolveProjection(state, explicitProjected).getSubtypes(entityId)
            if (projectedSubtypes.isNotEmpty()) return projectedSubtypes.size
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes?.size ?: 0
    }

    private fun resolveColorCount(
        state: GameState,
        entityId: EntityId,
        useProjected: Boolean,
        explicitProjected: ProjectedState?
    ): Int {
        // For battlefield permanents the projected color set is authoritative even when empty —
        // a creature turned colorless by a layer-5 effect must count 0, not fall back to its
        // printed colors. Off the battlefield (or when projection isn't requested) use base colors.
        if (useProjected && entityId in state.getBattlefield()) {
            return resolveProjection(state, explicitProjected).getColors(entityId).size
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.colors?.size ?: 0
    }

    /**
     * Resolve power or toughness for an entity, using projected state when available
     * and falling back to base characteristic values.
     */
    private fun resolvePowerOrToughness(
        state: GameState,
        entityId: EntityId,
        isPower: Boolean,
        context: EffectContext,
        useProjected: Boolean,
        explicitProjected: ProjectedState? = null
    ): Int {
        if (useProjected) {
            val projection = resolveProjection(state, explicitProjected)
            val projectedValue = if (isPower) projection.getPower(entityId) else projection.getToughness(entityId)
            if (projectedValue != null) return projectedValue
        }
        // Last-known-info fallback for dies/leaves-the-battlefield triggers: when the
        // triggering entity is no longer on the battlefield, its projected P/T is gone,
        // so consult the value captured on the ZoneChangeEvent (Rule 603.10, 112.7a).
        if (entityId == context.triggeringEntityId || entityId == context.sourceId) {
            val lastKnown = if (isPower) context.triggerLastKnownPower else context.triggerLastKnownToughness
            if (lastKnown != null) return lastKnown
        }
        // Fall back to base stats (entity not on battlefield or projection disabled)
        return resolveCharacteristicValue(state, entityId, isPower, context)
    }

    /**
     * Resolve a characteristic value (power or toughness) from base stats.
     * Handles Fixed, Dynamic, and DynamicWithOffset characteristic values.
     */
    private fun resolveCharacteristicValue(
        state: GameState,
        entityId: EntityId,
        isPower: Boolean,
        context: EffectContext
    ): Int {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
        val value = if (isPower) card.baseStats?.power else card.baseStats?.toughness
        return when (value) {
            is CharacteristicValue.Fixed -> value.value
            is CharacteristicValue.Dynamic -> evaluate(state, value.source, context)
            is CharacteristicValue.DynamicWithOffset -> evaluate(state, value.source, context) + value.offset
            null -> 0
        }
    }

    /**
     * Read the printed base power of a card. Used by
     * [DynamicAmount.CraftedMaterialsTotalPower] to sum the power of cards in exile (CR 712.8a:
     * a card outside the battlefield/stack shows only its front-face characteristics, which is
     * the printed face for non-DFCs). Dynamic CDA values on the printed card are evaluated with
     * a fresh context targeting that card, falling back to 0 if anything is unset.
     */
    private fun basePowerOfPrintedCard(state: GameState, entityId: EntityId): Int {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
        val ctx = EffectContext(sourceId = entityId, controllerId = card.ownerId ?: return 0)
        return when (val p = card.baseStats?.power) {
            is CharacteristicValue.Fixed -> p.value
            is CharacteristicValue.Dynamic -> evaluate(state, p.source, ctx)
            is CharacteristicValue.DynamicWithOffset -> evaluate(state, p.source, ctx) + p.offset
            null -> 0
        }
    }

    /**
     * Count of counters of the kind described by [filter] on the permanent [entityId]. Counters
     * are physically stored on the permanent (base state, layer-independent), so this reads
     * [CountersComponent] directly. [CounterTypeFilter.Any] sums every kind present.
     */
    private fun counterCountOf(state: GameState, entityId: EntityId, filter: CounterTypeFilter): Int {
        val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return 0
        return when (filter) {
            is CounterTypeFilter.Any -> counters.counters.values.sum()
            else -> counters.getCount(resolveCounterType(filter))
        }
    }

    /**
     * The color(s) a single mana symbol contributes to devotion (CR 700.5). A two-color hybrid
     * contributes both halves; a monocolored hybrid ({2/B}) and a Phyrexian symbol ({B/P})
     * contribute their one color. Generic, colorless, and {X} symbols contribute nothing.
     */
    private fun manaSymbolColors(symbol: ManaSymbol): List<Color> = when (symbol) {
        is ManaSymbol.Colored -> listOf(symbol.color)
        is ManaSymbol.Hybrid -> listOf(symbol.color1, symbol.color2)
        is ManaSymbol.Phyrexian -> listOf(symbol.color)
        is ManaSymbol.MonocolorHybrid -> listOf(symbol.color)
        else -> emptyList()
    }

    private fun resolveCounterType(filter: CounterTypeFilter): CounterType {
        return when (filter) {
            is CounterTypeFilter.Any -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.PlusOnePlusOne -> CounterType.PLUS_ONE_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusOne -> CounterType.MINUS_ONE_MINUS_ONE
            is CounterTypeFilter.PlusOnePlusZero -> CounterType.PLUS_ONE_PLUS_ZERO
            is CounterTypeFilter.PlusZeroPlusOne -> CounterType.PLUS_ZERO_PLUS_ONE
            is CounterTypeFilter.MinusOneMinusZero -> CounterType.MINUS_ONE_MINUS_ZERO
            is CounterTypeFilter.MinusZeroMinusOne -> CounterType.MINUS_ZERO_MINUS_ONE
            is CounterTypeFilter.Loyalty -> CounterType.LOYALTY
            is CounterTypeFilter.Named -> {
                try {
                    CounterType.valueOf(filter.name.uppercase().replace(' ', '_'))
                } catch (_: IllegalArgumentException) {
                    CounterType.PLUS_ONE_PLUS_ONE
                }
            }
        }
    }
}
