package com.wingedsheep.gameserver.dto

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
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
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition

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
    private val cardRegistry: CardRegistry
) {


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
            val isZoneVisible = isZoneVisibleTo(zoneKey, viewingPlayerId)

            // For hidden zones (like opponent's hand), check which individual cards are revealed
            val visibleCardIds = if (isZoneVisible) {
                entityIds
            } else {
                entityIds.filter { entityId ->
                    isCardRevealedTo(state, entityId, viewingPlayerId)
                }
            }

            zones.add(
                ClientZone(
                    zoneId = zoneKey,
                    cardIds = visibleCardIds,
                    size = entityIds.size,
                    isVisible = isZoneVisible || visibleCardIds.isNotEmpty()
                )
            )

            // Include card details for visible cards (either whole zone visible, or individually revealed)
            for (entityId in visibleCardIds) {
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
                        ?: transformAbilityOnStack(state, entityId)
                    if (clientCard != null) {
                        cards[entityId] = clientCard
                    }
                }
            }
        }
        // --- FIX END ---

        // Reveal top library card for players with PlayFromTopOfLibrary (e.g., Future Sight)
        // The top card is revealed to ALL players per the card's oracle text.
        for (ownerId in state.turnOrder) {
            if (hasPlayFromTopOfLibrary(state, ownerId)) {
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
            combat = combat
        )
    }

    /**
     * Check if a zone's contents should be visible to a player.
     */
    private fun isZoneVisibleTo(zoneKey: ZoneKey, viewingPlayerId: EntityId): Boolean {
        return when (zoneKey.zoneType) {
            Zone.LIBRARY -> false
            Zone.HAND -> zoneKey.ownerId == viewingPlayerId
            Zone.BATTLEFIELD,
            Zone.GRAVEYARD,
            Zone.STACK,
            Zone.EXILE,
            Zone.COMMAND -> true
        }
    }

    /**
     * Check if a player controls a permanent with PlayFromTopOfLibrary.
     */
    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a player controls a permanent with LookAtTopOfLibrary (e.g., Lens of Clarity).
     * This reveals the top card of the controller's library privately (only to them).
     */
    private fun hasLookAtTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is LookAtTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a player controls a permanent with LookAtFaceDownCreatures (e.g., Lens of Clarity).
     * This reveals the identity of opponent's face-down battlefield creatures to the controller.
     */
    private fun hasLookAtFaceDownCreatures(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is LookAtFaceDownCreatures }) {
                return true
            }
        }
        return false
    }

    /**
     * Check if an individual card has been revealed to a specific player.
     * This is used for "look at hand" or "reveal hand" effects where the viewing player
     * can see specific cards in an otherwise hidden zone.
     */
    private fun isCardRevealedTo(state: GameState, entityId: EntityId, viewingPlayerId: EntityId): Boolean {
        val revealedComponent = state.getEntity(entityId)?.get<RevealedToComponent>()
        return revealedComponent?.isRevealedTo(viewingPlayerId) == true
    }

    /**
     * Transform an activated or triggered ability on the stack into a ClientCard DTO.
     * These don't have CardComponent, so we create a synthetic card representation.
     */
    private fun transformAbilityOnStack(state: GameState, entityId: EntityId): ClientCard? {
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
                oracleText = runtimeAbilityText(state, entityId, activatedAbility.controllerId, activatedAbility.effect, activatedAbility.xValue)
                    ?: activatedAbility.effect.description,
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
                imageUri = cardDef?.metadata?.imageUri,
                chosenX = activatedAbility.xValue
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

            return ClientCard(
                id = entityId,
                name = "${triggeredAbility.sourceName} trigger",
                manaCost = "",
                manaValue = 0,
                typeLine = "Triggered Ability",
                cardTypes = setOf("Ability"),
                subtypes = emptySet(),
                colors = sourceCard?.colors ?: emptySet(),
                oracleText = runtimeAbilityText(state, entityId, triggeredAbility.controllerId, triggeredAbility.effect, triggeredAbility.xValue)
                    ?: triggeredAbility.description,
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
                imageUri = cardDef?.metadata?.imageUri,
                sourceZone = sourceZone
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
        val isFaceDown = isInFaceDownZone && (container.has<FaceDownComponent>() || spellOnStack?.castFaceDown == true)
        // Use projected P/T which correctly handles face-down base 2/2 + any modifications
        val power = projectedValues?.power ?: if (isFaceDown) 2 else cardComponent.baseStats?.basePower
        val toughness = projectedValues?.toughness ?: if (isFaceDown) 2 else cardComponent.baseStats?.baseToughness
        val rawKeywords = projectedValues?.keywords?.mapNotNull {
            try { Keyword.valueOf(it) } catch (_: Exception) { null }
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
        val staticProtections = cardDef?.keywordAbilities
            ?.filterIsInstance<KeywordAbility.ProtectionFromColor>()
            ?.map { it.color }
            ?: emptyList()
        val protections = (projectedProtections.ifEmpty { staticProtections }).distinct()

        // Add PROTECTION keyword when protections are present
        val keywords = if (protections.isNotEmpty()) rawKeywords + Keyword.PROTECTION else rawKeywords

        val colors = projectedValues?.colors?.mapNotNull {
            try { Color.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: cardComponent.colors

        // Get state components
        val isTapped = container.has<TappedComponent>()
        // Summoning sickness doesn't affect creatures with haste
        val hasSummoningSicknessComponent = container.has<SummoningSicknessComponent>()
        val hasHaste = keywords.contains(com.wingedsheep.sdk.core.Keyword.HASTE)
        val hasSummoningSickness = hasSummoningSicknessComponent && !hasHaste

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
                isCardRevealedTo(state, entityId, viewingPlayerId) ||
                (zoneKey.zoneType == Zone.BATTLEFIELD && hasLookAtFaceDownCreatures(state, viewingPlayerId))
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
                    morphCost = null,
                    imageUri = "https://cards.scryfall.io/normal/front/e/9/e9375cbe-93c0-41a5-a6e3-fb4416f54a69.jpg",
                    activeEffects = emptyList(),
                    revealedName = if (isRevealedToViewer) cardComponent.name else null,
                    revealedImageUri = if (isRevealedToViewer) cardDef?.metadata?.imageUri else null
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
                counters = container.get<CountersComponent>()?.counters ?: emptyMap(),
                isTapped = isTapped,
                hasSummoningSickness = hasSummoningSickness,
                isTransformed = false,
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
                morphCost = null, // Opponent can't see morph cost
                imageUri = "https://cards.scryfall.io/normal/front/e/9/e9375cbe-93c0-41a5-a6e3-fb4416f54a69.jpg", // Morph token from Commander 2019
                activeEffects = buildCardActiveEffects(state, entityId),
                revealedName = if (isRevealedToViewer) cardComponent.name else null,
                revealedImageUri = if (isRevealedToViewer) cardDef?.metadata?.imageUri else null
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

        // Get linked exile (cards exiled by this permanent, e.g., Suspension Field)
        val linkedExile = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList()

        // Check if token
        val isToken = container.has<TokenComponent>()

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

        // Get kicker status for spells on the stack
        val wasKicked = spellOnStack?.wasKicked ?: false

        // Get chosen X value for spells on the stack
        val chosenX = spellOnStack?.xValue

        // Get chosen creature type for "as enters" permanents (e.g., Doom Cannon) or spells on stack (e.g., Aphetto Dredging)
        // Also check floating effects for temporary type changes (e.g., Mistform Wall)
        val chosenCreatureType = container.get<ChosenCreatureTypeComponent>()?.creatureType
            ?: spellOnStack?.chosenCreatureType
            ?: state.floatingEffects
                .filter { it.effect.affectedEntities.contains(entityId) }
                .mapNotNull { (it.effect.modification as? SerializableModification.SetCreatureSubtypes)?.subtypes?.firstOrNull() }
                .lastOrNull()

        // Get chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator)
        val chosenColor = container.get<ChosenColorComponent>()?.color?.displayName

        // Get sacrificed creature types for spells with sacrifice-as-cost (e.g., Endemic Plague)
        val sacrificedCreatureTypes = spellOnStack?.sacrificedPermanentSubtypes
            ?.values?.flatten()?.toSet()
            ?.takeIf { it.isNotEmpty() }

        // Build type line string from TypeLine, using projected types/subtypes if available
        val typeLine = cardComponent.typeLine
        val projectedSubtypes = projectedValues?.subtypes?.toList()
        val displaySubtypes = projectedSubtypes ?: typeLine.subtypes.map { it.value }
        val projectedTypes = projectedValues?.types
        val displayCardTypes = if (projectedTypes != null) {
            projectedTypes.mapNotNull { try { CardType.valueOf(it) } catch (_: Exception) { null } }
        } else {
            typeLine.cardTypes.toList()
        }
        val typeLineParts = mutableListOf<String>()
        if (typeLine.supertypes.isNotEmpty()) {
            typeLineParts.add(typeLine.supertypes.joinToString(" ") { it.displayName })
        }
        typeLineParts.add(displayCardTypes.joinToString(" ") { it.displayName })
        val typeLineString = if (displaySubtypes.isNotEmpty()) {
            "${typeLineParts.joinToString(" ")} — ${displaySubtypes.joinToString(" ")}"
        } else {
            typeLineParts.joinToString(" ")
        }

        // Build active effects from floating effects
        val activeEffects = buildCardActiveEffects(state, entityId, projectedState)

        // Check if this card is playable from exile (impulse draw like Mind's Desire)
        val playableFromExile = zoneKey.zoneType == Zone.EXILE &&
            container.get<MayPlayFromExileComponent>()?.controllerId == viewingPlayerId

        return ClientCard(
            id = entityId,
            name = cardComponent.name,
            manaCost = cardComponent.manaCost.toString(),
            manaValue = cardComponent.manaCost.cmc,
            typeLine = typeLineString,
            cardTypes = displayCardTypes.map { it.name }.toSet(),
            subtypes = displaySubtypes.toSet(),
            colors = colors,
            oracleText = cardComponent.oracleText,
            power = power,
            toughness = toughness,
            basePower = cardComponent.baseStats?.basePower,
            baseToughness = cardComponent.baseStats?.baseToughness,
            damage = damage,
            keywords = keywords,
            abilityFlags = abilityFlags,
            protections = protections,
            counters = counters,
            isTapped = isTapped,
            hasSummoningSickness = hasSummoningSickness,
            isTransformed = false, // TODO: Add transformed support
            isAttacking = isAttacking,
            isBlocking = isBlocking,
            attackingTarget = attackingTarget,
            blockingTarget = blockingTarget,
            controllerId = controllerId,
            ownerId = ownerId,
            isToken = isToken,
            zone = zoneKey,
            attachedTo = attachedTo,
            attachments = attachments,
            linkedExile = linkedExile,
            isFaceDown = isFaceDown,
            morphCost = if (isFaceDown && morphData != null) morphData.morphCost.description else null,
            targets = targets,
            imageUri = cardDef?.metadata?.imageUri ?: cardComponent.imageUri,
            activeEffects = activeEffects,
            rulings = cardDef?.metadata?.rulings?.map {
                ClientRuling(date = it.date, text = it.text)
            } ?: emptyList(),
            wasKicked = wasKicked,
            chosenX = chosenX,
            chosenCreatureType = chosenCreatureType,
            chosenColor = chosenColor,
            sacrificedCreatureTypes = sacrificedCreatureTypes,
            playableFromExile = playableFromExile,
            copyOf = container.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()?.let { copyComp ->
                cardRegistry.getCard(copyComp.originalCardDefinitionId)?.name
            },
            damageDistribution = spellOnStack?.damageDistribution?.takeIf { it.isNotEmpty() },
            sagaTotalChapters = cardDef?.finalChapter,
            classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel,
            classMaxLevel = cardDef?.maxClassLevel,
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
            } else null
        )
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

        // For modal spells with a mode chosen at cast time, show the chosen mode description
        if (spellOnStack.chosenModes.isNotEmpty() && effect is com.wingedsheep.sdk.scripting.effects.ModalEffect) {
            val modeIndex = spellOnStack.chosenModes.first()
            val chosenMode = effect.modes.getOrNull(modeIndex)
            if (chosenMode != null) {
                return try {
                    val evaluator = DynamicAmountEvaluator()
                    val context = EffectContext(
                        sourceId = spellEntityId,
                        controllerId = spellOnStack.casterId,
                        opponentId = state.getOpponent(spellOnStack.casterId),
                        xValue = spellOnStack.xValue,
                        sacrificedPermanents = spellOnStack.sacrificedPermanents,
                        sacrificedPermanentSubtypes = spellOnStack.sacrificedPermanentSubtypes,
                        exiledCardCount = spellOnStack.exiledCardCount
                    )
                    chosenMode.effect.runtimeDescription { amount -> evaluator.evaluate(state, amount, context) }
                } catch (_: Exception) {
                    chosenMode.description
                }
            }
        }

        return try {
            val evaluator = DynamicAmountEvaluator()
            val context = EffectContext(
                sourceId = spellEntityId,
                controllerId = spellOnStack.casterId,
                opponentId = state.getOpponent(spellOnStack.casterId),
                xValue = spellOnStack.xValue,
                sacrificedPermanents = spellOnStack.sacrificedPermanents,
                sacrificedPermanentSubtypes = spellOnStack.sacrificedPermanentSubtypes,
                exiledCardCount = spellOnStack.exiledCardCount
            )
            effect.runtimeDescription { amount -> evaluator.evaluate(state, amount, context) }
        } catch (_: Exception) {
            effect.description
        }
    }

    /**
     * Generate runtime text for an ability on the stack with dynamic amounts resolved.
     * Returns null if evaluation fails or the effect has no dynamic amounts.
     */
    private fun runtimeAbilityText(
        state: GameState,
        abilityEntityId: EntityId,
        controllerId: EntityId,
        effect: Effect,
        xValue: Int?
    ): String? {
        return try {
            val evaluator = DynamicAmountEvaluator()
            val context = EffectContext(
                sourceId = abilityEntityId,
                controllerId = controllerId,
                opponentId = state.getOpponent(controllerId),
                xValue = xValue
            )
            val text = effect.runtimeDescription { amount -> evaluator.evaluate(state, amount, context) }
            // Only return if it differs from static description (i.e., dynamic amounts were resolved)
            if (text != effect.description) text else null
        } catch (_: Exception) {
            null
        }
    }

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
        val lifeTotalComponent = container?.get<LifeTotalComponent>()
        val landDropsComponent = container?.get<LandDropsComponent>()
        val manaPoolComponent = container?.get<ManaPoolComponent>()

        // Calculate zone sizes
        val handSize = state.getHand(playerId).size
        val librarySize = state.getLibrary(playerId).size
        val graveyardSize = state.getGraveyard(playerId).size
        val exileSize = state.getExile(playerId).size

        // Determine lands played this turn
        val landsPlayed = if (landDropsComponent != null) {
            landDropsComponent.maxPerTurn - landDropsComponent.remaining
        } else {
            0
        }

        // Check if player has lost (they're not the winner and game is over)
        val hasLost = state.gameOver && state.winnerId != null && state.winnerId != playerId

        // Mana pool is public information in MTG - show for all players
        val manaPool = if (manaPoolComponent != null) {
            ClientManaPool(
                white = manaPoolComponent.white,
                blue = manaPoolComponent.blue,
                black = manaPoolComponent.black,
                red = manaPoolComponent.red,
                green = manaPoolComponent.green,
                colorless = manaPoolComponent.colorless
            )
        } else {
            null
        }

        // Build active effects list
        val activeEffects = buildActiveEffects(state, playerId, container)

        return ClientPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = lifeTotalComponent?.life ?: 20,
            poisonCounters = 0, // TODO: Add poison counter component support
            handSize = handSize,
            librarySize = librarySize,
            graveyardSize = graveyardSize,
            exileSize = exileSize,
            landsPlayedThisTurn = landsPlayed,
            hasLost = hasLost,
            manaPool = manaPool,
            activeEffects = activeEffects
        )
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
        val preventedCreatureTypes = mutableSetOf<String>()
        for (floatingEffect in state.floatingEffects) {
            if (playerId !in floatingEffect.effect.affectedEntities) continue
            when (val modification = floatingEffect.effect.modification) {
                is SerializableModification.PreventNextDamage -> {
                    preventDamageTotal += modification.remainingAmount
                }
                is SerializableModification.PreventNextDamageFromCreatureType -> {
                    preventedCreatureTypes.add(modification.creatureType)
                }
                else -> {}
            }
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

        // Check for PlayerShroudComponent (e.g., Gilded Light)
        if (container.has<PlayerShroudComponent>()) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_shroud",
                    name = "Shroud",
                    description = "You have shroud (you can't be the target of spells or abilities)",
                    icon = "shield"
                )
            )
        }

        // Check for PlayerHexproofComponent (e.g., Dawn's Truce)
        if (container.has<PlayerHexproofComponent>()) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_hexproof",
                    name = "Hexproof",
                    description = "You have hexproof (you can't be the target of spells or abilities your opponents control)",
                    icon = "shield"
                )
            )
        }

        // Check for permanent-based player hexproof (e.g., Shalai, Voice of Plenty)
        val hasHexproof = !container.has<PlayerHexproofComponent>() && state.getBattlefield().any { entityId ->
            val entityContainer = state.getEntity(entityId) ?: return@any false
            entityContainer.get<GrantsControllerHexproofComponent>() != null &&
                entityContainer.get<ControllerComponent>()?.playerId == playerId
        }
        if (hasHexproof) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_hexproof",
                    name = "Hexproof",
                    description = "You have hexproof (you can't be the target of spells or abilities your opponents control)",
                    icon = "shield"
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
            val copyWord = if (totalCopies == 1) "copy" else "copies"
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_spell_copy",
                    name = "Copy Spell",
                    description = "Your next instant or sorcery spell will be copied $totalCopies time(s) ($sourceName)",
                    icon = "copy-spell"
                )
            )
        }

        // Check for permanent global triggered abilities (emblems) controlled by this player
        // Group by source name so a single emblem with multiple abilities shows as one badge
        val emblemsBySource = state.globalGrantedTriggeredAbilities
            .filter { it.controllerId == playerId && it.duration == Duration.Permanent }
            .groupBy { it.sourceName }

        for ((sourceName, abilities) in emblemsBySource) {
            val description = abilities.joinToString(" ") {
                it.descriptionOverride ?: it.ability.description
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "emblem_${sourceName.lowercase().replace(" ", "_").replace(",", "")}",
                    name = "$sourceName Emblem",
                    description = description,
                    icon = "emblem"
                )
            )
        }

        return effects
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

        // Check all floating effects that affect this entity
        var preventDamageTotal = 0
        var regenerationShieldCount = 0

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
                    effects.add(
                        ClientCardEffect(
                            effectId = "must_block_${modification.attackerId}",
                            name = "Must Block",
                            description = "This creature must block a specific attacker if able",
                            icon = "must-attack"
                        )
                    )
                }
                is SerializableModification.PreventAllCombatDamage -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "prevent_all_combat_damage",
                            name = "No Combat Dmg",
                            description = "All combat damage is prevented",
                            icon = "prevent-damage"
                        )
                    )
                }
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
                is SerializableModification.PreventCombatDamageFromGroup -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "prevent_combat_damage_from_group",
                            name = "No Combat Dmg",
                            description = "Combat damage from this creature is prevented",
                            icon = "prevent-damage"
                        )
                    )
                }
                is SerializableModification.PreventDamageFromAttackingCreatures -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "prevent_damage_from_attackers",
                            name = "Fog",
                            description = "Damage from attacking creatures is prevented",
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
                is SerializableModification.DeflectNextDamageFromSource -> {
                    effects.add(
                        ClientCardEffect(
                            effectId = "deflect_damage_${modification.damageSourceId}",
                            name = "Deflect",
                            description = "The next damage from the chosen source is prevented and dealt to that source's controller",
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

        // Check for triggered ability condition indicators (intervening-if progress)
        effects.addAll(buildTriggerConditionBadges(state, entityId))

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

        return effects
    }

    /**
     * Build badges showing progress toward intervening-if trigger conditions.
     * For example, Oversold Cemetery shows "Creatures in GY: 2/4".
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
            val badge = evaluateConditionBadge(state, condition, controllerId)
            if (badge != null) badges.add(badge)
        }

        return badges
    }

    /**
     * Evaluate a trigger condition and return a badge showing progress.
     */
    private fun evaluateConditionBadge(
        state: GameState,
        condition: Condition,
        controllerId: EntityId
    ): ClientCardEffect? {
        return when (condition) {
            is Compare -> {
                val context = com.wingedsheep.engine.handlers.EffectContext(
                    sourceId = null,
                    controllerId = controllerId,
                    opponentId = state.turnOrder.firstOrNull { it != controllerId }
                )
                val evaluator = com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
                val leftVal = evaluator.evaluate(state, condition.left, context)
                val rightVal = evaluator.evaluate(state, condition.right, context)
                val met = when (condition.operator) {
                    ComparisonOperator.LT -> leftVal < rightVal
                    ComparisonOperator.LTE -> leftVal <= rightVal
                    ComparisonOperator.EQ -> leftVal == rightVal
                    ComparisonOperator.NEQ -> leftVal != rightVal
                    ComparisonOperator.GT -> leftVal > rightVal
                    ComparisonOperator.GTE -> leftVal >= rightVal
                }
                ClientCardEffect(
                    effectId = "condition_compare",
                    name = "$leftVal/$rightVal",
                    description = "${condition.left.description} ($leftVal/${rightVal})",
                    icon = if (met) "condition-met" else "condition-unmet"
                )
            }
            else -> null
        }
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
            defendingPlayerId = defendingPlayerId ?: state.getOpponent(attackingPlayerId!!) ?: return null,
            attackers = attackers,
            blockers = blockers
        )
    }

    /**
     * Find all creatures that have "must be blocked by all" requirement from floating effects.
     */
    private fun findMustBeBlockedCreatures(state: GameState): Set<EntityId> {
        return state.floatingEffects
            .filter { it.effect.modification is SerializableModification.MustBeBlockedByAll }
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
}
