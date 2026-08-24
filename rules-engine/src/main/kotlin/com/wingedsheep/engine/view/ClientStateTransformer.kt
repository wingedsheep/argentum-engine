package com.wingedsheep.engine.view

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.mechanics.combat.rules.DefenderBypass
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.RIOT_MODE_COUNTER
import com.wingedsheep.sdk.dsl.RIOT_MODE_HASTE
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MORPH_HELPER_CARD_IMAGE_URI
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.imageOverrideFor
import com.wingedsheep.engine.mechanics.targeting.ControllerHexproof
import com.wingedsheep.engine.mechanics.targeting.ControllerShroud
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.references.Player as ScriptPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Transforms internal game state into client-facing DTOs.
 *
 * This class:
 * - Masks hidden information (opponent's hand, libraries)
 * - Transforms internal components into explicit DTO fields
 * - Applies continuous effects to show "true" card state
 * - Prevents information leakage by only including relevant data
 */
class ClientStateTransformer(
    private val cardRegistry: CardRegistry,
    private val debugMode: Boolean = false
) {

    private val conditionEvaluator = com.wingedsheep.engine.handlers.ConditionEvaluator()
    private val visibility = Visibility(cardRegistry, debugMode)
    // Reused (with conditionEvaluator + cardRegistry) to surface each player's effective maximum
    // hand size via the shared com.wingedsheep.engine.core.MaximumHandSize source of truth.
    private val dynamicAmountEvaluator = DynamicAmountEvaluator(conditionEvaluator)


    /**
     * Transform the game state for a specific player's view.
     *
     * @param state The full game state
     * @param viewingPlayerId The player who will see this state
     * @return Client-safe game state DTO
     */
    fun transform(
        state: GameState,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false
    ): ClientGameState {
        // Project the game state to apply continuous effects (Rule 613)
        val projectedState = state.projectedState

        // Build visible cards map
        val cards = mutableMapOf<EntityId, ClientCard>()

        // Process all zones
        val zones = mutableListOf<ClientZone>()

        for ((zoneKey, entityIds) in state.zones) {
            val isZoneVisible = visibility.isZoneVisibleTo(state, zoneKey, viewingPlayerId, isSpectator)

            // For libraries we always send the full ordered list of entity IDs so the client can
            // render a correctly sized stack. Individual card *details* are only populated for cards
            // that have been revealed to the viewing player (Scry, Surveil, look-at-top-N, etc.).
            // Unrevealed slots end up as opaque IDs the client renders as card backs.
            val isLibrary = zoneKey.zoneType == Zone.LIBRARY
            val cardsWithDetails = if (isZoneVisible) {
                entityIds
            } else {
                entityIds.filter { entityId ->
                    visibility.isCardRevealedTo(state, entityId, viewingPlayerId)
                }
            }
            val zoneCardIds = if (isLibrary) entityIds else cardsWithDetails

            zones.add(
                ClientZone(
                    zoneId = zoneKey,
                    cardIds = zoneCardIds,
                    size = entityIds.size,
                    isVisible = isZoneVisible || cardsWithDetails.isNotEmpty() || isLibrary
                )
            )

            // Include card details for visible cards (either whole zone visible, or individually revealed)
            for (entityId in cardsWithDetails) {
                val clientCard = transformCard(state, entityId, zoneKey, projectedState, viewingPlayerId, isSpectator)
                if (clientCard != null) {
                    cards[entityId] = clientCard
                }
            }
        }

        // --- FIX START: Ensure Battlefield is always present ---
        if (zones.none { it.zoneId.zoneType == Zone.BATTLEFIELD }) {
            val bfZoneKey = ZoneKey(viewingPlayerId, Zone.BATTLEFIELD)
            val bfEntities = state.getBattlefield()

            zones.add(
                ClientZone(
                    zoneId = bfZoneKey,
                    cardIds = bfEntities,
                    size = bfEntities.size,
                    isVisible = true
                )
            )

            // Add cards if they happen to exist (rare if zone was missing from map, but good for safety)
            for (entityId in bfEntities) {
                if (entityId !in cards) {
                    val clientCard = transformCard(state, entityId, bfZoneKey, projectedState, viewingPlayerId, isSpectator)
                    if (clientCard != null) {
                        cards[entityId] = clientCard
                    }
                }
            }
        }
        // --- FIX END ---

        // --- FIX START: Ensure Stack is always present ---
        if (zones.none { it.zoneId.zoneType == Zone.STACK }) {
            val stackZoneKey = ZoneKey(viewingPlayerId, Zone.STACK)
            zones.add(
                ClientZone(
                    zoneId = stackZoneKey,
                    cardIds = state.stack,
                    size = state.stack.size,
                    isVisible = true
                )
            )

            // Include card details for stack items
            for (entityId in state.stack) {
                if (entityId !in cards) {
                    val clientCard = transformCard(state, entityId, stackZoneKey, projectedState, viewingPlayerId, isSpectator)
                        ?: transformAbilityOnStack(state, entityId, viewingPlayerId)
                    if (clientCard != null) {
                        cards[entityId] = clientCard
                    }
                }
            }
        }
        // --- FIX END ---

        // Reveal top library card for players who "play with the top card revealed"
        // (PlayFromTopOfLibrary, e.g. Future Sight; or RevealTopOfLibrary, e.g. Goblin Spy).
        // The top card is revealed to ALL players per the card's oracle text. The two abilities
        // differ only in play permission (handled elsewhere); the public reveal is identical.
        for (ownerId in state.turnOrder) {
            if (revealsTopOfLibraryPublicly(state, ownerId)) {
                val library = state.getLibrary(ownerId)
                if (library.isNotEmpty()) {
                    val topCardId = library.first()
                    if (topCardId !in cards) {
                        val libraryZoneKey = ZoneKey(ownerId, Zone.LIBRARY)
                        val clientCard = transformCard(state, topCardId, libraryZoneKey, projectedState, viewingPlayerId)
                        if (clientCard != null) {
                            cards[topCardId] = clientCard
                            // Update the library zone entry to include this card as visible
                            val zoneIndex = zones.indexOfFirst {
                                it.zoneId.ownerId == ownerId && it.zoneId.zoneType == Zone.LIBRARY
                            }
                            if (zoneIndex >= 0) {
                                val existingZone = zones[zoneIndex]
                                val newCardIds = if (existingZone.cardIds.contains(topCardId)) {
                                    existingZone.cardIds
                                } else {
                                    listOf(topCardId) + existingZone.cardIds
                                }
                                zones[zoneIndex] = existingZone.copy(
                                    cardIds = newCardIds,
                                    isVisible = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reveal top library card privately for players with LookAtTopOfLibrary (e.g., Lens of Clarity)
        // Unlike PlayFromTopOfLibrary, this only reveals to the controller, not all players.
        if (!isSpectator && hasLookAtTopOfLibrary(state, viewingPlayerId)) {
            val library = state.getLibrary(viewingPlayerId)
            if (library.isNotEmpty()) {
                val topCardId = library.first()
                if (topCardId !in cards) {
                    val libraryZoneKey = ZoneKey(viewingPlayerId, Zone.LIBRARY)
                    val clientCard = transformCard(state, topCardId, libraryZoneKey, projectedState, viewingPlayerId)
                    if (clientCard != null) {
                        cards[topCardId] = clientCard
                        val zoneIndex = zones.indexOfFirst {
                            it.zoneId.ownerId == viewingPlayerId && it.zoneId.zoneType == Zone.LIBRARY
                        }
                        if (zoneIndex >= 0) {
                            val existingZone = zones[zoneIndex]
                            val newCardIds = if (existingZone.cardIds.contains(topCardId)) {
                                existingZone.cardIds
                            } else {
                                listOf(topCardId) + existingZone.cardIds
                            }
                            zones[zoneIndex] = existingZone.copy(
                                cardIds = newCardIds,
                                isVisible = true
                            )
                        }
                    }
                }
            }
        }

        // Build player information
        val players = state.turnOrder.map { playerId ->
            transformPlayer(state, playerId, viewingPlayerId)
        }

        // Build combat state if in combat
        val combat = transformCombat(state)

        // Get active and priority players, defaulting to first player if not set
        val activePlayerId = state.activePlayerId ?: state.turnOrder.firstOrNull() ?: viewingPlayerId
        val priorityPlayerId = state.priorityPlayerId ?: activePlayerId

        // Hotseat (play-against-yourself): the viewing player holds input authority for
        // every seat via HotseatControlComponent. Spectators never get hotseat control.
        val hotseat = !isSpectator && state.turnOrder.any { playerId ->
            state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.player.HotseatControlComponent>()
                ?.controllerId == viewingPlayerId
        }

        // Hijack indicators (Mindslaver-style). For every other player whose turn the
        // viewing player currently controls, set youAreHijacking = that player. If the
        // viewing player is themselves being controlled, set youAreHijackedBy. Skipped in
        // hotseat, where actorFor also redirects but the dedicated [hotseat] flag drives UI.
        var youAreHijacking: EntityId? = null
        var youAreHijackedBy: EntityId? = null
        if (!hotseat) {
            for (playerId in state.turnOrder) {
                val actor = state.actorFor(playerId)
                if (playerId == viewingPlayerId && actor != viewingPlayerId) {
                    youAreHijackedBy = actor
                } else if (playerId != viewingPlayerId && actor == viewingPlayerId) {
                    youAreHijacking = playerId
                }
            }
        }

        // Persistent yields are private to each player: only ever surface the viewer's own.
        val activeYields = if (isSpectator) emptyList() else buildClientYields(state.yieldsFor(viewingPlayerId))

        // The deck tracker is the viewer's own decklist — never a spectator's or an opponent's.
        val deck = if (isSpectator) emptyList() else buildDeckList(state, viewingPlayerId)

        return ClientGameState(
            viewingPlayerId = viewingPlayerId,
            cards = cards,
            zones = zones,
            players = players,
            currentPhase = state.phase,
            currentStep = state.step,
            activePlayerId = activePlayerId,
            priorityPlayerId = priorityPlayerId,
            turnNumber = state.turnNumber,
            isGameOver = state.gameOver,
            winnerId = state.winnerId,
            combat = combat,
            voidActive = state.nonlandPermanentLeftBattlefieldThisTurn || state.spellWarpedThisTurn,
            dayNight = state.dayNight,
            youAreHijacking = youAreHijacking,
            youAreHijackedBy = youAreHijackedBy,
            hotseat = hotseat,
            activeYields = activeYields,
            deck = deck
        )
    }

    /**
     * Build [viewingPlayerId]'s own decklist: every non-token card they *own*, wherever it now is,
     * grouped by printed card.
     *
     * Deriving the list from live ownership rather than a stored decklist keeps it honest for free —
     * a stolen creature is still in its owner's deck (ownership never changes, CR 108.3), a token
     * copy of one never is, and a permanent copying something else counts as the card it was printed
     * as (that's what [CopyOfComponent] remembers).
     *
     * Zones: the sideboard is excluded as outside the game (CR 400.11a); the command zone is included
     * so a commander stays one stable row instead of appearing and vanishing as it's cast and
     * returns. The stack lives outside [GameState.zones], so it is walked separately.
     */
    private fun buildDeckList(state: GameState, viewingPlayerId: EntityId): List<ClientDeckCard> {
        class Tally {
            var copies = 0
            var remaining = 0
            /** Art from a real (non-copy) printing of this card in the game, if we saw one. */
            var printingImageUri: String? = null
        }

        val tallies = HashMap<String, Tally>()

        fun record(entityId: EntityId, zoneType: Zone) {
            val container = state.getEntity(entityId) ?: return
            if (container.has<TokenComponent>()) return
            val cardComponent = container.get<CardComponent>() ?: return
            val ownerId = cardComponent.ownerId ?: container.get<OwnerComponent>()?.playerId
            if (ownerId != viewingPlayerId) return

            // A copy effect overwrites CardComponent with the copied card; CopyOfComponent holds
            // what this card is actually printed as, which is what belongs in the decklist.
            val copyOf = container.get<CopyOfComponent>()
            val definitionId = copyOf?.originalCardDefinitionId ?: cardComponent.cardDefinitionId

            val tally = tallies.getOrPut(definitionId) { Tally() }
            tally.copies++
            if (!ownerKnowsIdentity(state, entityId, zoneType, viewingPlayerId)) tally.remaining++
            if (copyOf == null && tally.printingImageUri == null) {
                tally.printingImageUri = cardComponent.imageUri
            }
        }

        for ((zoneKey, entityIds) in state.zones) {
            if (zoneKey.zoneType == Zone.SIDEBOARD) continue
            for (entityId in entityIds) record(entityId, zoneKey.zoneType)
        }
        for (entityId in state.stack) record(entityId, Zone.STACK)

        return tallies.mapNotNull { (definitionId, tally) ->
            val definition = cardRegistry.getCard(definitionId) ?: return@mapNotNull null
            ClientDeckCard(
                cardName = definition.name,
                copies = tally.copies,
                remaining = tally.remaining,
                cmc = definition.cmc,
                cardTypes = definition.typeLine.cardTypes.map { it.name },
                colors = definition.colors.map { it.name },
                imageUri = tally.printingImageUri ?: definition.metadata.imageUri
            )
        }.sortedWith(compareBy({ it.cmc }, { it.cardName }))
    }

    /**
     * Whether the owner of [entityId] can currently tell *which* of their cards this one is.
     *
     * False for anything in their library (that's the whole point — you know what's in your deck,
     * not where), and for a card of theirs that is face down somewhere they can't look: exiled face
     * down, or a face-down permanent an opponent controls. Mirrors the face-down masking in
     * [transformCard] so the tracker never claims knowledge the client wasn't sent.
     */
    private fun ownerKnowsIdentity(
        state: GameState,
        entityId: EntityId,
        zoneType: Zone,
        viewingPlayerId: EntityId
    ): Boolean {
        if (zoneType == Zone.LIBRARY) return false
        val container = state.getEntity(entityId) ?: return false
        val isInFaceDownZone =
            zoneType == Zone.BATTLEFIELD || zoneType == Zone.STACK || zoneType == Zone.EXILE
        if (!isInFaceDownZone || !container.has<FaceDownComponent>()) return true
        val controllerId = container.get<ControllerComponent>()?.playerId
        return controllerId == viewingPlayerId ||
            visibility.isCardRevealedTo(state, entityId, viewingPlayerId) ||
            (zoneType == Zone.BATTLEFIELD && visibility.hasLookAtFaceDownCreatures(state, viewingPlayerId))
    }

    /**
     * Flatten a player's [com.wingedsheep.engine.state.PlayerYields] into one [ClientYield] per
     * ability identity, merging the auto-pass scopes and the auto-answer into a single display row.
     * The display name is the card name carried in the definition id (`"Name#SET-123"` → `"Name"`).
     */
    private fun buildClientYields(yields: com.wingedsheep.engine.state.PlayerYields): List<ClientYield> {
        val identities = yields.untilEndOfTurn + yields.wholeGame + yields.autoAnswer.keys
        return identities.map { id ->
            ClientYield(
                cardDefinitionId = id.cardDefinitionId,
                abilityId = id.abilityId.value,
                displayName = id.cardDefinitionId.substringBefore("#"),
                untilEndOfTurn = id in yields.untilEndOfTurn,
                wholeGame = id in yields.wholeGame,
                autoAnswer = yields.autoAnswer[id]
            )
        }
    }

    /**
     * Check if a zone's contents should be visible to a player.
     */
    /**
     * True when [cardId] (in exile) is linked to a battlefield permanent controlled by
     * [viewingPlayerId] that grants a cast-from-linked-exile permission, the card matches
     * its filter, and (when applicable) ownership matches. Used to flag linked-exile
     * cards as ghost cards in the viewer's hand so they know they can cast them later.
     */
    private fun isCastableFromLinkedExile(
        state: GameState,
        viewingPlayerId: EntityId,
        cardId: EntityId,
        cardContainer: com.wingedsheep.engine.state.ComponentContainer
    ): Boolean {
        val cardComp = cardContainer.get<CardComponent>() ?: return false
        for (permId in state.getBattlefield()) {
            val permContainer = state.getEntity(permId) ?: continue
            if (permContainer.get<ControllerComponent>()?.playerId != viewingPlayerId) continue
            val linked = permContainer.get<LinkedExileComponent>() ?: continue
            if (cardId !in linked.exiledIds) continue
            val permCard = permContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(permCard.cardDefinitionId) ?: continue
            val grant = cardDef.script.staticAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile>()
                .firstOrNull() ?: continue
            if (grant.ownedByYou && cardComp.ownerId != viewingPlayerId) continue
            // "exiled with [granter] this turn" gates eligibility on the turn the card
            // entered exile (e.g. Maralen). Cards exiled on prior turns aren't castable,
            // so they shouldn't appear as ghost-castables in the viewer's hand either.
            if (grant.exiledThisTurnOnly) {
                val turn = cardContainer
                    .get<com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent>()
                    ?.turnNumber
                if (turn == null || turn != state.turnNumber) continue
            }
            // Filter check mirrors the enumerator's CardPredicate loop for parity.
            val passesFilter = grant.filter.cardPredicates.all { pred ->
                when (pred) {
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !cardComp.typeLine.isLand
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> cardComp.typeLine.isCreature
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> cardComp.typeLine.isArtifact
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonartifact -> !cardComp.typeLine.isArtifact
                    else -> true
                }
            }
            if (passesFilter) return true
        }
        return false
    }

    /**
     * Resolve a static ability that may be gated by a [ConditionalStaticAbility] (e.g. The
     * Belligerent's play-from-top window, a [LookAtTopOfLibrary] gated on "attacked this turn").
     * Unconditional abilities pass through unchanged; a conditional one resolves to its inner
     * ability only while its condition currently holds for the granting permanent, and to null
     * otherwise. Mirrors `CastPermissionUtils.activeStaticAbility` so that what a player can SEE
     * stays in sync with what the legal-actions layer lets them DO.
     */
    /**
     * Check if a player controls a permanent that reveals the top card of their library to all
     * players — either [PlayFromTopOfLibrary] (Future Sight) or [RevealTopOfLibrary] (Goblin Spy).
     * The two abilities share the public-reveal visibility; they diverge only in play permission,
     * which is handled by the cast/play-from-top paths (keyed on [PlayFromTopOfLibrary] alone).
     */
    /**
     * Check if a player controls a permanent with an active LookAtTopOfLibrary (e.g., Lens of
     * Clarity; The Belligerent's is gated behind "attacked this turn").
     * This reveals the top card of the controller's library privately (only to them).
     */
    /**
     * Check if a player controls a permanent with LookAtFaceDownCreatures (e.g., Lens of Clarity).
     * This reveals the identity of opponent's face-down battlefield creatures to the controller.
     */
    /**
     * Check if an individual card has been revealed to a specific player.
     * This is used for "look at hand" or "reveal hand" effects where the viewing player
     * can see specific cards in an otherwise hidden zone.
     */
    private fun revealsTopOfLibraryPublicly(state: GameState, playerId: EntityId): Boolean =
        visibility.revealsTopOfLibraryPublicly(state, playerId)

    private fun hasLookAtTopOfLibrary(state: GameState, playerId: EntityId): Boolean =
        visibility.hasLookAtTopOfLibrary(state, playerId)

    /**
     * Transform an activated or triggered ability on the stack into a ClientCard DTO.
     * These don't have CardComponent, so we create a synthetic card representation.
     */
    private fun transformAbilityOnStack(
        state: GameState,
        entityId: EntityId,
        viewingPlayerId: EntityId
    ): ClientCard? {
        val container = state.getEntity(entityId) ?: return null

        // Helper to transform targets
        fun transformTargets(targetsComponent: TargetsComponent?): List<ClientChosenTarget> {
            return targetsComponent?.targets?.mapNotNull { chosenTarget ->
                when (chosenTarget) {
                    is ChosenTarget.Player -> ClientChosenTarget.Player(chosenTarget.playerId)
                    is ChosenTarget.Permanent -> ClientChosenTarget.Permanent(chosenTarget.entityId)
                    is ChosenTarget.Spell -> ClientChosenTarget.Spell(chosenTarget.spellEntityId)
                    is ChosenTarget.Card -> ClientChosenTarget.Card(chosenTarget.cardId)
                }
            } ?: emptyList()
        }

        // Check for activated ability
        val activatedAbility = container.get<ActivatedAbilityOnStackComponent>()
        if (activatedAbility != null) {
            // Get the source card's info to display
            val sourceCard = state.getEntity(activatedAbility.sourceId)?.get<CardComponent>()
            val cardDef = cardRegistry.getCard(activatedAbility.sourceName)

            // Get targets for this ability
            val targetsComponent = container.get<TargetsComponent>()
            val targets = transformTargets(targetsComponent)

            return ClientCard(
                id = entityId,
                name = "${activatedAbility.sourceName} ability",
                manaCost = "",
                manaValue = 0,
                typeLine = "Ability",
                cardTypes = setOf("Ability"),
                subtypes = emptySet(),
                colors = sourceCard?.colors ?: emptySet(),
                oracleText = activatedAbility.descriptionOverride
                    ?: runtimeAbilityText(state, entityId, activatedAbility)
                    ?: effectDisplayText(activatedAbility.effect, selfNounFor(state, activatedAbility.sourceId)),
                power = null,
                toughness = null,
                basePower = null,
                baseToughness = null,
                damage = null,
                keywords = emptySet(),
                counters = emptyMap(),
                isTapped = false,
                hasSummoningSickness = false,
                isTransformed = false,
                isAttacking = false,
                isBlocking = false,
                attackingTarget = null,
                blockingTarget = null,
                controllerId = activatedAbility.controllerId,
                ownerId = activatedAbility.controllerId,
                isToken = false,
                zone = ZoneKey(activatedAbility.controllerId, Zone.STACK),
                attachedTo = null,
                attachments = emptyList(),
                isFaceDown = false,
                targets = targets,
                imageUri = sourceCard?.imageUri ?: cardDef?.metadata?.imageUri,
                chosenX = activatedAbility.xValue,
                abilityIdentity = activatedAbility.abilityIdentity?.let {
                    ClientAbilityIdentity(it.cardDefinitionId, it.abilityId.value)
                }
            )
        }

        // Check for triggered ability
        val triggeredAbility = container.get<TriggeredAbilityOnStackComponent>()
        if (triggeredAbility != null) {
            val sourceCard = state.getEntity(triggeredAbility.sourceId)?.get<CardComponent>()
            val cardDef = cardRegistry.getCard(triggeredAbility.sourceName)

            val targetsComponent = container.get<TargetsComponent>()
            val targets = transformTargets(targetsComponent)

            // Triggering entity ID for visual source arrow (separate from targeting arrows)
            val triggeringId = triggeredAbility.triggeringEntityId?.takeIf { id ->
                state.getBattlefield().contains(id)
            }

            // Find the source entity's current zone (for graveyard trigger styling)
            val sourceZone = findEntityZone(state, triggeredAbility.sourceId)

            // Modal-copy breakdown: spell copies carry the original's chosenModes (700.2g) so the
            // opponent can see the same per-mode text and target names on the copy.
            val triggeredModal = triggeredAbility.effect as? ModalEffect
            val triggeredModeDescriptions: List<String> =
                if (triggeredModal != null && triggeredAbility.chosenModes.isNotEmpty()) {
                    val evaluator = DynamicAmountEvaluator()
                    val context = triggeredAbilityContext(state, entityId, triggeredAbility)
                    triggeredAbility.chosenModes.map { modeIndex ->
                        val mode = triggeredModal.modes.getOrNull(modeIndex)
                            ?: return@map "Unknown mode"
                        try {
                            mode.effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
                        } catch (_: Exception) {
                            mode.description
                        }
                    }
                } else emptyList()
            val triggeredPerModeTargets: List<ClientPerModeTargetGroup> =
                if (triggeredAbility.chosenModes.isNotEmpty()) {
                    buildPerModeTargetGroups(
                        state,
                        triggeredAbility.chosenModes,
                        triggeredAbility.modeTargetsOrdered,
                        triggeredModeDescriptions,
                        viewingPlayerId
                    )
                } else emptyList()

            return ClientCard(
                id = entityId,
                name = "${triggeredAbility.sourceName} trigger",
                manaCost = "",
                manaValue = 0,
                typeLine = "Triggered Ability",
                cardTypes = setOf("Ability"),
                subtypes = emptySet(),
                colors = sourceCard?.colors ?: emptySet(),
                oracleText = triggeredAbility.descriptionOverride
                    ?: runtimeAbilityText(state, entityId, triggeredAbility)
                    ?: effectDisplayText(triggeredAbility.effect, selfNounFor(state, triggeredAbility.sourceId)),
                power = null,
                toughness = null,
                basePower = null,
                baseToughness = null,
                damage = null,
                keywords = emptySet(),
                counters = emptyMap(),
                isTapped = false,
                hasSummoningSickness = false,
                isTransformed = false,
                isAttacking = false,
                isBlocking = false,
                attackingTarget = null,
                blockingTarget = null,
                controllerId = triggeredAbility.controllerId,
                ownerId = triggeredAbility.controllerId,
                isToken = false,
                zone = ZoneKey(triggeredAbility.controllerId, Zone.STACK),
                attachedTo = null,
                attachments = emptyList(),
                isFaceDown = false,
                targets = targets,
                triggeringEntityId = triggeringId,
                imageUri = sourceCard?.imageUri ?: cardDef?.metadata?.imageUri,
                sourceZone = sourceZone,
                chosenX = triggeredAbility.xValue,
                abilityIdentity = triggeredAbility.abilityIdentity?.let {
                    ClientAbilityIdentity(it.cardDefinitionId, it.abilityId.value)
                },
                copyIndex = triggeredAbility.copyIndex,
                copyTotal = triggeredAbility.copyTotal,
                chosenModeDescriptions = triggeredModeDescriptions,
                perModeTargets = triggeredPerModeTargets
            )
        }

        return null
    }

    /**
     * Transform an entity into a ClientCard DTO.
     */
    private fun transformCard(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        projectedState: ProjectedState,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false
    ): ClientCard? {
        val container = state.getEntity(entityId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null

        // Get base controller (default to owner if not set)
        val baseControllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return null

        // Get owner
        val ownerId = cardComponent.ownerId ?: container.get<OwnerComponent>()?.playerId ?: baseControllerId

        // For battlefield permanents, use projected values from the layer system (Rule 613)
        // For cards in other zones, use base values
        val projectedValues = if (zoneKey.zoneType == Zone.BATTLEFIELD) {
            projectedState.getProjectedValues(entityId)
        } else {
            null
        }

        // Use projected controller for battlefield permanents (accounts for control-changing effects)
        val controllerId = projectedValues?.controllerId ?: baseControllerId

        // A card is face-down if it has FaceDownComponent (on battlefield/exile) OR is cast face-down on the stack.
        // Per MTG rules, face-down cards are always revealed when they leave the battlefield/stack,
        // so only allow face-down status in zones where it makes sense (defense-in-depth).
        val spellOnStack = container.get<SpellOnStackComponent>()
        val isInFaceDownZone = zoneKey.zoneType == Zone.BATTLEFIELD || zoneKey.zoneType == Zone.STACK || zoneKey.zoneType == Zone.EXILE
        // A card exiled face down that the viewer has been granted permission to PLAY
        // ("look at and play the exiled cards" — Black Cat, Cunning Thief) must not be hidden from
        // that viewer, or the client has no card data to act on the CastSpell the server offers.
        // It stays face-down (masked) to everyone else. Keyed on the same may-play check that drives
        // `playableFromExile` below, so visibility and playability stay in lockstep. Scoped to exile
        // so battlefield/stack morph masking (a face-down 2/2 revealed via Spy Network stays a 2/2)
        // is unchanged.
        val viewerMayPlayThisExiledCard = !isSpectator &&
            zoneKey.zoneType == Zone.EXILE &&
            state.hasMayPlayFor(entityId, viewingPlayerId, conditionEvaluator, cardRegistry)
        val isFaceDown = isInFaceDownZone &&
            (container.has<FaceDownComponent>() || spellOnStack?.castFaceDown == true) &&
            !viewerMayPlayThisExiledCard
        // Use projected P/T which correctly handles face-down base 2/2 + any modifications.
        // CR 208.3: a noncreature permanent has no power or toughness — even one with a printed P/T
        // (a Vehicle), and even a creature turned into an artifact/land by a type-changing effect
        // (Kitesail Larcenist's Treasure, Song of the Dryads' Forest). On the battlefield we
        // therefore only surface P/T for permanents that are creatures in projected state (face-down
        // permanents are always 2/2 creatures), so a transformed permanent stops showing a stat box.
        // Non-battlefield zones keep base P/T (a creature card in hand still shows its printed P/T).
        val onBattlefield = zoneKey.zoneType == Zone.BATTLEFIELD
        val showsPowerToughness = !onBattlefield || isFaceDown || projectedState.isCreature(entityId)
        val power = if (!showsPowerToughness) null
            else projectedValues?.power ?: if (isFaceDown) 2 else cardComponent.baseStats?.basePower
        val toughness = if (!showsPowerToughness) null
            else projectedValues?.toughness ?: if (isFaceDown) 2 else cardComponent.baseStats?.baseToughness
        val rawKeywords = projectedValues?.keywords?.mapNotNull {
            when {
                // Granted toxic floats as TOXIC_<N> (e.g. Skrelv's activated ability); collapse
                // to the bare TOXIC keyword so the icon-render path picks it up.
                it.startsWith("TOXIC_") -> Keyword.TOXIC
                else -> try { Keyword.valueOf(it) } catch (_: Exception) { null }
            }
        }?.toSet() ?: cardComponent.baseKeywords
        val abilityFlags = projectedValues?.keywords?.mapNotNull {
            try { AbilityFlag.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: cardComponent.baseFlags.toSet()

        // Extract protection colors from projected keywords (PROTECTION_FROM_*) and card definition
        val protectionPrefix = "PROTECTION_FROM_"
        val projectedProtections = projectedValues?.keywords
            ?.filter { it.startsWith(protectionPrefix) }
            ?.mapNotNull { try { Color.valueOf(it.removePrefix(protectionPrefix)) } catch (_: Exception) { null } }
            ?: emptyList()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        // Which face-down mechanic this object is *drawn* as: it decides the helper card every
        // surface shows in place of the hidden art (morph's helmet, the Manifest token, "A
        // Mysterious Creature" for disguise/cloak). A permanent carries the mode as a component
        // from the moment it enters, but a spell still on the stack does not — it is stamped when
        // it resolves — so derive that one from the keyword it was cast under. Without the
        // fallback a creature *being cast* with disguise would be drawn as a morph.
        val faceDownDisplayMode = container.get<FaceDownModeComponent>()?.mode
            ?: if (spellOnStack?.castFaceDown == true) FaceDownTurnUp.castMode(cardDef) else null
        val faceDownHelperCardImageUri =
            faceDownDisplayMode?.helperCardImageUri ?: MORPH_HELPER_CARD_IMAGE_URI
        val staticProtections = cardDef?.keywordAbilities
            ?.filterIsInstance<KeywordAbility.Protection>()
            ?.mapNotNull { (it.scope as? ProtectionScope.Color)?.color }
            ?: emptyList()
        val protections = (projectedProtections.ifEmpty { staticProtections }).distinct()

        // Extract hexproof-from-color colors from projected keywords (HEXPROOF_FROM_*).
        // Both intrinsic per-color hexproof (HexproofFromComponent) and dynamically granted
        // hexproof (Tam, Mindful First-Year) flow through the same projection keywords.
        // Non-color scopes in that namespace (HEXPROOF_FROM_CARDTYPE_INSTANT, HEXPROOF_FROM_MONOCOLORED)
        // fail Color.valueOf and drop out here — like protection-from-card-type they surface to the
        // player through the card's oracle text rather than a per-color badge.
        val hexproofFromPrefix = "HEXPROOF_FROM_"
        val hexproofFromColors = projectedValues?.keywords
            ?.filter { it.startsWith(hexproofFromPrefix) }
            ?.mapNotNull { try { Color.valueOf(it.removePrefix(hexproofFromPrefix)) } catch (_: Exception) { null } }
            ?.distinct()
            ?: emptyList()

        // Hexproof from monocolored (CR 105.2) — a quality, not a color, so it rides its own flag.
        val hexproofFromMonocolored = projectedValues?.keywords?.contains("HEXPROOF_FROM_MONOCOLORED") ?: false

        // Hexproof from multicolored (CR 105.2b) — the other half of the quality pair, same shape.
        val hexproofFromMulticolored = projectedValues?.keywords?.contains("HEXPROOF_FROM_MULTICOLORED") ?: false

        // Add PROTECTION keyword when protections are present
        val keywords = if (protections.isNotEmpty()) rawKeywords + Keyword.PROTECTION else rawKeywords

        val colors = projectedValues?.colors?.mapNotNull {
            try { Color.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: cardComponent.colors

        // Get state components
        val isTapped = container.has<TappedComponent>()
        val isExerted = container.has<com.wingedsheep.engine.state.components.battlefield.ExertedComponent>()
        val isPhasedOut = container.has<PhasedOutComponent>()
        // Summoning sickness doesn't affect creatures with haste. The engine attaches the
        // marker to every entering permanent (so Vehicles / animated lands inherit it when
        // they become creatures), so gate on projected creature-ness here too — otherwise a
        // freshly played Mountain or Equipment would report summoning sickness to the client.
        val hasSummoningSicknessComponent = container.has<SummoningSicknessComponent>()
        val hasHaste = keywords.contains(com.wingedsheep.sdk.core.Keyword.HASTE)
        val hasSummoningSickness = hasSummoningSicknessComponent && !hasHaste &&
            projectedState.isCreature(entityId)

        // Get morph data for face-down creatures
        val morphData = container.get<MorphDataComponent>()

        // Handle face-down card masking
        // Opponents and spectators see modified stats but no card information
        // Controller sees real card info + morph cost (but not spectators)
        if (isFaceDown && (isSpectator || controllerId != viewingPlayerId)) {
            // Check if the face-down card has been revealed to the viewing player (e.g., via Spy Network)
            // Also check LookAtFaceDownCreatures (e.g., Lens of Clarity) — only for battlefield creatures,
            // not face-down spells on the stack (per ruling).
            val isRevealedToViewer = !isSpectator && (
                visibility.isCardRevealedTo(state, entityId, viewingPlayerId) ||
                (zoneKey.zoneType == Zone.BATTLEFIELD && visibility.hasLookAtFaceDownCreatures(state, viewingPlayerId))
            )

            // Face-down exiled cards show minimal info (not creatures, no P/T)
            if (zoneKey.zoneType == Zone.EXILE) {
                return ClientCard(
                    id = entityId,
                    name = "Face-down card",
                    manaCost = "",
                    manaValue = 0,
                    typeLine = "",
                    cardTypes = emptySet(),
                    subtypes = emptySet(),
                    colors = emptySet(),
                    oracleText = "",
                    power = null,
                    toughness = null,
                    basePower = null,
                    baseToughness = null,
                    damage = null,
                    keywords = emptySet(),
                    abilityFlags = emptySet(),
                    protections = emptyList(),
                    counters = emptyMap(),
                    isTapped = false,
                    hasSummoningSickness = false,
                    isTransformed = false,
                    isAttacking = false,
                    isBlocking = false,
                    attackingTarget = null,
                    blockingTarget = null,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    isToken = false,
                    zone = zoneKey,
                    attachedTo = null,
                    attachments = emptyList(),
                    isFaceDown = true,
                    faceDownMode = faceDownDisplayMode?.name,
                    morphCost = null,
                    imageUri = faceDownHelperCardImageUri,
                    activeEffects = emptyList(),
                    revealedName = if (isRevealedToViewer) cardComponent.name else null,
                    revealedImageUri = if (isRevealedToViewer) (cardComponent.imageUri ?: cardDef?.metadata?.imageUri) else null
                )
            }

            // Face-down battlefield/stack creatures (morph)
            // Use projected keywords — granted keywords (e.g., flying from an aura) are public information
            val faceDownKeywords = projectedValues?.keywords?.mapNotNull {
                try { Keyword.valueOf(it) } catch (_: Exception) { null }
            }?.toSet() ?: emptySet()
            val faceDownAbilityFlags = projectedValues?.keywords?.mapNotNull {
                try { AbilityFlag.valueOf(it) } catch (_: Exception) { null }
            }?.toSet() ?: emptySet()
            // Extract protection colors from projected keywords for face-down creatures
            val faceDownProtections = projectedValues?.keywords
                ?.filter { it.startsWith(protectionPrefix) }
                ?.mapNotNull { try { Color.valueOf(it.removePrefix(protectionPrefix)) } catch (_: Exception) { null } }
                ?: emptyList()
            val faceDownHexproofFromColors = projectedValues?.keywords
                ?.filter { it.startsWith(hexproofFromPrefix) }
                ?.mapNotNull { try { Color.valueOf(it.removePrefix(hexproofFromPrefix)) } catch (_: Exception) { null } }
                ?.distinct()
                ?: emptyList()
            val faceDownKeywordsWithProtection = if (faceDownProtections.isNotEmpty()) faceDownKeywords + Keyword.PROTECTION else faceDownKeywords
            return ClientCard(
                id = entityId,
                name = "Face-down creature",
                manaCost = "",
                manaValue = 0,
                typeLine = "Creature",
                cardTypes = setOf("CREATURE"),
                subtypes = emptySet(),
                colors = emptySet(),
                oracleText = "",
                power = power,
                toughness = toughness,
                basePower = 2,
                baseToughness = 2,
                damage = container.get<DamageComponent>()?.amount,
                keywords = faceDownKeywordsWithProtection,
                abilityFlags = faceDownAbilityFlags,
                protections = faceDownProtections,
                hexproofFromColors = faceDownHexproofFromColors,
                counters = container.get<CountersComponent>()?.counters ?: emptyMap(),
                isTapped = isTapped,
                isExerted = isExerted,
                hasSummoningSickness = hasSummoningSickness,
                isTransformed = false,
                isPhasedOut = isPhasedOut,
                isAttacking = container.get<AttackingComponent>() != null,
                isBlocking = container.get<BlockingComponent>() != null,
                attackingTarget = container.get<AttackingComponent>()?.defenderId,
                blockingTarget = container.get<BlockingComponent>()?.blockedAttackerIds?.firstOrNull(),
                controllerId = controllerId,
                ownerId = ownerId,
                isToken = false,
                zone = zoneKey,
                attachedTo = container.get<AttachedToComponent>()?.targetId,
                attachments = state.getBattlefield().filter { otherId ->
                    state.getEntity(otherId)?.get<AttachedToComponent>()?.targetId == entityId
                },
                linkedExile = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList(),
                isFaceDown = true,
                faceDownMode = faceDownDisplayMode?.name,
                morphCost = null, // Opponent can't see morph cost
                imageUri = faceDownHelperCardImageUri,
                activeEffects = buildCardActiveEffects(state, entityId),
                revealedName = if (isRevealedToViewer) cardComponent.name else null,
                revealedImageUri = if (isRevealedToViewer) (cardComponent.imageUri ?: cardDef?.metadata?.imageUri) else null
            )
        }

        // Get damage
        val damageComponent = container.get<DamageComponent>()
        val damage = damageComponent?.amount

        // Get counters
        val countersComponent = container.get<CountersComponent>()
        val counters = countersComponent?.counters ?: emptyMap()

        // Get combat state
        val attackingComponent = container.get<AttackingComponent>()
        val blockingComponent = container.get<BlockingComponent>()
        val isAttacking = attackingComponent != null
        val isBlocking = blockingComponent != null
        val attackingTarget = attackingComponent?.defenderId
        val blockingTarget = blockingComponent?.blockedAttackerIds?.firstOrNull()

        // Get attachments
        val attachedToComponent = container.get<AttachedToComponent>()
        val attachedTo = attachedToComponent?.targetId

        // Compute what's attached to this card
        val attachments = state.getBattlefield().filter { otherId ->
            state.getEntity(otherId)?.get<AttachedToComponent>()?.targetId == entityId
        }

        // Surface colours granted to this permanent by an attached "choose a colour" aura
        // (Shimmerwilds Growth: "Enchanted land is the chosen color"). The chosen colour is
        // stored on the *aura's* CastChoicesComponent, and the aura sits hidden behind its host,
        // so without this the host shows no sign of the colour it has become. We only surface a
        // colour from auras that actually grant the chosen colour to their host (they carry a
        // GrantChosenColor static ability), so an aura that picks a colour for some other reason
        // doesn't paint a misleading pip on the host.
        val grantedColors = attachments.mapNotNull { auraId ->
            val auraContainer = state.getEntity(auraId) ?: return@mapNotNull null
            val grantsColor = auraContainer.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.script?.staticAbilities?.any { it is GrantChosenColor } == true
            if (grantsColor) auraContainer.chosenColor() else null
        }.toSet()

        // Get linked exile (cards exiled by this permanent, e.g., Suspension Field)
        val linkedExile = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList()

        // Check if token
        val isToken = container.has<TokenComponent>()

        // Commander flag — surfaced to the client so the UI can render a crown / gold border on
        // the commander even after it lands on the battlefield (where the gold halo on the
        // command-zone widget no longer shows). Token copies never carry CommanderComponent
        // (CR 903.10a) so this is naturally false on token clones.
        val isCommander = container.has<com.wingedsheep.engine.state.components.identity.CommanderComponent>()

        // Ring-bearer flag — surfaced so the UI can render a prominent golden Ring icon on the
        // creature a player designated as their Ring-bearer (CR 701.54). The designation is stripped
        // on a real control change (see RingBearerComponent), so the presence of the component is
        // enough — a stolen permanent or token copy never falsely carries it.
        val isRingBearer = container.has<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()

        // Soulbond partner (CR 702.95b) — surfaced so the UI can draw the bond between the two
        // paired battlefield slots. Read through SoulbondPairing so a pair that has broken but not
        // yet been tidied up by `SoulbondPairingCheck` never surfaces a bond to a departed creature.
        val pairedWithId = com.wingedsheep.engine.mechanics.SoulbondPairing.partnerOf(state, entityId)

        // Get targets for spells/abilities on stack (for targeting arrows)
        val targetsComponent = container.get<TargetsComponent>()
        val targets = targetsComponent?.targets?.mapNotNull { chosenTarget ->
            when (chosenTarget) {
                is ChosenTarget.Player -> ClientChosenTarget.Player(chosenTarget.playerId)
                is ChosenTarget.Permanent -> ClientChosenTarget.Permanent(chosenTarget.entityId)
                is ChosenTarget.Spell -> ClientChosenTarget.Spell(chosenTarget.spellEntityId)
                is ChosenTarget.Card -> ClientChosenTarget.Card(chosenTarget.cardId)
            }
        } ?: emptyList()

        // Per-mode breakdown for modal spells whose modes/targets were chosen at cast time (700.2).
        // Opponents see the same data so they can respond with counterspells knowing what's coming.
        val modalEffectForStack = (cardDef?.script?.spellEffect as? ModalEffect)
        val chosenModeDescriptions: List<String> = if (
            zoneKey.zoneType == Zone.STACK &&
            spellOnStack != null &&
            modalEffectForStack != null &&
            spellOnStack.chosenModes.isNotEmpty()
        ) {
            buildChosenModeDescriptions(state, entityId, spellOnStack, modalEffectForStack)
        } else emptyList()
        val perModeTargets: List<ClientPerModeTargetGroup> = if (
            zoneKey.zoneType == Zone.STACK &&
            spellOnStack != null &&
            spellOnStack.chosenModes.isNotEmpty()
        ) {
            buildPerModeTargetGroups(
                state,
                spellOnStack.chosenModes,
                spellOnStack.modeTargetsOrdered,
                chosenModeDescriptions,
                viewingPlayerId
            )
        } else emptyList()

        // Name the optional additional cost a spell on the stack declared ("Kicked", "Bargained",
        // "Offspring"), so opponents can see at a glance which branch is coming on resolution. The
        // label is derived server-side from the keyword's printed prefix — the client renders the
        // badge verbatim rather than mapping slots to words itself.
        val optionalCostLabel = spellOnStack?.declaredCostSlot?.let { slot ->
            val declared = cardDef?.keywordAbilities
                ?.filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost>()
                ?.firstOrNull { it.declaredSlot == slot }
            when {
                declared?.keyword == Keyword.OFFSPRING -> "Offspring"
                slot == com.wingedsheep.sdk.scripting.ChoiceSlot.BARGAINED -> "Bargained"
                slot == com.wingedsheep.sdk.scripting.ChoiceSlot.KICKED -> "Kicked"
                // Teamwork prints its N, so the badge is the keyword's own prefix ("Teamwork 2")
                // rather than the bare slot name (CR 702.194b — "cast using teamwork").
                slot == com.wingedsheep.sdk.scripting.ChoiceSlot.TEAMWORK ->
                    declared?.displayPrefix ?: "Teamwork"
                else -> slot.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        // Name how the spell got onto the stack ("Disturb · Graveyard") so a cast from anywhere but
        // hand doesn't read as a cast out of hand. Same server-side-naming rule as the label above.
        val castProvenanceLabel = spellOnStack?.let {
            CastProvenance.badgeLabel(it.alternativeCost, it.castFromZone)
        }

        // An alternative cost replaces the printed cost, so the printed pips explain nothing about
        // what this cast actually took: name the body it ate and the mana that left the pool. Scoped
        // to alternative-cost casts on purpose — for a normal cast both are already inferable from
        // the card, and every spell would grow two badges for nothing. Emerge (CR 702.119a) is the
        // case that needs it: the sacrifice is *why* the cost shrank.
        val alternativeCostSpell = spellOnStack?.takeIf { it.alternativeCost != null }
        val costSacrificeLabel = alternativeCostSpell?.let {
            CastProvenance.sacrificeLabel(it.sacrificedPermanents.mapNotNull { snapshot -> snapshot.name })
        }
        val manaPaidCost = alternativeCostSpell?.let {
            CastProvenance.paidManaCost(
                white = it.manaSpentWhite,
                blue = it.manaSpentBlue,
                black = it.manaSpentBlack,
                red = it.manaSpentRed,
                green = it.manaSpentGreen,
                colorless = it.manaSpentColorless,
            )
        }

        // Surface whether the optional Blight additional cost was paid (Lorwyn Eclipsed)
        // so opponents can see at a glance that a stronger effect is incoming on resolution.
        val wasBlightPaid = spellOnStack?.wasBlightPaid ?: false

        // Detect whether this spell promised a gift (Bloomburrow gift mechanic).
        // Permanent spells carry the promise as the gift additional cost elected while casting
        // (CR 702.174a — `giftRecipient`); instants and sorceries model it as a modal choice whose
        // "promise" mode's effect tree contains GiftGivenEffect. Surface either to opponents so
        // they can see at a glance that a gift is coming on resolution, rather than having to parse
        // the mode description.
        val giftPromised = spellOnStack?.let { comp ->
            if (comp.giftRecipient != null) return@let true
            if (comp.chosenModes.isEmpty()) return@let false
            val spellEffect = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script?.spellEffect
                ?: return@let false
            val modal = spellEffect as? ModalEffect ?: return@let false
            comp.chosenModes.any { idx ->
                val mode = modal.modes.getOrNull(idx) ?: return@any false
                effectTreeContainsGift(mode.effect)
            }
        } ?: false

        // Get chosen X value for spells on the stack
        val chosenX = spellOnStack?.xValue

        // Get chosen creature type for "as enters" permanents (e.g., Doom Cannon) or spells on stack (e.g., Aphetto Dredging).
        // Note: temporary type changes from floating SetCreatureSubtypes effects (e.g., Mistform Wall, Figure of Fable)
        // are surfaced via the "type-change" active-effect badge in buildCardActiveEffects, not as a chosen-type label.
        val chosenCreatureType = container.chosenCreatureType()
            ?: spellOnStack?.chosenCreatureType

        // Get chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator)
        val chosenColor = container.chosenColor()?.displayName

        // Get chosen mode label for "as enters, choose X or Y" permanents (e.g., Outpost Siege).
        // Resolve the stored mode id back to the display label declared in the card's
        // EntersWithChoice(modeOptions = [...]) so the UI can show the human-friendly name.
        // Riot's counter/haste mode is a one-time enters-with choice, not an ongoing mode — its
        // effect is already visible as a +1/+1 counter or the haste keyword — so it gets no badge.
        val chosenMode = container
            .chosenModeId()
            ?.takeUnless { it == RIOT_MODE_COUNTER || it == RIOT_MODE_HASTE }
            ?.let { modeId ->
                val modeOptions = cardDef?.script?.replacementEffects
                    ?.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithChoice>()
                    ?.firstOrNull { it.choiceType == com.wingedsheep.sdk.scripting.ChoiceType.MODE }
                    ?.modeOptions
                    .orEmpty()
                modeOptions.firstOrNull { it.id == modeId }?.label ?: modeId
            }

        // Get chosen card name for "as enters, choose a card name" permanents (e.g., Petrified Hamlet)
        val chosenCardName = container.chosenCardName()

        // Get chosen card type for "choose a card type" permanents (e.g., Arachne, Psionic Weaver)
        val chosenCardType = container.chosenCardType()

        // Get sacrificed creature types for spells with sacrifice-as-cost (e.g., Endemic Plague)
        val sacrificedCreatureTypes = spellOnStack?.sacrificedPermanents
            ?.flatMap { it.subtypes }?.toSet()
            ?.takeIf { it.isNotEmpty() }

        // A spell cast as a non-permanent secondary face (an Omen, an Adventure, or a split half) is
        // — while it sits on the stack — that face, not the card's default permanent characteristics.
        // Per [com.wingedsheep.sdk.model.CardLayout.OMEN]: "from every zone other than the stack the
        // card is just the Dragon"; on the stack it's the Omen spell (e.g. Petty Revenge). Without
        // this, casting the Omen showed the Dragon's name/type/text/P-T on the stack. We only swap in
        // spell faces (instant/sorcery) — a modal-DFC permanent back keeps its own handling.
        val castFace = if (zoneKey.zoneType == Zone.STACK) {
            spellOnStack?.faceIndex
                ?.let { cardDef?.cardFaces?.getOrNull(it) }
                ?.takeIf { !it.typeLine.isPermanent }
        } else null

        // Build type line string from TypeLine, using projected types/subtypes if available
        val typeLine = castFace?.typeLine ?: cardComponent.typeLine
        val projectedSubtypes = projectedValues?.subtypes?.toList()
        val displaySubtypes = projectedSubtypes ?: typeLine.subtypes.map { it.value }
        // When the projected subtypes contain every creature type — either via CHANGELING
        // (natively or granted) or via "is all creature types" (Stalactite Dagger) —
        // listing them all in the type line bloats it to ~150 entries. Render the base
        // subtypes instead. The CHANGELING badge (if any) or the source's static ability
        // already conveys "every creature type" to the player. The DTO `subtypes` field
        // still carries the full projected set for any client-side filtering.
        val hasAllCreatureTypes = projectedSubtypes != null && hasEveryCreatureType(projectedSubtypes)
        val typeLineSubtypes = if (rawKeywords.contains(Keyword.CHANGELING) || hasAllCreatureTypes) {
            typeLine.subtypes.map { it.value }
        } else {
            displaySubtypes
        }
        val projectedTypes = projectedValues?.types
        val displayCardTypes = if (projectedTypes != null) {
            projectedTypes.mapNotNull { try { CardType.valueOf(it) } catch (_: Exception) { null } }
        } else {
            typeLine.cardTypes.toList()
        }
        // Supertypes share the projected `types` set with card types and subtypes (see
        // StateProjector.extractTypes), so a granted supertype — Origin of Spider-Man's "it becomes
        // a legendary Spider Hero", the Ring emblem's "your Ring-bearer is legendary" (CR 701.54c) —
        // only reaches the client if we read them from the projection too. Reading base
        // `typeLine.supertypes` dropped them: they're not a CardType, so `displayCardTypes` filters
        // them out as well and "Legendary" vanished from the rendered type line entirely.
        val displaySupertypes = if (projectedTypes != null) {
            Supertype.fromProjectedTypes(projectedTypes)
        } else {
            typeLine.supertypes.toList()
        }
        val typeLineParts = mutableListOf<String>()
        if (displaySupertypes.isNotEmpty()) {
            typeLineParts.add(displaySupertypes.joinToString(" ") { it.displayName })
        }
        typeLineParts.add(displayCardTypes.joinToString(" ") { it.displayName })
        val typeLineString = if (typeLineSubtypes.isNotEmpty()) {
            "${typeLineParts.joinToString(" ")} — ${typeLineSubtypes.joinToString(" ")}"
        } else {
            typeLineParts.joinToString(" ")
        }

        // Build active effects from floating effects
        val activeEffects = buildCardActiveEffects(state, entityId, projectedState) +
            notedCreatureTypeBadges(container, viewingPlayerId, isSpectator)

        // Check if this card is playable from exile (impulse draw like Mind's Desire,
        // or cast-from-linked-exile like Rona / Dawnhand Dissident).
        val mayPlayFromExile = state.hasMayPlayFor(entityId, viewingPlayerId, conditionEvaluator, cardRegistry)
        val playableFromExile = zoneKey.zoneType == Zone.EXILE && (
            mayPlayFromExile || isCastableFromLinkedExile(state, viewingPlayerId, entityId, container)
        )

        // Plotted cards (CR 718) sit face-up in exile with a PlottedComponent; surface a flag so the
        // client can badge them as plotted (otherwise indistinguishable from any other exiled card).
        val isPlotted = zoneKey.zoneType == Zone.EXILE && container.has<PlottedComponent>()

        // Active paradigm cards (Secrets of Strixhaven) sit face-up in exile with a ParadigmComponent,
        // recasting a free copy of themselves each precombat main; surface a flag so the client can show
        // them in a dedicated public pile (otherwise indistinguishable from any other exiled card).
        val isParadigm = zoneKey.zoneType == Zone.EXILE &&
            container.has<com.wingedsheep.engine.state.components.battlefield.ParadigmComponent>()

        // Suspended cards (CR 702.62) sit face-up in exile with a SuspendedComponent, counting down at
        // the owner's upkeep; surface a flag so the client can show them in a dedicated public pile
        // (otherwise indistinguishable from any other exiled card). CR 702.62b: "suspended" also
        // requires at least one time counter — the marker alone lingers after the owner declines the
        // free cast at zero counters (see SuspendCardFromHandHandler), and that leftover shouldn't
        // read as an active countdown.
        val isSuspended = zoneKey.zoneType == Zone.EXILE &&
            container.has<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>() &&
            (container.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?.getCount(CounterType.TIME) ?: 0) > 0

        // Prepared permanents (Secrets of Strixhaven) carry a PreparedComponent while a copy of their
        // prepare spell waits castable in exile; surface a flag so the client can badge the creature.
        val isPrepared = zoneKey.zoneType == Zone.BATTLEFIELD &&
            container.has<com.wingedsheep.engine.state.components.battlefield.PreparedComponent>()

        // The exiled prepare-spell copy carries a PreparedSpellCopyComponent. It surfaces as a castable
        // ghost card in the controller's hand; flag it so the client can badge it as coming from a
        // prepared creature (rather than reading like a generic impulse-draw exile card).
        val isPreparedSpell = zoneKey.zoneType == Zone.EXILE &&
            container.has<com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent>()

        // Warped permanents (CR 702.185, Edge of Eternities) carry a WarpedComponent until they're
        // exiled at the next end step; surface a flag so the client can show the cosmic warp cue.
        val isWarped = zoneKey.zoneType == Zone.BATTLEFIELD &&
            container.has<com.wingedsheep.engine.state.components.battlefield.WarpedComponent>()

        // Dashed permanents (CR 702.109, Khans of Tarkir) carry a DashedComponent until they're
        // returned to hand at the next end step; surface a flag so the client can show a dash cue.
        val isDashed = zoneKey.zoneType == Zone.BATTLEFIELD &&
            container.has<com.wingedsheep.engine.state.components.battlefield.DashedComponent>()

        // Mounts (CR 702.171): surface the printed Saddle N and whether the permanent currently
        // carries the saddled designation, so the client can badge both states. Read from the card
        // definition for the same reason `SaddleEnumerator` does — the handler resolves the saddle
        // keyword by definition id, so a renamed copy (CR 707.9) still saddles.
        val saddleRequirement = if (zoneKey.zoneType == Zone.BATTLEFIELD) {
            cardDef?.keywordAbilities
                ?.filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Numeric>()
                ?.firstOrNull { it.keyword == com.wingedsheep.sdk.core.Keyword.SADDLE }
                ?.n
        } else {
            null
        }
        val isSaddled = zoneKey.zoneType == Zone.BATTLEFIELD &&
            container.has<com.wingedsheep.engine.state.components.battlefield.SaddledComponent>()

        // Threshold-style progress badge: detect static abilities gated on
        // "controller's graveyard has at least N cards".
        val thresholdInfo = cardDef?.let { def ->
            findGraveyardThreshold(def)?.let { required ->
                val current = state.getGraveyard(controllerId).size
                ClientThresholdInfo(
                    current = current,
                    required = required,
                    active = current >= required
                )
            }
        }

        // Delirium progress badge: shown only on cards whose definition actually gates on
        // delirium ("four or more card types among cards in your graveyard"), detected by
        // walking the card's serialized tree so the badge appears wherever delirium lives
        // (static/triggered/activated ability, spell effect, cost reduction, replacement).
        // Unlike threshold, this counts distinct card types, not raw graveyard size.
        val deliriumInfo = cardDef?.let { def ->
            findDeliriumThreshold(def)?.let { required ->
                val current = distinctGraveyardCardTypes(state, controllerId)
                ClientDeliriumInfo(
                    current = current,
                    required = required,
                    active = current >= required
                )
            }
        }

        // Modal DFC (CR 712) back face for display/flip preview. A *spell* back lives in
        // `cardFaces` (Flamescroll Celebrant // Revel in Silence) and is picked up here; a
        // *permanent* back lives in `backFace` (the MSH hero cycle) and is picked up by
        // `dfcBackFace` below, which is also what the transform machinery reads.
        val modalBackFace = if (cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.MODAL_DFC) {
            cardDef.cardFaces.firstOrNull()
        } else null

        return ClientCard(
            id = entityId,
            // A Layer-3 SetName continuous effect (Witness Protection's TransformPermanent
            // setName) overwrites the displayed name, mirroring how subtypes/types above
            // already prefer the projected value over the base CardComponent. A non-permanent
            // cast face (Omen/Adventure/split half) on the stack still wins, since it has no
            // battlefield projection entry to overwrite.
            name = castFace?.name ?: projectedValues?.name ?: cardComponent.name,
            manaCost = (castFace?.manaCost ?: cardComponent.manaCost).toString(),
            manaValue = (castFace?.manaCost ?: cardComponent.manaCost).cmc,
            typeLine = typeLineString,
            cardTypes = displayCardTypes.map { it.name }.toSet(),
            subtypes = displaySubtypes.toSet(),
            colors = if (castFace != null) castFace.manaCost.colors else colors,
            grantedColors = grantedColors,
            oracleText = castFace?.oracleText ?: cardComponent.oracleText,
            // A non-permanent cast face (Omen/Adventure/split half) has no power/toughness.
            power = if (castFace != null) null else power,
            toughness = if (castFace != null) null else toughness,
            basePower = cardComponent.baseStats?.basePower,
            baseToughness = cardComponent.baseStats?.baseToughness,
            damage = damage,
            keywords = keywords,
            abilityFlags = abilityFlags,
            protections = protections,
            hexproofFromColors = hexproofFromColors,
            hexproofFromMonocolored = hexproofFromMonocolored,
            hexproofFromMulticolored = hexproofFromMulticolored,
            counters = counters,
            isTapped = isTapped,
            isExerted = isExerted,
            hasSummoningSickness = hasSummoningSickness,
            isTransformed = false, // TODO: Add transformed support
            isPhasedOut = isPhasedOut,
            isAttacking = isAttacking,
            isBlocking = isBlocking,
            attackingTarget = attackingTarget,
            blockingTarget = blockingTarget,
            controllerId = controllerId,
            // A battle's protector (CR 310.9). Absent on every other permanent.
            protectorId = container.get<com.wingedsheep.engine.state.components.battlefield.ProtectorComponent>()
                ?.playerId,
            ownerId = ownerId,
            isToken = isToken,
            isCommander = isCommander,
            isRingBearer = isRingBearer,
            pairedWithId = pairedWithId,
            zone = zoneKey,
            attachedTo = attachedTo,
            attachments = attachments,
            linkedExile = linkedExile,
            isFaceDown = isFaceDown,
            faceDownMode = if (isFaceDown) faceDownDisplayMode?.name else null,
            isSuspected = projectedValues?.isSuspected == true,
            isSolved = container.has<com.wingedsheep.engine.state.components.battlefield.SolvedComponent>(),
            saddleRequirement = saddleRequirement,
            isSaddled = isSaddled,
            isPlotted = isPlotted,
            isParadigm = isParadigm,
            isSuspended = isSuspended,
            isPrepared = isPrepared,
            isPreparedSpell = isPreparedSpell,
            isWarped = isWarped,
            isDashed = isDashed,
            morphCost = if (isFaceDown && morphData != null) morphData.morphCost.description else null,
            targets = targets,
            imageUri = state.imageOverrideFor(entityId)
                ?: cardDef?.metadata?.imageUriByCreatureSubtype
                    ?.entries
                    ?.firstOrNull { (subtype) -> subtype in displaySubtypes }
                    ?.value
                ?: cardComponent.imageUri
                ?: cardDef?.metadata?.imageUri,
            imageRotation = cardDef?.metadata?.imageRotation ?: 0,
            activeEffects = activeEffects,
            rulings = cardDef?.metadata?.rulings?.map {
                ClientRuling(date = it.date, text = it.text)
            } ?: emptyList(),
            optionalCostLabel = optionalCostLabel,
            castProvenanceLabel = castProvenanceLabel,
            costSacrificeLabel = costSacrificeLabel,
            manaPaidCost = manaPaidCost,
            giftPromised = giftPromised,
            wasBlightPaid = wasBlightPaid,
            chosenX = chosenX,
            chosenCreatureType = chosenCreatureType,
            chosenColor = chosenColor,
            chosenMode = chosenMode,
            chosenCardName = chosenCardName,
            chosenCardType = chosenCardType,
            sacrificedCreatureTypes = sacrificedCreatureTypes,
            playableFromExile = playableFromExile,
            copyOf = container.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()?.let { copyComp ->
                cardRegistry.getCard(copyComp.originalCardDefinitionId)?.name
            },
            // The two legendary flags are complements, and the `!in displaySupertypes` clause is what
            // makes them so: a non-legendary copy that an effect then makes legendary again (Impostor
            // Syndrome's copy designated Ring-bearer) is simply legendary, so "not legendary" is a lie
            // about it and the client would otherwise badge it both ways at once.
            nonLegendaryCopy = zoneKey.zoneType == Zone.BATTLEFIELD
                && cardDef != null
                && Supertype.LEGENDARY in cardDef.typeLine.supertypes
                && Supertype.LEGENDARY !in cardComponent.typeLine.supertypes
                && Supertype.LEGENDARY !in displaySupertypes,
            legendaryByEffect = zoneKey.zoneType == Zone.BATTLEFIELD
                && Supertype.LEGENDARY in displaySupertypes
                && Supertype.LEGENDARY !in cardComponent.typeLine.supertypes,
            // Same projected-minus-printed shape as legendaryByEffect. Skipped for the
            // all-creature-types case, which typeLineSubtypes already collapses above.
            grantedSubtypes = if (zoneKey.zoneType == Zone.BATTLEFIELD && !hasAllCreatureTypes) {
                val printed = cardComponent.typeLine.subtypes.map { it.value }.toSet()
                // Subtypes a *floating* effect is responsible for already have their own badge —
                // "+Hero" for AddSubtype (`type_added`) and the full list for SetCreatureSubtypes
                // (`type_changed`), both built below from those effects directly. Repeating them
                // here would show the same grant twice in the preview. What this field is actually
                // for is the case those badges miss: a grant from a continuous *static* ability,
                // such as an Aura's "is a legendary Soldier in addition to its other types".
                val alreadyBadged = state.floatingEffects
                    .filter { entityId in it.effect.affectedEntities }
                    .flatMap {
                        when (val mod = it.effect.modification) {
                            is SerializableModification.AddSubtype -> listOf(mod.subtype)
                            is SerializableModification.SetCreatureSubtypes -> displaySubtypes
                            else -> emptyList()
                        }
                    }
                    .toSet()
                displaySubtypes.filterNot { it in printed || it in alreadyBadged }.toSet()
            } else emptySet(),
            grantedCardTypes = if (zoneKey.zoneType == Zone.BATTLEFIELD) {
                val printed = cardComponent.typeLine.cardTypes.map { it.name }.toSet()
                displayCardTypes.map { it.name }.filterNot { it in printed }.toSet()
            } else emptySet(),
            damageDistribution = (spellOnStack?.damageDistribution ?: container.get<TriggeredAbilityOnStackComponent>()?.damageDistribution)?.takeIf { it.isNotEmpty() },
            sagaTotalChapters = cardDef?.finalChapter,
            classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel,
            classMaxLevel = cardDef?.maxClassLevel,
            thresholdInfo = thresholdInfo,
            deliriumInfo = deliriumInfo,
            stackText = if (zoneKey.zoneType == Zone.STACK && spellOnStack != null && cardDef != null) {
                when {
                    spellOnStack.castFaceDown -> "Cast as a face-down 2/2 creature"
                    // Instants/sorceries always show their spell effect description
                    cardDef.typeLine.cardTypes.any { it == CardType.INSTANT || it == CardType.SORCERY } ->
                        runtimeStackText(state, entityId, spellOnStack, cardDef)
                    // Permanents only show text when ambiguous (card has alternate cast modes like cycling or morph)
                    cardDef.keywordAbilities.any { it is KeywordAbility.Cycling || it is KeywordAbility.Morph } ->
                        runtimeStackText(state, entityId, spellOnStack, cardDef) ?: cardComponent.oracleText
                    // Unambiguous permanent cast — no text needed
                    else -> null
                }
            } else null,
            chosenModeDescriptions = chosenModeDescriptions,
            perModeTargets = perModeTargets,
            // A modal DFC with a *spell* back (CR 712) keeps it in `cardFaces`, so the SDK
            // `isDoubleFaced` (transform machinery) stays false — surface it to the client as
            // double-faced for display/flip-preview only. One with a *permanent* back already
            // reports `isDoubleFaced`, since that back is reachable by transform too (CR 712.3).
            isDoubleFaced = container.has<com.wingedsheep.engine.state.components.identity.DoubleFacedComponent>() || cardDef?.isDoubleFaced == true || modalBackFace != null,
            currentFace = container.get<com.wingedsheep.engine.state.components.identity.DoubleFacedComponent>()?.currentFace?.name
                ?: if (cardDef?.isDoubleFaced == true || modalBackFace != null) "FRONT" else null,
            backFaceName = dfcBackFace(container, cardDef)?.name ?: modalBackFace?.name,
            backFaceTypeLine = dfcBackFace(container, cardDef)?.typeLine?.toString() ?: modalBackFace?.typeLine?.toString(),
            backFaceOracleText = dfcBackFace(container, cardDef)?.oracleText ?: modalBackFace?.oracleText,
            backFaceImageUri = cardComponent.backFaceImageUri ?: dfcBackFace(container, cardDef)?.metadata?.imageUri ?: modalBackFace?.imageUri,
            planeswalkerAbilities = buildPlaneswalkerAbilities(cardDef, zoneKey),
            isRoom = cardDef?.isRoom == true,
            // `cardDef` already tracks the *displayed* face of a DFC (dfcBackFace resolves the
            // other one relative to DoubleFacedComponent.currentFace), so a defeated Siege recast
            // as its portrait back face correctly stops reporting landscape.
            isLandscapeFace = cardDef?.isLandscapePrint == true,
            // Only transforming DFCs can pair a landscape face with a portrait one; a modal DFC's
            // other half lives in `cardFaces` under the MODAL_DFC layout, which is portrait on both
            // sides, so it never contributes here.
            backFaceIsLandscape = dfcBackFace(container, cardDef)?.isLandscapePrint == true,
            cardFaces = buildClientCardFaces(container, cardDef),
            castFaceIndex = spellOnStack?.faceIndex,
            // Impending (CR 702.176): expose the reduced cost + time-counter count so the client can
            // always present the impending cast option (graying it out when unaffordable). Intrinsic
            // to the card definition, so it's surfaced in every zone.
            impending = cardDef?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.Impending>()
                ?.firstOrNull()
                ?.let { ClientImpending(cost = it.cost.toString(), time = it.time) }
        )
    }

    /**
     * Build per-face DTOs for split-layout cards (currently Rooms). Returns an empty list for
     * normal single-face cards. The [ClientCardFace.isUnlocked] flag reflects the live
     * [RoomComponent] state for battlefield permanents and is `false` everywhere else (since
     * lock state only exists once the Room is on the battlefield, per CR 709.5d).
     */
    private fun buildClientCardFaces(
        container: com.wingedsheep.engine.state.ComponentContainer,
        cardDef: CardDefinition?
    ): List<ClientCardFace> {
        if (cardDef == null || cardDef.layout != com.wingedsheep.sdk.model.CardLayout.SPLIT) return emptyList()
        if (cardDef.cardFaces.isEmpty()) return emptyList()
        val roomComp = container.get<com.wingedsheep.engine.state.components.identity.RoomComponent>()
        return cardDef.cardFaces.map { face ->
            val faceIdValue = face.name
            val isUnlocked = roomComp?.unlocked?.any { it.value == faceIdValue } ?: false
            ClientCardFace(
                faceId = faceIdValue,
                name = face.name,
                manaCost = face.manaCost.toString(),
                typeLine = face.typeLine.toString(),
                oracleText = face.oracleText,
                isUnlocked = isUnlocked
            )
        }
    }

    private fun buildPlaneswalkerAbilities(
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        zoneKey: ZoneKey
    ): List<ClientPlaneswalkerAbility>? {
        if (cardDef == null) return null
        if (zoneKey.zoneType != Zone.BATTLEFIELD) return null
        if (!cardDef.typeLine.cardTypes.contains(CardType.PLANESWALKER)) return null
        val abilities = cardDef.script.activatedAbilities.filter { it.isPlaneswalkerAbility }
        if (abilities.isEmpty()) return null
        // Consumed in order, so two abilities sharing a loyalty cost (Garruk Relentless's two
        // 0-cost abilities) each take their own oracle line instead of both echoing the first.
        val oracleDescriptions = parseOracleLoyaltyLines(cardDef.oracleText)
            .mapValues { (_, lines) -> ArrayDeque(lines) }
        return abilities.mapNotNull { ability ->
            val loyalty = (ability.cost as? com.wingedsheep.sdk.scripting.AbilityCost.Loyalty)?.change
                ?: return@mapNotNull null
            val description = oracleDescriptions[loyalty]?.removeFirstOrNull()
                ?: ability.descriptionOverride
                ?: effectDisplayText(ability.effect, "this planeswalker")
            ClientPlaneswalkerAbility(
                abilityId = ability.id.value,
                loyaltyChange = loyalty,
                description = description
            )
        }
    }

    /**
     * Parse a planeswalker's oracle text into a map of loyalty change → ability texts, in the order
     * the lines appear. Example line: "−2: Ajani deals 4 damage to target tapped creature."
     * Handles the Unicode minus (U+2212), the ASCII hyphen, and a leading "+".
     *
     * A loyalty cost maps to a *list* because it isn't unique: Garruk Relentless has two 0-cost
     * abilities, Vivien, Monsters' Advocate two −2s. Keying one text per cost gave every ability
     * sharing that cost the first line's text, so the card showed the same ability twice.
     */
    private fun parseOracleLoyaltyLines(oracleText: String): Map<Int, List<String>> {
        if (oracleText.isBlank()) return emptyMap()
        val pattern = Regex("""^\s*([+−\-]?)(\d+):\s*(.+?)\s*$""")
        val result = mutableMapOf<Int, MutableList<String>>()
        for (raw in oracleText.lines()) {
            val match = pattern.matchEntire(raw) ?: continue
            val sign = match.groupValues[1]
            val magnitude = match.groupValues[2].toIntOrNull() ?: continue
            val loyalty = if (sign == "−" || sign == "-") -magnitude else magnitude
            val text = match.groupValues[3].trimEnd('.', ' ')
            result.getOrPut(loyalty) { mutableListOf() }.add(text)
        }
        return result
    }

    /**
     * Resolve the back face [CardDefinition] for a DFC client DTO. Looks at the permanent's
     * [com.wingedsheep.engine.state.components.identity.DoubleFacedComponent] first (so a
     * transformed permanent still exposes its other face), and falls back to the card
     * definition's `backFace` pointer for cards not currently on the battlefield.
     */
    private fun dfcBackFace(
        container: com.wingedsheep.engine.state.ComponentContainer,
        cardDef: CardDefinition?
    ): CardDefinition? {
        val dfc = container.get<com.wingedsheep.engine.state.components.identity.DoubleFacedComponent>()
        if (dfc != null) {
            val otherId = when (dfc.currentFace) {
                com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.FRONT -> dfc.backCardDefinitionId
                com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.BACK -> dfc.frontCardDefinitionId
            }
            cardRegistry.getCard(otherId)?.let { return it }
        }
        return cardDef?.backFace
    }

    private fun effectTreeContainsGift(effect: Effect): Boolean = when (effect) {
        is GiftGivenEffect -> true
        is CompositeEffect -> effect.effects.any { effectTreeContainsGift(it) }
        is GatedEffect -> effectTreeContainsGift(effect.then) ||
            (effect.otherwise?.let { effectTreeContainsGift(it) } ?: false)
        is ModalEffect -> effect.modes.any { effectTreeContainsGift(it.effect) }
        else -> false
    }

    /**
     * Generate stack text with dynamic amounts evaluated to concrete values.
     * Falls back to static description if evaluation fails.
     */
    private fun runtimeStackText(
        state: GameState,
        spellEntityId: EntityId,
        spellOnStack: SpellOnStackComponent,
        cardDef: CardDefinition
    ): String? {
        val effect = cardDef.script.spellEffect ?: return null

        // For modal spells with modes chosen at cast time, concatenate all chosen mode
        // descriptions (choose-N commands show every picked mode, in order, one per line).
        if (spellOnStack.chosenModes.isNotEmpty() && effect is ModalEffect) {
            val descriptions = buildChosenModeDescriptions(state, spellEntityId, spellOnStack, effect)
            if (descriptions.isNotEmpty()) {
                return descriptions.joinToString("\n")
            }
        }

        return try {
            val evaluator = DynamicAmountEvaluator()
            val chosenTargets = state.getEntity(spellEntityId)
                ?.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()
                ?.targets
                ?: emptyList()
            val context = EffectContext(
                sourceId = spellEntityId,
                controllerId = spellOnStack.casterId,
                xValue = spellOnStack.xValue,
                declaredCostSlot = spellOnStack.declaredCostSlot,
                wasBlightPaid = spellOnStack.wasBlightPaid,
                sacrificedPermanents = spellOnStack.sacrificedPermanents,
                chosenEntitySnapshots = spellOnStack.chosenEntitySnapshots,
                exiledCardCount = spellOnStack.exiledCardCount,
                additionalCostBlightAmount = spellOnStack.additionalCostBlightAmount,
                targets = chosenTargets,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(
                    storedCollections = com.wingedsheep.engine.mechanics.stack.buildBeheldStoredCollections(
                        spellOnStack.beheldCards, cardDef
                    )
                )
            )
            // Resolve ConditionalEffect at stack-time: opponents see only the branch that
            // will fire (e.g., Cinder Strike shows "deals 4 damage" vs "deals 2 damage"
            // depending on whether the optional Blight cost was paid) instead of the full
            // "if X, do Y. Otherwise, do Z." description.
            val resolvedEffect = resolveConditionalForStack(state, effect, context)
            resolvedEffect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
        } catch (_: Exception) {
            effect.description
        }
    }

    /**
     * Recursively replace [ConditionalEffect]s in [effect] with the branch the spell will
     * actually take, using [context] to evaluate each condition. Composite branches are
     * resolved one level deep so nested conditions also collapse. Conditions that depend
     * on state not yet captured at cast time fall through to the original effect.
     */
    private fun resolveConditionalForStack(
        state: GameState,
        effect: com.wingedsheep.sdk.scripting.effects.Effect,
        context: EffectContext
    ): com.wingedsheep.sdk.scripting.effects.Effect {
        effect.asConditional()?.let { conditional ->
            val taken = if (conditionEvaluator.evaluate(state, conditional.condition, context)) {
                conditional.then
            } else {
                conditional.otherwise
            }
            return taken?.let { resolveConditionalForStack(state, it, context) }
                ?: com.wingedsheep.sdk.scripting.effects.CompositeEffect(emptyList())
        }
        return when (effect) {
            is com.wingedsheep.sdk.scripting.effects.CompositeEffect ->
                effect.copy(effects = effect.effects.map { resolveConditionalForStack(state, it, context) })
            else -> effect
        }
    }

    /**
     * Evaluate each chosen mode's runtime description for a modal spell on the stack. Aligned
     * 1:1 with [SpellOnStackComponent.chosenModes]; unknown indices yield "Unknown mode" so the
     * client still sees a placeholder rather than silently dropping entries.
     */
    private fun buildChosenModeDescriptions(
        state: GameState,
        spellEntityId: EntityId,
        spellOnStack: SpellOnStackComponent,
        modal: ModalEffect
    ): List<String> {
        if (spellOnStack.chosenModes.isEmpty()) return emptyList()
        val evaluator = DynamicAmountEvaluator()
        val context = EffectContext(
            sourceId = spellEntityId,
            controllerId = spellOnStack.casterId,
            xValue = spellOnStack.xValue,
            sacrificedPermanents = spellOnStack.sacrificedPermanents,
            exiledCardCount = spellOnStack.exiledCardCount,
            additionalCostBlightAmount = spellOnStack.additionalCostBlightAmount
        )
        return spellOnStack.chosenModes.map { modeIndex ->
            val mode = modal.modes.getOrNull(modeIndex) ?: return@map "Unknown mode"
            try {
                mode.effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
            } catch (_: Exception) {
                mode.description
            }
        }
    }

    /**
     * Build per-mode target groups for a modal spell on the stack, aligned with
     * [SpellOnStackComponent.modeTargetsOrdered]. Hidden-zone targets are redacted to a generic
     * "a card in X's hand/library" string when the viewer does not own the zone.
     */
    private fun buildPerModeTargetGroups(
        state: GameState,
        chosenModes: List<Int>,
        modeTargetsOrdered: List<List<ChosenTarget>>,
        modeDescriptions: List<String>,
        viewingPlayerId: EntityId
    ): List<ClientPerModeTargetGroup> {
        if (chosenModes.isEmpty()) return emptyList()
        return chosenModes.mapIndexed { index, modeIndex ->
            val rawTargets = modeTargetsOrdered.getOrNull(index) ?: emptyList()
            val clientTargets = rawTargets.map { target ->
                when (target) {
                    is ChosenTarget.Player -> ClientChosenTarget.Player(target.playerId)
                    is ChosenTarget.Permanent -> ClientChosenTarget.Permanent(target.entityId)
                    is ChosenTarget.Spell -> ClientChosenTarget.Spell(target.spellEntityId)
                    is ChosenTarget.Card -> ClientChosenTarget.Card(target.cardId)
                }
            }
            val targetNames = rawTargets.map { target ->
                resolveTargetDisplayName(state, target, viewingPlayerId)
            }
            ClientPerModeTargetGroup(
                modeIndex = modeIndex,
                modeDescription = modeDescriptions.getOrNull(index) ?: "",
                targets = clientTargets,
                targetNames = targetNames
            )
        }
    }

    /**
     * Resolve a [ChosenTarget] to a human-readable display name for the stack view. Cards in
     * hidden zones are redacted unless the viewing player owns the zone.
     */
    private fun resolveTargetDisplayName(
        state: GameState,
        target: ChosenTarget,
        viewingPlayerId: EntityId
    ): String = when (target) {
        is ChosenTarget.Player -> state.getEntity(target.playerId)
            ?.get<PlayerComponent>()?.name ?: "a player"
        is ChosenTarget.Permanent -> state.getEntity(target.entityId)
            ?.get<CardComponent>()?.name ?: "a permanent"
        is ChosenTarget.Spell -> state.getEntity(target.spellEntityId)
            ?.get<CardComponent>()?.name ?: "a spell"
        is ChosenTarget.Card -> {
            val hiddenZone = target.zone == Zone.HAND || target.zone == Zone.LIBRARY
            if (hiddenZone && target.ownerId != viewingPlayerId) {
                val ownerName = state.getEntity(target.ownerId)
                    ?.get<PlayerComponent>()?.name ?: "opponent"
                "a card in ${ownerName}'s ${target.zone.name.lowercase()}"
            } else {
                state.getEntity(target.cardId)?.get<CardComponent>()?.name ?: "a card"
            }
        }
    }

    /**
     * Generate runtime text for an activated ability on the stack with dynamic amounts resolved.
     * Mirrors the cost-payment LKI carried on [ActivatedAbilityOnStackComponent] (sacrificed and
     * tapped permanent snapshots, X) into the [EffectContext] so stack text for effects like
     * "draw cards equal to the sacrificed creature's power" renders the actual number instead
     * of falling back to 0. Returns null if evaluation fails or the effect has no dynamic amounts.
     */
    private fun runtimeAbilityText(
        state: GameState,
        abilityEntityId: EntityId,
        activated: ActivatedAbilityOnStackComponent
    ): String? {
        val context = EffectContext(
            sourceId = abilityEntityId,
            controllerId = activated.controllerId,
            targets = chosenTargetsOf(state, abilityEntityId),
            xValue = activated.xValue,
            sacrificedPermanents = activated.sacrificedPermanents,
            tappedPermanents = activated.tappedPermanents,
            tappedEntitySnapshots = activated.tappedEntitySnapshots
        )
        return runtimeAbilityText(state, activated.effect, context)
    }

    /**
     * The targets already chosen for an ability sitting on the stack.
     *
     * An ability's targets are locked in when it's put on the stack, so text rendered for it can
     * read them — "double its power" on a 5/5 should say "+5/+5", not fall back to the amount's
     * wording. The spell path does the same thing; omitting it here left every
     * [com.wingedsheep.sdk.scripting.values.EntityReference.Target]-relative amount undeterminable
     * on the stack, where it is in fact known.
     */
    private fun chosenTargetsOf(state: GameState, abilityEntityId: EntityId) =
        state.getEntity(abilityEntityId)
            ?.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()
            ?.targets
            ?: emptyList()

    /**
     * Generate runtime text for a triggered ability on the stack. Builds an [EffectContext] with
     * the triggering-entity fields populated so dynamic amounts referencing
     * [com.wingedsheep.sdk.scripting.values.EntityReference.Triggering] (e.g., "deals damage equal
     * to its power") render the correct value instead of falling back to 0.
     */
    private fun runtimeAbilityText(
        state: GameState,
        abilityEntityId: EntityId,
        triggered: TriggeredAbilityOnStackComponent
    ): String? = runtimeAbilityText(
        state,
        triggered.effect,
        triggeredAbilityContext(state, abilityEntityId, triggered)
    )

    private fun runtimeAbilityText(state: GameState, effect: Effect, context: EffectContext): String? {
        return try {
            val evaluator = DynamicAmountEvaluator()
            val text = effect.runtimeDescription { amount -> evaluator.evaluateForDisplay(state, amount, context) }
            // Only return if it differs from static description (i.e., dynamic amounts were resolved)
            if (text != effect.description) text else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The noun a [com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN] referring to [entityId]
     * should render as — "this creature" for a creature (incl. artifact/enchantment creatures), else
     * the noun matching its projected card type, falling back to
     * [com.wingedsheep.sdk.scripting.targets.DEFAULT_SELF_NOUN] ("this permanent") for a multi-type,
     * DFC, or off-battlefield permanent whose type we can't pin down. Battlefield type reads go
     * through projected state (Rule 613), never base `typeLine`.
     */
    private fun selfNounFor(state: GameState, entityId: EntityId): String {
        val types = state.projectedState.getTypes(entityId)
        return when {
            "CREATURE" in types -> "this creature"
            "LAND" in types -> "this land"
            "ARTIFACT" in types -> "this artifact"
            "ENCHANTMENT" in types -> "this enchantment"
            "PLANESWALKER" in types -> "this planeswalker"
            "BATTLE" in types -> "this battle"
            else -> com.wingedsheep.sdk.scripting.targets.DEFAULT_SELF_NOUN
        }
    }

    /**
     * Render [effect]'s display text with any self-noun placeholder resolved to [selfNoun] (the noun
     * for the source permanent — see [selfNounFor]). Effects that carry no self-reference fall
     * through to their default-resolved [Effect.description]. This is the type-aware render layer
     * for [com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription]: the SDK emits the
     * placeholder token, the client sees the type-correct noun.
     */
    private fun effectDisplayText(effect: Effect, selfNoun: String): String =
        (effect as? com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription)
            ?.let { com.wingedsheep.sdk.scripting.targets.resolveSelfNoun(it.descriptionTemplate, selfNoun) }
            ?: effect.description

    /**
     * Build an [EffectContext] mirroring how [TriggerProcessor] does at resolution time, so
     * stack-text rendering can evaluate `EntityReference.Triggering`-based dynamic amounts (e.g.,
     * "deals damage equal to its power") with the actual triggering entity instead of a null.
     */
    private fun triggeredAbilityContext(
        state: GameState,
        abilityEntityId: EntityId,
        triggered: TriggeredAbilityOnStackComponent
    ): EffectContext = EffectContext(
        sourceId = abilityEntityId,
        controllerId = triggered.controllerId,
        targets = chosenTargetsOf(state, abilityEntityId),
        xValue = triggered.xValue,
        triggerDamageAmount = triggered.triggerDamageAmount,
        triggerCounterCount = triggered.triggerCounterCount,
        triggerTotalCounterCount = triggered.triggerTotalCounterCount,
        triggerLastKnownCounters = triggered.triggerLastKnownCounters,
        triggerLastKnownDamageDealtByPlayers = triggered.triggerLastKnownDamageDealtByPlayers,
        triggeringEntityId = triggered.triggeringEntityId,
        triggeringPlayerId = triggered.triggeringPlayerId,
        targetingSourceEntityId = triggered.targetingSourceEntityId,
        triggerLastKnownPower = triggered.lastKnownPower,
        triggerLastKnownToughness = triggered.lastKnownToughness,
        triggerDiedBatchTotalPower = triggered.diedBatchTotalPower,
        triggerScryCount = triggered.triggerScryCount,
        triggerDiscardCount = triggered.triggerDiscardCount,
        triggerDiscoverValue = triggered.triggerDiscoverValue,
        triggerExcessDamageAmount = triggered.triggerExcessDamageAmount,
        triggerRecipientToughness = triggered.triggerRecipientToughness,
        triggerManaSpentOnTriggeringSpell = triggered.triggerManaSpentOnTriggeringSpell,
        triggerColorsSpentOnTriggeringSpell = triggered.triggerColorsSpentOnTriggeringSpell,
        triggerManaValueOfTriggeringSpell = triggered.triggerManaValueOfTriggeringSpell,
        triggerXValueOfTriggeringSpell = triggered.triggerXValueOfTriggeringSpell
    )

    /**
     * Transform player information.
     */
    private fun transformPlayer(
        state: GameState,
        playerId: EntityId,
        viewingPlayerId: EntityId
    ): ClientPlayer {
        val container = state.getEntity(playerId)
        val playerComponent = container?.get<PlayerComponent>()
        // CR 810.9a — a player's displayed life is the team's shared total in Two-Headed Giant.
        val displayedLife = if (container?.get<LifeTotalComponent>() != null) state.lifeTotal(playerId) else null
        val landDropsComponent = container?.get<LandDropsComponent>()
        val manaPoolComponent = container?.get<ManaPoolComponent>()

        // Calculate zone sizes
        val handSize = state.getHand(playerId).size
        val librarySize = state.getLibrary(playerId).size
        val graveyardSize = state.getGraveyard(playerId).size
        val exileSize = state.getExile(playerId).size

        // Effective maximum hand size (CR 402.2): 7 by default, smaller/larger when an effect set
        // it, null when unlimited (Reliquary Tower). Shared source of truth with cleanup.
        val maxHandSize = com.wingedsheep.engine.core.MaximumHandSize.effective(
            state, playerId, cardRegistry, conditionEvaluator, dynamicAmountEvaluator
        )

        // Determine lands played this turn
        val landsPlayed = if (landDropsComponent != null) {
            landDropsComponent.maxPerTurn - landDropsComponent.remaining
        } else {
            0
        }

        // A player has lost when the engine has marked them (mid-game elimination in a
        // multiplayer pod — drives the opponent-rail tombstone while the game continues),
        // or at game end when someone else won (2-player degenerate case).
        val hasLost = container?.has<PlayerLostComponent>() == true ||
            (state.gameOver && state.winnerId != null && state.winnerId != playerId)

        // Mana pool is public information in MTG - show for all players
        val manaPool = if (manaPoolComponent != null) {
            ClientManaPool(
                white = manaPoolComponent.white,
                blue = manaPoolComponent.blue,
                black = manaPoolComponent.black,
                red = manaPoolComponent.red,
                green = manaPoolComponent.green,
                colorless = manaPoolComponent.colorless,
                restrictedMana = manaPoolComponent.restrictedMana.map { entry ->
                    val expiryNote = if (entry.expiry == com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT) {
                        "This mana lasts until end of combat, then is lost."
                    } else null
                    ClientRestrictedManaEntry(
                        color = entry.color?.symbol?.toString(),
                        restrictionDescription = listOfNotNull(
                            entry.restriction.description.ifBlank { null },
                            expiryNote
                        ).joinToString(" ")
                    )
                }
            )
        } else {
            null
        }

        // Build active effects list
        val activeEffects = buildActiveEffects(state, playerId, container)

        return ClientPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = displayedLife ?: 20,
            poisonCounters = container?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0,
            handSize = handSize,
            maxHandSize = maxHandSize,
            librarySize = librarySize,
            graveyardSize = graveyardSize,
            exileSize = exileSize,
            landsPlayedThisTurn = landsPlayed,
            hasLost = hasLost,
            manaPool = manaPool,
            activeEffects = activeEffects,
            commanderDamage = buildCommanderDamage(state, playerId),
            // CR 702.179 — public information, and 0 for the overwhelming majority of games.
            speed = state.speed(playerId),
            // CR 107.14 — public information like poison counters, and 0 outside energy decks.
            energyCounters = container?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
        )
    }

    /**
     * Build per-commander damage tallies against [playerId]. Empty when the format has no commanders
     * and for defenders no commander has connected with yet.
     */
    private fun buildCommanderDamage(
        state: GameState,
        playerId: EntityId
    ): List<ClientCommanderDamage> {
        val threshold = state.format.commanderDamageThreshold ?: return emptyList()
        if (state.commanderDamage.isEmpty()) return emptyList()

        return state.commanderDamage
            .asSequence()
            .filter { it.defendingPlayerId == playerId && it.amount > 0 }
            .mapNotNull { entry ->
                val container = state.getEntity(entry.commanderId) ?: return@mapNotNull null
                val card = container.get<CardComponent>() ?: return@mapNotNull null
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: card.ownerId
                    ?: return@mapNotNull null
                ClientCommanderDamage(
                    commanderId = entry.commanderId,
                    commanderName = card.name,
                    controllerId = controllerId,
                    amount = entry.amount,
                    threshold = threshold,
                    imageUri = card.imageUri,
                )
            }
            .sortedByDescending { it.amount }
            .toList()
    }

    /**
     * Build a list of active effects on a player for display as badges.
     */
    private fun buildActiveEffects(
        state: GameState,
        playerId: EntityId,
        container: com.wingedsheep.engine.state.ComponentContainer?
    ): List<ClientPlayerEffect> {
        if (container == null) return emptyList()

        val effects = mutableListOf<ClientPlayerEffect>()

        // Check floating effects for damage prevention shields on this player
        var preventDamageTotal = 0
        var preventsAllDamage = false
        var preventsAllCombatDamage = false
        var preventsAttackingCreatureDamage = false
        val preventedCreatureTypes = mutableSetOf<String>()
        val preventedCombatDamageSources = mutableSetOf<String>()
        val preventedFromSources = mutableSetOf<EntityId>()
        // Single-instance chosen-source shields, kept separate from the all-damage ones above
        // because they read differently: "the next time" rather than "all damage", and Dark Sphere
        // halves rather than prevents. Pair = (source, halved).
        val preventedNextInstanceFromSources = mutableListOf<Pair<EntityId, Boolean>>()
        for (floatingEffect in state.floatingEffects) {
            val modification = floatingEffect.effect.modification
            if (
                modification is SerializableModification.PreventAllDamageToGroup &&
                modification.includesController &&
                floatingEffect.controllerId == playerId
            ) {
                val sourceDescription = modification.sourceFilter?.description
                val scopeDescription = if (modification.combatOnly) "combat damage" else "damage"
                val sourceSuffix = sourceDescription?.let { " from $it" } ?: ""
                effects.add(
                    ClientPlayerEffect(
                        effectId = "prevent_damage_to_controller_${floatingEffect.id.value}",
                        name = "Damage Shield",
                        description = "All $scopeDescription that would be dealt to you$sourceSuffix is prevented",
                        icon = "prevent-damage"
                    )
                )
            }
            // Board-wide combat-damage prevention (Fog, Holy Day, Spore Flower's ability). These
            // shields name neither a permanent nor a player, so they carry no affected entity to
            // hang a card badge on — and without a player badge the only trace that one resolved is
            // a line in the log. They apply to every creature's combat damage, so both players
            // carry the badge.
            when (modification) {
                is SerializableModification.PreventAllCombatDamage -> preventsAllCombatDamage = true
                is SerializableModification.PreventCombatDamageFromGroup ->
                    preventedCombatDamageSources.add(modification.filter.description)
                else -> {}
            }
            if (playerId !in floatingEffect.effect.affectedEntities) continue
            when (modification) {
                is SerializableModification.PreventAllDamageTo -> {
                    preventsAllDamage = true
                }
                // Deep Wood. Unlike the two above this one is scoped to the shield's controller,
                // so it badges only the protected player.
                is SerializableModification.PreventDamageFromAttackingCreatures -> {
                    preventsAttackingCreatureDamage = true
                }
                is SerializableModification.PreventNextDamage -> {
                    preventDamageTotal += modification.remainingAmount
                }
                is SerializableModification.PreventNextDamageFromCreatureType -> {
                    preventedCreatureTypes.add(modification.creatureType)
                }
                is SerializableModification.PreventAllDamageFromSource -> {
                    preventedFromSources.add(modification.damageSourceId)
                }
                is SerializableModification.PreventNextDamageInstanceFromSource -> {
                    preventedNextInstanceFromSources.add(
                        modification.damageSourceId to modification.halveRoundedDown
                    )
                }
                else -> {}
            }
        }
        if (preventsAllDamage) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_all_damage",
                    name = "Prevent All Damage",
                    description = "All damage that would be dealt to you is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        if (preventsAllCombatDamage) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_all_combat_damage",
                    name = "No Combat Damage",
                    description = "All combat damage that would be dealt this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        if (preventsAttackingCreatureDamage) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage_from_attackers",
                    name = "No Attacker Damage",
                    description = "Damage that attacking creatures would deal to you this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        for (sourceDescription in preventedCombatDamageSources) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_combat_damage_from_${sourceDescription.lowercase().replace(' ', '_')}",
                    name = "No Combat Damage",
                    description = "Combat damage that would be dealt by $sourceDescription this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        if (preventDamageTotal > 0) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage",
                    name = "Prevent $preventDamageTotal",
                    description = "The next $preventDamageTotal damage that would be dealt to you is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        for (creatureType in preventedCreatureTypes) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage_from_${creatureType.lowercase()}",
                    name = "Prevent $creatureType",
                    description = "The next time a $creatureType would deal damage to you this turn, prevent that damage",
                    icon = "prevent-damage"
                )
            )
        }
        for (sourceId in preventedFromSources) {
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "a chosen source"
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage_from_source_${sourceId.value}",
                    name = "Prevent from $sourceName",
                    description = "All damage that would be dealt to you by $sourceName this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }

        // The Circle of Protection family and Dark Sphere: one instance from one chosen source.
        // Listed per shield rather than deduplicated by source — two Circles pointed at the same
        // source are two separate shields, each spent by its own damage instance.
        for ((sourceId, halved) in preventedNextInstanceFromSources) {
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "a chosen source"
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_next_damage_instance_from_source_${sourceId.value}" +
                        if (halved) "_halved" else "",
                    name = if (halved) "Halve from $sourceName" else "Prevent from $sourceName",
                    description = if (halved) {
                        "The next time $sourceName would deal damage to you this turn, " +
                            "prevent half that damage, rounded down"
                    } else {
                        "The next time $sourceName would deal damage to you this turn, prevent that damage"
                    },
                    icon = "prevent-damage"
                )
            )
        }

        // Damage-doubling warnings — the mirror image of the prevention shields above, so a player
        // about to take double damage can see it before they attack into it (Twinflame Tyrant,
        // Gratuitous Violence). One badge per replacement: each applies once (CR 616.1), so two
        // Tyrants show two badges and quadruple the damage.
        for (doubler in DamageUtils.damageDoublersAffectingPlayer(state, playerId)) {
            val scope = when (doubler.damageType) {
                is DamageType.Combat -> "Combat damage"
                is DamageType.NonCombat -> "Noncombat damage"
                is DamageType.Any -> "Damage"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "damage_doubled_${doubler.sourceId.value}",
                    name = "Damage Doubled",
                    description = "$scope dealt to you is doubled by ${doubler.sourceName}",
                    icon = "double-damage"
                )
            )
        }

        // Player-scoped damage doubling from a floating effect (Lightning, Army of One's "Stagger").
        // Same badge, but it outlives its source, so it's named for the effect rather than a card.
        for (floatingEffect in state.floatingEffects) {
            val modification = floatingEffect.effect.modification
            if (modification !is SerializableModification.DoubleDamageToPlayer) continue
            if (modification.playerId != playerId) continue
            val attribution = floatingEffect.sourceName?.let { " ($it)" } ?: ""
            effects.add(
                ClientPlayerEffect(
                    effectId = "damage_doubled_player_${floatingEffect.id.value}",
                    name = "Damage Doubled",
                    description = "Damage dealt to you and to permanents you control is doubled$attribution",
                    icon = "double-damage"
                )
            )
        }

        // Check for SkipCombatPhasesComponent (False Peace effect)
        if (container.has<SkipCombatPhasesComponent>()) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_combat",
                    name = "Skip Combat",
                    description = "Combat phases will be skipped on your next turn",
                    icon = "shield-off"
                )
            )
        }

        // Turn-scoped step/phase skips (Fatespinner). One badge per chosen part, so a player who
        // has been hit by two of them sees both.
        container.get<SkippedTurnPartsComponent>()?.parts?.forEach { part ->
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_turn_part_${part.name.lowercase()}",
                    name = "Skip ${part.displayName.replaceFirstChar { it.uppercase() }}",
                    description = "You skip each ${part.displayName} this turn",
                    icon = "shield-off"
                )
            )
        }

        // Check for SkipUntapComponent (Exhaustion effect)
        val skipUntap = container.get<SkipUntapComponent>()
        if (skipUntap != null) {
            val affected = when {
                skipUntap.affectsCreatures && skipUntap.affectsLands -> "Creatures and lands"
                skipUntap.affectsCreatures -> "Creatures"
                skipUntap.affectsLands -> "Lands"
                else -> "Permanents"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_untap",
                    name = "Skip Untap",
                    description = "$affected won't untap during your next untap step",
                    icon = "shield"
                )
            )
        }

        // Check for LoseAtEndStepComponent (Last Chance effect)
        val loseAtEndStep = container.get<LoseAtEndStepComponent>()
        if (loseAtEndStep != null) {
            val description = if (loseAtEndStep.turnsUntilLoss <= 0) {
                "You will lose the game at the beginning of this turn's end step"
            } else {
                "You will lose the game at the beginning of your next end step"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "lose_at_end_step",
                    name = "Last Chance",
                    description = description,
                    icon = "skull"
                )
            )
        }

        // Check for SkipNextTurnComponent (opponent will skip their turn)
        if (container.has<SkipNextTurnComponent>()) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_next_turn",
                    name = "Skip Turn",
                    description = "Your next turn will be skipped",
                    icon = "skip"
                )
            )
        }

        // Shroud, from a resolution-time effect (Gilded Light) or from a permanent that grants it
        // (True Believer). [ControllerShroud] unions the two and re-evaluates any "as long as …"
        // gate the grant sits behind, so the badge tracks the gate instead of freezing on entry.
        if (ControllerShroud.appliesTo(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_shroud",
                    name = "Shroud",
                    description = "You have shroud (you can't be the target of spells or abilities)",
                    icon = "shield"
                )
            )
        }

        // Check for SpellsCantBeCounteredComponent (e.g., Domri, Anarch of Bolas +1)
        container.get<SpellsCantBeCounteredComponent>()?.let { component ->
            val filterDescription = component.filters
                .joinToString(", ") { it.description }
                .ifBlank { "Spells" }
            effects.add(
                ClientPlayerEffect(
                    effectId = "spells_cant_be_countered",
                    name = "Uncounterable",
                    description = "${filterDescription.replaceFirstChar { it.uppercase() }} " +
                        "spells you cast this turn can't be countered",
                    icon = "no-counter"
                )
            )
        }

        // Check for FlashGrantsThisTurnComponent (e.g., Borne Upon a Wind).
        // GameObjectFilter.Any describes itself as "card" — render it as a bare "spells"
        // so the Borne-Upon-a-Wind case reads naturally, and join multiple filter branches
        // with "or" to keep stacked grants grammatical.
        container.get<FlashGrantsThisTurnComponent>()?.let { component ->
            val rendered = component.filters
                .map { if (it == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) "" else it.description.lowercase() }
                .filter { it.isNotEmpty() }
            val spellPhrase = if (rendered.isEmpty()) "spells" else "${rendered.joinToString(" or ")} spells"
            effects.add(
                ClientPlayerEffect(
                    effectId = "flash_grants_this_turn",
                    name = "Flash",
                    description = "You may cast $spellPhrase this turn as though they had flash",
                    icon = "lightning"
                )
            )
        }

        // Hexproof, from a resolution-time effect (Dawn's Truce) or from a permanent that grants it
        // (Shalai, Voice of Plenty). Same union-and-re-evaluate as shroud above; this used to be
        // two blocks, the second scanning the battlefield on the *base* controller so a stolen
        // Shalai badged the wrong player.
        if (ControllerHexproof.appliesTo(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_hexproof",
                    name = "Hexproof",
                    description = "You have hexproof (you can't be the target of spells or abilities your opponents control)",
                    icon = "shield"
                )
            )
        }

        // Ascend / city's blessing (CR 702.131). Read through CitysBlessingService rather than the
        // component so the badge tracks the rule: ascend on a permanent is continuous, and a player
        // who has just crossed ten permanents already has the blessing even though the state-based
        // action that writes the marker hasn't been polled yet. Surface the actual Scryfall "City's
        // Blessing" marker card (tblc #40) as the badge image so it matches the physical-game
        // marker players know.
        if (CitysBlessingService.has(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "citys_blessing",
                    name = "City's Blessing",
                    description = "You have the city's blessing for the rest of the game",
                    icon = "shield",
                    imageUri = "https://cards.scryfall.io/normal/front/3/0/30758c2e-fc01-4037-838c-bdabe8a4e5a3.jpg?1721428739"
                )
            )
        }

        // Storied / enduring story (CR 702.195). Read through EnduringStoryService rather than the
        // component for the same reason as the city's blessing above: storied on a permanent is
        // continuous, so a player who has just crossed three qualifying permanents already has the
        // designation even though the state-based action that writes the marker hasn't been polled
        // yet. The badge image is the actual Scryfall "Enduring Story" marker card (thob #14), so it
        // matches the physical-game marker players know.
        if (EnduringStoryService.has(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "enduring_story",
                    name = "Enduring Story",
                    description = "You have an enduring story for the rest of the game",
                    icon = "shield",
                    imageUri = "https://cards.scryfall.io/normal/front/5/3/53bfaac7-07cf-4637-8f64-aba93ec7fd1a.jpg?1785534385"
                )
            )
        }

        // The Ring emblem (CR 701.54). Surface the tempt count so the player can see which of the
        // emblem's four cumulative abilities are active and which creature is their Ring-bearer.
        container.get<TheRingComponent>()?.let { ring ->
            val bearerName = state.getBattlefield()
                .firstOrNull { state.getEntity(it)?.get<RingBearerComponent>()?.ownerId == playerId }
                ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            val bearerLine = bearerName?.let { "Your Ring-bearer is $it." } ?: "You have no Ring-bearer."
            effects.add(
                ClientPlayerEffect(
                    effectId = "the_ring",
                    name = "The Ring",
                    description = "The Ring has tempted you ${ring.temptCount} time(s). $bearerLine",
                    icon = "the-ring",
                    // current = raw tempt count (may exceed 4); total = the four cumulative abilities.
                    progress = ClientEffectProgress(current = ring.temptCount, total = 4)
                )
            )
        }

        // Check for MustAttackPlayerComponent (Taunt effect)
        val mustAttack = container.get<MustAttackPlayerComponent>()
        if (mustAttack != null) {
            val description = if (mustAttack.activeThisTurn) {
                "Your creatures must attack this turn"
            } else {
                "Your creatures must attack on your next turn"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "must_attack",
                    name = "Taunted",
                    description = description,
                    icon = "taunt"
                )
            )
        }

        // Check for pending spell copies (e.g., Howl of the Horde)
        val pendingCopies = state.pendingSpellCopies.filter { it.controllerId == playerId }
        if (pendingCopies.isNotEmpty()) {
            val totalCopies = pendingCopies.sumOf { it.copies }
            val sourceName = pendingCopies.joinToString(", ") { it.sourceName }
            val filterDesc = pendingCopies.map { it.spellFilter.description }.distinct().joinToString("/")
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_spell_copy",
                    name = "Copy Spell",
                    description = "Your next $filterDesc spell will be copied $totalCopies time(s) ($sourceName)",
                    icon = "copy-spell"
                )
            )
        }

        // Check for pending "next spell can't be countered" riders (e.g., Mistrise Village)
        val pendingUncounterable = state.pendingUncounterableSpells.filter { it.controllerId == playerId }
        if (pendingUncounterable.isNotEmpty()) {
            val sourceName = pendingUncounterable.map { it.sourceName }.distinct().joinToString(", ")
            val filterDesc = pendingUncounterable.map { it.spellFilter.description }.distinct().joinToString("/")
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_uncounterable_spell",
                    name = "Uncounterable",
                    description = "Your next $filterDesc spell can't be countered ($sourceName)",
                    icon = "no-counter"
                )
            )
        }

        // Check for pending "next spell can be cast without paying its mana cost" riders
        // (e.g. World War Hulk I) — the player needs to see the free cast is still available.
        val pendingFreeCasts = state.pendingFreeCastSpells.filter { it.controllerId == playerId }
        if (pendingFreeCasts.isNotEmpty()) {
            val sourceName = pendingFreeCasts.map { it.sourceName }.distinct().joinToString(", ")
            // With several riders the broadest one sets the wording: an unfiltered rider frees the
            // next spell whatever it is, so naming only a filtered sibling's types would understate
            // it — and joining a blank filter into the list rendered as "red or green creature /".
            val filterDesc = if (pendingFreeCasts.any { it.spellFilter == GameObjectFilter.Any }) {
                ""
            } else {
                pendingFreeCasts
                    .map { it.spellFilter.description }
                    .distinct()
                    .joinToString("/") + " "
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_free_cast_spell",
                    name = "Free Cast",
                    description = "Your next ${filterDesc}spell can be cast without paying its mana cost ($sourceName)",
                    icon = "free-cast"
                )
            )
        }

        // Check for global triggered abilities controlled by this player
        // Group by source name so abilities from the same source show as one badge
        val globalAbilitiesBySource = state.globalGrantedTriggeredAbilities
            .filter { it.controllerId == playerId }
            .groupBy { it.sourceName to it.duration }

        for ((key, abilities) in globalAbilitiesBySource) {
            val (sourceName, duration) = key
            val description = abilities.joinToString(" ") {
                it.descriptionOverride ?: it.ability.description
            }
            val isPermanent = duration == Duration.Permanent
            val durationSuffix = if (isPermanent) "" else " (${duration.description})"
            effects.add(
                ClientPlayerEffect(
                    effectId = "emblem_${sourceName.lowercase().replace(" ", "_").replace(",", "")}${if (!isPermanent) "_temp" else ""}",
                    name = if (isPermanent) "$sourceName Emblem" else sourceName,
                    description = description + durationSuffix,
                    icon = if (isPermanent) "emblem" else "triggered-ability"
                )
            )
        }

        // Check for event-based delayed triggers controlled by this player
        // (e.g., Flitterwing Nuisance's "whenever a creature you control deals combat
        //  damage to a player this turn, draw a card" floating ability).
        // Step-based delayed triggers are scheduled actions, not ongoing effects, so skip them.
        val delayedBySource = state.delayedTriggers
            .filter { it.controllerId == playerId && it.trigger != null }
            .groupBy { it.sourceName }

        for ((sourceName, triggers) in delayedBySource) {
            val first = triggers.first()
            val triggerDesc = first.trigger?.event?.description ?: "the triggered event"
            val effectDesc = first.effect.description.replaceFirstChar { it.lowercase() }
            val countSuffix = if (triggers.size > 1) " (×${triggers.size})" else ""
            effects.add(
                ClientPlayerEffect(
                    effectId = "delayed_trigger_${sourceName.lowercase().replace(" ", "_").replace(",", "")}",
                    name = "$sourceName$countSuffix",
                    description = "Whenever $triggerDesc, $effectDesc. (Until end of turn)",
                    icon = "triggered-ability"
                )
            )
        }

        // Check for static-ability emblems controlled by this player. These are synthetic
        // "emblem source" entities created by CreatePermanentEmblemExecutor; they live in
        // GameState.entities but never enter a zone.
        for ((emblemId, emblemContainer) in state.entities) {
            val emblem = emblemContainer.get<com.wingedsheep.engine.state.components.identity.EmblemSourceComponent>()
                ?: continue
            val emblemController = emblemContainer.get<ControllerComponent>()?.playerId
            if (emblemController != playerId) continue
            effects.add(
                ClientPlayerEffect(
                    effectId = "emblem_static_${emblemId.value}",
                    name = "${emblem.sourceName} Emblem",
                    description = emblem.description,
                    icon = "emblem"
                )
            )
        }

        // Check for granted spell keywords (emblems like "spells you cast have storm")
        val playerContainer = state.getEntity(playerId)
        val grantedKeywords = playerContainer?.get<GrantedSpellKeywordsComponent>()
        if (grantedKeywords != null) {
            for (grant in grantedKeywords.grants) {
                val filterDesc = grant.spellFilter.description
                val spellTypeDesc = if (filterDesc == "card" || filterDesc.isBlank()) "Spells"
                    else "${filterDesc.replaceFirstChar { it.uppercase() }} spells"
                effects.add(
                    ClientPlayerEffect(
                        effectId = "emblem_spell_keyword_${grant.keyword.name.lowercase()}",
                        name = "Emblem",
                        description = "$spellTypeDesc you cast have ${grant.keyword.name.lowercase()}.",
                        icon = "emblem"
                    )
                )
            }
        }

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
    private fun notedCreatureTypeBadges(
        container: com.wingedsheep.engine.state.ComponentContainer,
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

    /**
     * Build a list of active effects on a card for display as badges.
     * These come from floating effects that target this specific card.
     */
    private fun buildCardActiveEffects(
        state: GameState,
        entityId: EntityId,
        projectedState: ProjectedState? = null
    ): List<ClientCardEffect> {
        val effects = mutableListOf<ClientCardEffect>()

        // Check for text replacements
        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        if (textReplacement != null) {
            for (r in textReplacement.replacements) {
                effects.add(
                    ClientCardEffect(
                        effectId = "text_modified_${r.fromWord}_${r.toWord}",
                        name = "Text Modified",
                        description = "${r.fromWord} → ${r.toWord}",
                        icon = "text-change"
                    )
                )
            }
        }

        // Damage this creature deals is doubled by an Equipment/Aura attached to it (Mjölnir,
        // Hammer of Thor). This is the source-side mirror of the player badges built in
        // buildPlayerActiveEffects: the doubling is a property of *this* creature's outgoing damage,
        // so it belongs here rather than warning every player that damage dealt to them is doubled.
        // Attachment is checked by the engine, so the badge is absent while the Equipment is unequipped.
        for (doubler in DamageUtils.damageDoublersAffectingSource(state, entityId)) {
            val scope = when (doubler.damageType) {
                is DamageType.Combat -> "Combat damage"
                is DamageType.NonCombat -> "Noncombat damage"
                is DamageType.Any -> "Damage"
            }
            effects.add(
                ClientCardEffect(
                    effectId = "damage_doubled_source_${doubler.sourceId.value}",
                    name = "Damage Doubled",
                    description = "$scope this creature deals is doubled by ${doubler.sourceName}",
                    icon = "double-damage"
                )
            )
        }

        // Check all floating effects that affect this entity
        var preventDamageTotal = 0
        var regenerationShieldCount = 0
        var removeDamageShieldCount = 0

        for (floatingEffect in state.floatingEffects) {
            if (entityId !in floatingEffect.effect.affectedEntities) continue

            when (val modification = floatingEffect.effect.modification) {
                is SerializableModification.CantBeBlockedExceptByColor -> {
                    val colorName = modification.color.lowercase().replaceFirstChar { it.uppercase() }
                    effects.add(
                        ClientCardEffect(
                            effectId = "cant_be_blocked_except_by_${modification.color.lowercase()}",
                            name = "Evasion",
                            description = "Can't be blocked except by $colorName creatures",
                            icon = "evasion"
                        )
                    )
                }
                // The filter-based sibling of the color case above (Speed, Young Avenger's "can't be
                // blocked this turn except by creatures with haste", Resilient Roadrunner). It routes
                // through the same projected evasion channel, so the block rules already enforced it
                // — only the badge was missing, leaving the restriction invisible to both players.
                is SerializableModification.CantBeBlockedExceptBy -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "cant_be_blocked_except_by",
                            name = "Evasion",
                            description = "Can't be blocked except by ${modification.blockerFilter.description}",
                            icon = "evasion"
                        )
                    )
                }
                is SerializableModification.MustBeBlockedByAll -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "must_be_blocked_by_all",
                            name = "Lure",
                            description = "Must be blocked by all creatures able to block it",
                            icon = "lure"
                        )
                    )
                }
                is SerializableModification.MustBeBlockedIfAble -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "must_be_blocked",
                            name = "Must Be Blocked",
                            description = "Must be blocked if able",
                            icon = "lure"
                        )
                    )
                }
                is SerializableModification.SetCantBlock -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "cant_block",
                            name = "Can't Block",
                            description = "This creature can't block this turn",
                            icon = "cant-block"
                        )
                    )
                }
                is SerializableModification.PreventNextDamage -> {
                    preventDamageTotal += modification.remainingAmount
                }
                is SerializableModification.RegenerationShield -> {
                    regenerationShieldCount++
                }
                is SerializableModification.RemoveDamageShield -> {
                    removeDamageShieldCount++
                }
                is SerializableModification.PreventAllDamageDealtBy -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "prevent_all_damage_dealt_by",
                            name = "Silenced",
                            description = "All damage this creature would deal is prevented this turn",
                            icon = "prevent-damage"
                        )
                    )
                }
                is SerializableModification.SetCantAttack -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "cant_attack",
                            name = "Can't Attack",
                            description = "This creature can't attack",
                            icon = "cant-attack"
                        )
                    )
                }
                is SerializableModification.CantBeRegenerated -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "cant_be_regenerated",
                            name = "No Regen",
                            description = "This creature can't be regenerated",
                            icon = "cant-attack"
                        )
                    )
                }
                is SerializableModification.ExileOnDeath -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "exile_on_death",
                            name = "Exile on Death",
                            description = "If this creature would die, exile it instead",
                            icon = "exile-on-death"
                        )
                    )
                }
                is SerializableModification.ExileControllerGraveyardOnDeath -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "exile_gy_on_death",
                            name = "Exile GY on Death",
                            description = "When this creature dies, its controller's graveyard is exiled",
                            icon = "exile-on-death"
                        )
                    )
                }
                is SerializableModification.MustBlockSpecificAttacker -> {
                    // Name the attacker. The pinned creature is not always the obvious one — an
                    // ANY-bound "blocks that Wolf" trigger (Tolsimir, Midnight's Light) pins the
                    // blocker to a creature other than the ability's source, and a defender who
                    // guesses wrong only finds out when their declaration is rejected.
                    val attackerName = state.getEntity(modification.attackerId)
                        ?.get<CardComponent>()?.name
                    effects.add(
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
                    )
                }
                // The unrestricted requirement (Culvert Ambusher). Badged for the same reason as
                // "Must Attack": the defending player finds out about it when their declaration is
                // rejected, which is far too late to be the first they hear of it.
                is SerializableModification.SetMustBlock -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "must_block_this_turn",
                            name = "Must Block",
                            description = "This creature must block this turn if able",
                            icon = "must-attack"
                        )
                    )
                }
                // PreventAllCombatDamage, PreventCombatDamageFromGroup and
                // PreventDamageFromAttackingCreatures are not card-scoped — the first two hold no
                // affected entity at all and the third holds a player — so they are badged on the
                // player in buildActiveEffects instead.
                is SerializableModification.PreventCombatDamageToAndBy -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "prevent_combat_damage_to_and_by",
                            name = "No Combat Dmg",
                            description = "All combat damage dealt to and dealt by this creature is prevented",
                            icon = "prevent-damage"
                        )
                    )
                }
                is SerializableModification.RedirectNextDamage -> {
                    val amountText = modification.amount?.let { "$it" } ?: "all"
                    effects.add(
                        ClientCardEffect(
                            effectId = "redirect_next_damage",
                            name = "Redirect $amountText",
                            description = "The next $amountText damage that would be dealt to this is redirected",
                            icon = "redirect"
                        )
                    )
                }
                is SerializableModification.PreventNextDamageFromSourceShield -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "deflect_damage_${modification.damageSourceId}",
                            name = "Deflect",
                            description = "The next damage from the chosen source is prevented; a triggered ability then resolves",
                            icon = "redirect"
                        )
                    )
                }
                is SerializableModification.ReflectCombatDamage -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "reflect_combat_damage",
                            name = "Reflect",
                            description = "Combat damage dealt to you is also dealt to the attacking player",
                            icon = "redirect"
                        )
                    )
                }
                is SerializableModification.RedirectCombatDamageToController -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "redirect_combat_damage_to_controller",
                            name = "Redirected",
                            description = "Combat damage this creature would deal is dealt to its controller instead",
                            icon = "redirect"
                        )
                    )
                }
                is SerializableModification.RemoveAllAbilities -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "lost_all_abilities",
                            name = "No Abilities",
                            description = "This permanent has lost all abilities",
                            icon = "lost-abilities"
                        )
                    )
                }
                // SetCreatureSubtypes is surfaced once below, using the projected state,
                // so superseded floating effects (e.g., an earlier Scout transform that
                // a later Soldier transform overrode) don't produce duplicate badges.
                // ChangeColor is surfaced once below from projected state (same reasoning).
                // Other modifications don't need badges (stats/keywords/types are shown elsewhere)
                else -> { /* No badge needed */ }
            }
        }

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

        // Check for triggered ability condition indicators (intervening-if progress)
        effects.addAll(buildTriggerConditionBadges(state, entityId))

        // Surface a single "type-change" badge when projected creature subtypes diverge
        // from what the printed card art shows. We compute this from projected state
        // (rather than per floating effect) so superseded transformations don't produce
        // duplicate badges (e.g., Figure of Fable Scout → Soldier).
        if (projectedState != null) {
            val baseCardComponent = state.getEntity(entityId)?.get<CardComponent>()
            val baseSubtypes = baseCardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
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
                if (!hasChangelingKeyword) {
                    effects.add(
                        ClientCardEffect(
                            effectId = "all_creature_types",
                            name = "All types",
                            description = "Is every creature type",
                            icon = "type-change"
                        )
                    )
                }
            } else if (hasSetCreatureSubtypes && projectedSubtypes.isNotEmpty() && projectedSubtypes != baseSubtypes) {
                val joined = projectedSubtypes.joinToString(" ")
                effects.add(
                    ClientCardEffect(
                        effectId = "type_changed",
                        name = joined,
                        description = "Creature types are now $joined",
                        icon = "type-change"
                    )
                )
            } else if (!hasSetCreatureSubtypes) {
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
                if (addedSubtypes.isNotEmpty()) {
                    val joined = addedSubtypes.joinToString(" ")
                    effects.add(
                        ClientCardEffect(
                            effectId = "type_added",
                            name = "+$joined",
                            description = "Also a $joined in addition to its other types",
                            icon = "type-change"
                        )
                    )
                }
            }

            // Surface a "type-change" badge when a permanent's CARD TYPES are replaced by a
            // type-changing effect (Kitesail Larcenist "becomes a Treasure artifact", Song of the
            // Dryads "becomes a Forest land", Polymorphist's Jest "becomes a Frog"). This is the
            // full-replacement modification (SetCardTypes); additive "becomes a creature in addition
            // to its other types" animate effects use AddType and don't trip this. The printed art
            // still reads as the original object, so the badge makes the new type visible and pairs
            // with the P/T box vanishing (CR 208.3). Driven from projected state so superseded
            // transformations don't stack, and the projected subtypes ride along ("Artifact —
            // Treasure").
            val hasSetCardTypes = state.floatingEffects.any {
                entityId in it.effect.affectedEntities &&
                    it.effect.modification is SerializableModification.SetCardTypes
            }
            if (hasSetCardTypes) {
                val baseTypes = baseCardComponent?.typeLine?.cardTypes?.map { it.name }?.toSet() ?: emptySet()
                val projectedTypes = projectedState.getTypes(entityId)
                if (projectedTypes.isNotEmpty() && projectedTypes != baseTypes) {
                    val typeWords = com.wingedsheep.sdk.core.CardType.entries
                        .filter { it.name in projectedTypes }
                        .map { it.displayName }
                    val subtypeWords = projectedState.getSubtypes(entityId).toList()
                    val newTypeLine = buildString {
                        append(typeWords.joinToString(" "))
                        if (subtypeWords.isNotEmpty()) append(" — ").append(subtypeWords.joinToString(" "))
                    }
                    effects.add(
                        ClientCardEffect(
                            effectId = "card_type_changed",
                            name = newTypeLine,
                            description = "Card type is now $newTypeLine",
                            icon = "type-change"
                        )
                    )
                }
            }

            // Surface a "type-change" badge when a land's basic land types are replaced
            // (e.g., Slimy Kavu / Dream Thrush "target land becomes a Swamp"). The type
            // line text already reflects this, but a battlefield permanent is read by its
            // art, so the badge makes the change visible. Driven from projected state so
            // superseded transformations (re-targeting the same land) don't stack.
            // Dream Thrush's chosen-type variant resolves to a concrete SetBasicLandTypes
            // at execution time, so the floating effect is always SetBasicLandTypes here.
            val hasSetBasicLandTypes = state.floatingEffects.any {
                entityId in it.effect.affectedEntities &&
                    it.effect.modification is SerializableModification.SetBasicLandTypes
            }
            if (hasSetBasicLandTypes) {
                val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                val baseLandTypes = baseSubtypes.filter { it in basicLandTypes }.toSet()
                val projectedLandTypes = projectedState.getSubtypes(entityId)
                    .filter { it in basicLandTypes }
                if (projectedLandTypes.isNotEmpty() && projectedLandTypes.toSet() != baseLandTypes) {
                    val joined = projectedLandTypes.joinToString(" ")
                    effects.add(
                        ClientCardEffect(
                            effectId = "land_type_changed",
                            name = joined,
                            description = "Land types are now $joined",
                            icon = "type-change"
                        )
                    )
                }
            }

            // Surface a single "color-change" badge when a ChangeColor floating effect
            // is replacing this entity's colors. Driven from projected state so
            // superseded transformations don't stack (Tam re-targeting same creature).
            val hasChangeColor = state.floatingEffects.any {
                entityId in it.effect.affectedEntities &&
                    it.effect.modification is SerializableModification.ChangeColor
            }
            if (hasChangeColor) {
                val baseColors = baseCardComponent?.colors?.map { it.name }?.toSet() ?: emptySet()
                val projectedColors = projectedState.getColors(entityId)
                if (projectedColors != baseColors) {
                    val name = when {
                        projectedColors.isEmpty() -> "Colorless"
                        projectedColors.size == 5 -> "All Colors"
                        else -> projectedColors.joinToString(" ") {
                            it.lowercase().replaceFirstChar { c -> c.uppercase() }
                        }
                    }
                    val description = "Colors are now $name"
                    effects.add(
                        ClientCardEffect(
                            effectId = "color_changed",
                            name = name,
                            description = description,
                            icon = "color-change"
                        )
                    )
                }
            }
        }

        // Check if this creature's damage is prevented by a PreventNextDamageFromCreatureType shield
        if (projectedState != null && projectedState.isCreature(entityId)) {
            val subtypes = projectedState.getSubtypes(entityId)
            for (floatingEffect in state.floatingEffects) {
                val modification = floatingEffect.effect.modification
                if (modification is SerializableModification.PreventNextDamageFromCreatureType) {
                    if (subtypes.any { it.equals(modification.creatureType, ignoreCase = true) }) {
                        effects.add(
                            ClientCardEffect(
                                effectId = "damage_prevented_by_type_${modification.creatureType.lowercase()}",
                                name = "Damage Prevented",
                                description = "Damage from this ${modification.creatureType} would be prevented",
                                icon = "prevent-damage"
                            )
                        )
                        break
                    }
                }
            }
        }

        // Surface temporarily-granted abilities (triggered / activated / cast-keyword). E.g. Sygg,
        // Wanderwine Wisdom grants a draw-on-combat-damage trigger until end of turn; Songcrafter
        // Mage grants harmonize to a graveyard spell. The grant is real game state but the printed
        // oracle text on the recipient doesn't reflect it, so without a badge a player can't see it.
        //
        // The *same* ability can be granted to one permanent more than once — a land earthbended
        // twice holds two separate grant entries of the identical "return it tapped" trigger (each
        // grant gets a fresh AbilityId, so they don't collapse by id). To the player they're one
        // ability, so dedupe by the shown description and emit a single badge instead of stacking
        // duplicate "Granted Ability" tiles.
        val seenGrantDescriptions = HashSet<String>()
        for (granted in state.grantedTriggeredAbilities) {
            if (granted.entityId != entityId) continue
            if (!seenGrantDescriptions.add(granted.ability.description)) continue
            effects.add(
                ClientCardEffect(
                    effectId = "granted_trig_${granted.ability.id.value}",
                    name = "Granted Ability",
                    description = granted.ability.description,
                    icon = "granted-ability"
                )
            )
        }
        for (granted in state.grantedActivatedAbilities) {
            if (granted.entityId != entityId) continue
            if (!seenGrantDescriptions.add(granted.ability.description)) continue
            effects.add(
                ClientCardEffect(
                    effectId = "granted_act_${granted.ability.id.value}",
                    name = "Granted Ability",
                    description = granted.ability.description,
                    icon = "granted-ability"
                )
            )
        }
        for (granted in state.grantedKeywordAbilities) {
            if (granted.entityId != entityId) continue
            if (!seenGrantDescriptions.add(granted.ability.description)) continue
            effects.add(
                ClientCardEffect(
                    effectId = "granted_kw_${granted.ability.keyword?.name ?: "keyword"}",
                    name = "Granted Ability",
                    description = granted.ability.description,
                    icon = "granted-ability"
                )
            )
        }
        // Granted *static* abilities (e.g. Cavern Stomper's "{3}{G}: can't be blocked by creatures
        // with power 2 or less"). These live in their own GameState list — combat reads them directly
        // rather than through the layer system — so they never reach the projected keyword set that
        // feeds `abilityFlags`. Without this badge the grant is invisible after it resolves.
        for (granted in state.grantedStaticAbilities) {
            if (granted.entityId != entityId) continue
            if (!seenGrantDescriptions.add(granted.ability.description)) continue
            effects.add(
                ClientCardEffect(
                    effectId = "granted_static_${granted.ability.description.hashCode()}",
                    name = "Granted Ability",
                    description = granted.ability.description,
                    icon = "granted-ability"
                )
            )
        }

        // Printed "can't be blocked by more than N" restrictions (incl. the conditional descend
        // form, e.g. Akawalli's descend-8) are read directly by BlockPhaseManager, never through
        // the layer system, so they never reach the projected keyword set / abilityFlags and get
        // no keyword chip. Surface an active one as a badge so the restriction is visible in play.
        val cardDefForRestrictions = state.getEntity(entityId)?.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val restrictionController = state.projectedState.getController(entityId)
        if (cardDefForRestrictions != null && restrictionController != null) {
            for (ability in cardDefForRestrictions.script.staticAbilities) {
                val active = visibility.activeStaticAbility(state, ability, entityId, restrictionController)
                if (active is com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan &&
                    active.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self
                ) {
                    val description = active.description.replaceFirstChar { it.uppercase() }
                    if (seenGrantDescriptions.add(description)) {
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
        }

        // Printed "can attack despite defender" (Shipwreck Sentry, Mechan Shieldmate, …) and the
        // temporary Krotiq-style grant are read directly by AttackRestrictionRules, never through the
        // layer system, so they never reach the projected keyword set / abilityFlags. Surface a badge
        // while the creature actually has Defender AND the restriction is currently lifted, so the
        // player can see a Defender that can presently attack (e.g. after an artifact entered this
        // turn). Shares DefenderBypass with the attack-legality rule so the badge shows exactly when
        // the attack would be allowed.
        if (restrictionController != null &&
            state.projectedState.hasKeyword(entityId, Keyword.DEFENDER) &&
            DefenderBypass.isActive(state, entityId, restrictionController, cardRegistry)
        ) {
            val description = "Can attack despite defender"
            if (seenGrantDescriptions.add(description)) {
                effects.add(
                    ClientCardEffect(
                        effectId = "can_attack_despite_defender",
                        name = "Can Attack",
                        description = description,
                        icon = "can-attack"
                    )
                )
            }
        }

        return effects
    }

    /**
     * Build badges showing progress toward intervening-if trigger conditions.
     * For example, Oversold Cemetery shows "Creatures in GY: 2/4", and an unsolved Case shows how
     * far along its "to solve" criterion is ("2/3 sources dealt damage").
     */
    private fun buildTriggerConditionBadges(
        state: GameState,
        entityId: EntityId
    ): List<ClientCardEffect> {
        val container = state.getEntity(entityId) ?: return emptyList()
        val cardComponent = container.get<CardComponent>() ?: return emptyList()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return emptyList()
        val controllerId = container.get<ControllerComponent>()?.playerId ?: return emptyList()

        val badges = mutableListOf<ClientCardEffect>()

        for (ability in cardDef.triggeredAbilities) {
            val condition = ability.triggerCondition ?: continue
            badges.addAll(evaluateConditionBadges(state, condition, controllerId, entityId))
        }

        // The client keys badges by effectId, so a card with two countable conditions — two
        // intervening-ifs, or one composite carrying two of them — needs them distinguished. The
        // first keeps the bare id so existing badges are untouched.
        return badges.mapIndexed { index, badge ->
            if (index == 0) badge else badge.copy(effectId = "${badge.effectId}_$index")
        }
    }

    /**
     * Whether [subtypes] covers every creature type — printed changeling, granted changeling, or an
     * "is all creature types" static (Undercover Skrull, Stalactite Dagger). Two places need the
     * same answer and must not drift: the type line collapses back to the printed subtypes rather
     * than rendering ~150 entries, and the badge builder substitutes a single "All types" badge.
     */
    private fun hasEveryCreatureType(subtypes: Collection<String>): Boolean {
        if (subtypes.isEmpty()) return false
        // Hash once: one caller hands us a List, and a linear `in` per creature type would make
        // this ~150 scans of it on every card of every state transform.
        val lookup = if (subtypes is Set<String>) subtypes else subtypes.toSet()
        return Subtype.ALL_CREATURE_TYPES.all { it in lookup }
    }

    /**
     * The progress badges one triggered ability's condition contributes: one per countable clause,
     * none while an uncountable clause is already false.
     */
    private fun evaluateConditionBadges(
        state: GameState,
        condition: Condition,
        controllerId: EntityId,
        sourceId: EntityId,
    ): List<ClientCardEffect> {
        // The badge must evaluate against the permanent that owns the ability: conditions
        // routinely read `EntityReference.Source` (counters on this permanent, its power,
        // whether it's attacking). With a null sourceId those resolve to 0, so the badge
        // reads a permanently-stuck "0/N" while the real condition works fine — e.g. MSH's
        // Plan enchantments, whose "the number of plan counters" badge never moved.
        val context = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
        )
        // An intervening-if with more than one clause is one condition to the engine and several
        // to a player. Every Case's "to solve" is such a composite — the printed criterion ANDed
        // with "and this Case is not solved" (CR 719.3a) — so without splitting it, the very cards
        // whose whole point is a countdown were the ones showing no countdown at all.
        val parts = when (condition) {
            is AllConditions -> condition.conditions
            else -> listOf(condition)
        }
        val progress = parts.map { part -> part to conditionEvaluator.countProgress(state, part, context) }
        // A clause that can't be counted is a gate, and a gate that's already shut makes the count
        // moot: a solved Case would otherwise sit on a stale "5/3" for the rest of the game,
        // because "not solved" — not the criterion — is what stops it triggering again.
        val gateShut = progress.any { (part, counted) ->
            counted == null && !conditionEvaluator.evaluate(state, part, context)
        }
        if (gateShut) return emptyList()

        return progress.mapNotNull { (part, counted) ->
            counted?.let {
                ClientCardEffect(
                    effectId = "condition_compare",
                    name = "${it.current}/${it.required}",
                    description = "${describeCountedClause(part)} (${it.current}/${it.required})",
                    icon = if (it.met) "condition-met" else "condition-unmet"
                )
            }
        }
    }

    /**
     * How a counted clause is named in its badge tooltip: the *quantity* it counts, not the whole
     * comparison. A [Compare] names its left operand ("the number of creature cards in your
     * graveyard"), which reads as a label beside the "2/4" rather than repeating the threshold that
     * is already there. The clause shapes that have no left operand — "you control no suspected
     * Skeletons", "you've cast N spells this turn" — fall back to describing themselves.
     */
    private fun describeCountedClause(condition: Condition): String = when (condition) {
        is Compare -> condition.left.description
        else -> condition.description
    }

    /**
     * Transform combat state if in combat.
     */
    private fun transformCombat(state: GameState): ClientCombatState? {
        // Check if we're in a combat step
        if (state.step.phase != Phase.COMBAT) {
            return null
        }

        // Find all creatures with "must be blocked by all" requirement
        val mustBeBlockedCreatures = findMustBeBlockedCreatures(state)

        val attackers = mutableListOf<ClientAttacker>()
        val blockers = mutableListOf<ClientBlocker>()
        var attackingPlayerId: EntityId? = null
        var defendingPlayerId: EntityId? = null

        // Find all attackers and blockers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val attackingComponent = container.get<AttackingComponent>()
            if (attackingComponent != null) {
                val blockedComponent = container.get<BlockedComponent>()

                // Track the attacking and defending players
                val controllerId = container.get<ControllerComponent>()?.playerId
                if (controllerId != null) {
                    attackingPlayerId = controllerId
                    defendingPlayerId = attackingComponent.defenderId
                }

                val damageOrderComponent = container.get<DamageAssignmentOrderComponent>()
                val damageAssignmentComponent = container.get<DamageAssignmentComponent>()

                attackers.add(
                    ClientAttacker(
                        creatureId = entityId,
                        creatureName = cardComponent.name,
                        attackingTarget = if (state.turnOrder.contains(attackingComponent.defenderId)) {
                            ClientCombatTarget.Player(attackingComponent.defenderId)
                        } else {
                            ClientCombatTarget.Planeswalker(attackingComponent.defenderId)
                        },
                        blockedBy = blockedComponent?.blockerIds ?: emptyList(),
                        mustBeBlockedByAll = entityId in mustBeBlockedCreatures,
                        bandId = attackingComponent.bandId,
                        damageAssignmentOrder = damageOrderComponent?.orderedBlockers,
                        damageAssignments = damageAssignmentComponent?.assignments
                    )
                )
            }

            val blockingComponent = container.get<BlockingComponent>()
            if (blockingComponent != null) {
                for (attackerId in blockingComponent.blockedAttackerIds) {
                    blockers.add(
                        ClientBlocker(
                            creatureId = entityId,
                            creatureName = cardComponent.name,
                            blockingAttacker = attackerId
                        )
                    )
                }
            }
        }

        // If no attackers, there's no combat state to show
        if (attackers.isEmpty()) {
            return null
        }

        return ClientCombatState(
            attackingPlayerId = attackingPlayerId ?: state.activePlayerId ?: return null,
            defendingPlayerId = defendingPlayerId ?: state.getOpponents(attackingPlayerId!!).firstOrNull() ?: return null,
            attackers = attackers,
            blockers = blockers
        )
    }

    /**
     * Find all creatures that have any "must be blocked" requirement from floating effects.
     * Includes both "must be blocked by all" (Lure) and "must be blocked if able" (Gaea's Protector).
     */
    private fun findMustBeBlockedCreatures(state: GameState): Set<EntityId> {
        return state.floatingEffects
            .filter {
                it.effect.modification is SerializableModification.MustBeBlockedByAll ||
                    it.effect.modification is SerializableModification.MustBeBlockedIfAble
            }
            .flatMap { it.effect.affectedEntities }
            .toSet()
    }

    /**
     * Find which zone an entity is currently in.
     * Returns the zone type name (e.g., "GRAVEYARD") or null if not found.
     */
    private fun findEntityZone(state: GameState, entityId: EntityId): String? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey.zoneType.name
            }
        }
        return null
    }

    /**
     * If any of the card's static abilities is gated on "you have at least N cards in
     * your graveyard" (the classic threshold pattern, also used by delirium-style cards),
     * return the smallest such N. Returns null if no such gate exists.
     */
    private fun findGraveyardThreshold(cardDef: CardDefinition): Int? {
        val thresholds = cardDef.script.staticAbilities
            .filterIsInstance<ConditionalStaticAbility>()
            .flatMap { extractGraveyardThresholds(it.condition) }
        return thresholds.minOrNull()
    }

    private fun extractGraveyardThresholds(condition: Condition): List<Int> = when (condition) {
        is Compare -> {
            val n = matchYourGraveyardAtLeast(condition)
            if (n != null) listOf(n) else emptyList()
        }
        is AllConditions -> condition.conditions.flatMap { extractGraveyardThresholds(it) }
        is AnyCondition -> condition.conditions.flatMap { extractGraveyardThresholds(it) }
        is NotCondition -> emptyList()
        else -> emptyList()
    }

    /**
     * Matches `Count(You, GRAVEYARD, Any) >= Fixed(N)` and the symmetric `Fixed(N) <= Count(...)`.
     * Returns N or null. Filters that count a subset (e.g. only creatures) are ignored — the
     * UI badge tracks raw graveyard size which is what threshold uses.
     */
    private fun matchYourGraveyardAtLeast(compare: Compare): Int? {
        fun isYourGraveyardCount(amount: DynamicAmount): Boolean =
            amount is DynamicAmount.Count &&
                amount.player == ScriptPlayer.You &&
                amount.zone == Zone.GRAVEYARD &&
                amount.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any

        val left = compare.left
        val right = compare.right
        return when (compare.operator) {
            ComparisonOperator.GTE ->
                if (isYourGraveyardCount(left) && right is DynamicAmount.Fixed) right.amount else null
            ComparisonOperator.LTE ->
                if (isYourGraveyardCount(right) && left is DynamicAmount.Fixed) left.amount else null
            ComparisonOperator.GT ->
                if (isYourGraveyardCount(left) && right is DynamicAmount.Fixed) right.amount + 1 else null
            ComparisonOperator.LT ->
                if (isYourGraveyardCount(right) && left is DynamicAmount.Fixed) left.amount + 1 else null
            else -> null
        }
    }

    /**
     * Distinct card types among cards in [playerId]'s graveyard — the Delirium count (CR; active
     * at 4+). Mirrors the engine's `Aggregation.DISTINCT_TYPES`. Graveyard is a non-battlefield
     * zone, so base card types are correct here (no projection needed).
     */
    private fun distinctGraveyardCardTypes(state: GameState, playerId: EntityId): Int =
        state.getGraveyard(playerId)
            .flatMapTo(mutableSetOf()) { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name } ?: emptyList()
            }
            .size

    /**
     * The delirium threshold a card gates on, or null if the card doesn't care about delirium.
     *
     * Delirium gates can sit anywhere in a card's script — a static ability, an activated/triggered
     * ability, a spell effect, a cost reduction, or a replacement effect — so rather than
     * enumerate every container we walk the card's serialized JSON tree (the same coverage
     * strategy as `CardLinter`) for a `distinct graveyard card types <comparison> N` shape.
     * Result is memoized per card name: card definitions are immutable and shared, and this runs
     * for every card in every client view.
     */
    private fun findDeliriumThreshold(cardDef: CardDefinition): Int? =
        deliriumThresholdCache.computeIfAbsent(cardDef.name) { detectDeliriumThreshold(cardDef) ?: 0 }
            .takeIf { it > 0 }

    /** Cache of card name → delirium threshold; `0` is the sentinel for "no delirium gate". */
    private val deliriumThresholdCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /**
     * Card encoder with defaults materialized (same trick as `CardLinter`): the shared
     * `CardSerialization.json` encodes with `encodeDefaults = false`, which would drop the
     * default-valued discriminators we match on (`player = You`, `filter = Any`), so encode them.
     */
    private val deliriumDetectJson = kotlinx.serialization.json.Json(
        from = com.wingedsheep.sdk.serialization.CardSerialization.json
    ) { encodeDefaults = true }

    private fun detectDeliriumThreshold(cardDef: CardDefinition): Int? {
        val tree = deliriumDetectJson.encodeToJsonElement(CardDefinition.serializer(), cardDef)
        val thresholds = mutableListOf<Int>()
        collectDeliriumThresholds(tree, thresholds)
        return thresholds.minOrNull()
    }

    private fun collectDeliriumThresholds(element: JsonElement, out: MutableList<Int>) {
        when (element) {
            is JsonObject -> {
                if (element.content("type") == "Compare") {
                    deliriumThresholdOf(element)?.let { out.add(it) }
                }
                element.values.forEach { collectDeliriumThresholds(it, out) }
            }
            is JsonArray -> element.forEach { collectDeliriumThresholds(it, out) }
            else -> {}
        }
    }

    /** A primitive string field's content, or null if the key is absent or holds a non-primitive. */
    private fun JsonObject.content(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    /**
     * Matches a `Compare` whose one side counts distinct card types in *your* graveyard
     * (`AggregateZone(You, Graveyard, DISTINCT_TYPES)`) and whose other side is a fixed number,
     * returning the threshold at which delirium turns on. Mirrors [matchYourGraveyardAtLeast] but
     * on the serialized tree and for the distinct-types aggregation.
     */
    private fun deliriumThresholdOf(compare: JsonObject): Int? {
        // `Player.You` serializes polymorphically as {"type":"You"}; tolerate a bare "You" too.
        fun isYou(operand: JsonElement?): Boolean = when (operand) {
            is JsonPrimitive -> operand.contentOrNull == "You"
            is JsonObject -> operand.content("type") == "You"
            else -> false
        }

        fun isDistinctGraveyardTypes(operand: JsonElement?): Boolean {
            val o = operand as? JsonObject ?: return false
            return o.content("type") == "AggregateZone" &&
                isYou(o["player"]) &&
                o.content("zone") == "Graveyard" &&
                o.content("aggregation") == "DISTINCT_TYPES"
        }

        fun fixedAmount(operand: JsonElement?): Int? {
            val o = operand as? JsonObject ?: return null
            if (o.content("type") != "Fixed") return null
            return o.content("amount")?.toIntOrNull()
        }

        val left = compare["left"]
        val right = compare["right"]
        return when (compare.content("operator")) {
            "GTE" -> if (isDistinctGraveyardTypes(left)) fixedAmount(right) else null
            "LTE" -> if (isDistinctGraveyardTypes(right)) fixedAmount(left) else null
            "GT" -> if (isDistinctGraveyardTypes(left)) fixedAmount(right)?.plus(1) else null
            "LT" -> if (isDistinctGraveyardTypes(right)) fixedAmount(left)?.plus(1) else null
            else -> null
        }
    }
}
