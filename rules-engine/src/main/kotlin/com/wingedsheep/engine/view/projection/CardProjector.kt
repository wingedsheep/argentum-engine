package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.ProjectedValues
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.imageOverrideFor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.FACE_DOWN_CARD_DISPLAY_NAME
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.view.ClientCard
import com.wingedsheep.engine.view.ClientImpending
import com.wingedsheep.engine.view.ClientRuling
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.RIOT_MODE_COUNTER
import com.wingedsheep.sdk.dsl.RIOT_MODE_HASTE
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MORPH_HELPER_CARD_IMAGE_URI
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Projects a card (anything with a [CardComponent], in any zone) into a [ClientCard].
 *
 * The projection runs in named stages:
 * 1. [frame] — identity: definition, owner, controller (projected on the battlefield), and whether
 *    the card is face-down *here*;
 * 2. [characteristics] — P/T, keywords, protections, colours and battlefield status, read from
 *    projected state on the battlefield (Rule 613) and from the printed card elsewhere;
 * 3. a face-down card the viewer may not know is masked ([hiddenExiledCard] /
 *    [hiddenFaceDownPermanent]) and the projection stops there;
 * 4. otherwise [visibleCard] adds the rest: the displayed type line, what was chosen for it,
 *    zone-specific status, spell-on-stack detail ([SpellOnStackProjector]), badges
 *    ([CardActiveEffectsProjector]), threshold/delirium progress ([ConditionBadgeProjector]) and its
 *    other faces ([CardFacesProjector]).
 */
internal class CardProjector(
    private val cardRegistry: CardRegistry,
    private val visibility: Visibility,
    private val conditionEvaluator: ConditionEvaluator,
    private val activeEffects: CardActiveEffectsProjector,
    private val conditionBadges: ConditionBadgeProjector,
    private val spellOnStackProjector: SpellOnStackProjector,
    private val facesProjector: CardFacesProjector,
) {
    // Answers "may the viewer cast this exiled card?" the way the cast handler does.
    private val legality = com.wingedsheep.engine.legality.LegalityKernel(cardRegistry, conditionEvaluator)


    /** Transform an entity into a ClientCard DTO, or null when it isn't a card. */
    fun project(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        projectedState: ProjectedState,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false
    ): ClientCard? {
        val frame = frame(state, entityId, zoneKey, projectedState, viewingPlayerId, isSpectator) ?: return null
        val characteristics = characteristics(frame)

        // Handle face-down card masking
        // Opponents and spectators see modified stats but no card information
        // Controller sees real card info + morph cost (but not spectators)
        if (frame.isFaceDown && (isSpectator || frame.controllerId != viewingPlayerId)) {
            // Check if the face-down card has been revealed to the viewing player (e.g., via Spy Network)
            // Also check LookAtFaceDownCreatures (e.g., Lens of Clarity) — only for battlefield creatures,
            // not face-down spells on the stack (per ruling).
            val isRevealedToViewer = visibility.isCardIdentityVisibleTo(
                state,
                zoneKey,
                entityId,
                viewingPlayerId,
                isSpectator,
            )
            // Face-down exiled cards show minimal info (not creatures, no P/T)
            return if (zoneKey.zoneType == Zone.EXILE) {
                hiddenExiledCard(frame, isRevealedToViewer)
            } else {
                hiddenFaceDownPermanent(frame, characteristics, isRevealedToViewer)
            }
        }
        return visibleCard(frame, characteristics)
    }

    // ---------------------------------------------------------------------------------------------
    // Stage 1: identity
    // ---------------------------------------------------------------------------------------------

    /** Who and what a card is, and how it is being shown in [zoneKey]. */
    private class CardFrame(
        val state: GameState,
        val entityId: EntityId,
        val zoneKey: ZoneKey,
        val projectedState: ProjectedState,
        val viewingPlayerId: EntityId,
        val isSpectator: Boolean,
        val container: ComponentContainer,
        val cardComponent: CardComponent,
        val cardDef: CardDefinition?,
        val ownerId: EntityId,
        val controllerId: EntityId,
        /** Layer-system values; present only on the battlefield. */
        val projectedValues: ProjectedValues?,
        val spellOnStack: SpellOnStackComponent?,
        val isFaceDown: Boolean,
        val faceDownDisplayMode: FaceDownMode?,
        val faceDownHelperCardImageUri: String,
    ) {
        val zoneType: Zone get() = zoneKey.zoneType
    }

    private fun frame(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        projectedState: ProjectedState,
        viewingPlayerId: EntityId,
        isSpectator: Boolean
    ): CardFrame? {
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

        // A card is face-down if it has FaceDownComponent (on battlefield/exile) OR is cast face-down on the stack.
        // Per MTG rules, face-down cards are always revealed when they leave the battlefield/stack,
        // so only allow face-down status in zones where it makes sense (defense-in-depth).
        val spellOnStack = container.get<SpellOnStackComponent>()
        val isInFaceDownZone = zoneKey.zoneType == Zone.BATTLEFIELD || zoneKey.zoneType == Zone.STACK || zoneKey.zoneType == Zone.EXILE
        // A card exiled face down that the viewer has been granted permission to PLAY
        // ("look at and play the exiled cards" — Black Cat, Cunning Thief) must not be hidden from
        // that viewer, or the client has no card data to act on the CastSpell the server offers.
        // It stays face-down (masked) to everyone else. Keyed on the same may-play check that drives
        // `playableFromExile`, so visibility and playability stay in lockstep. Scoped to exile
        // so battlefield/stack morph masking (a face-down 2/2 revealed via Spy Network stays a 2/2)
        // is unchanged.
        val viewerMayPlayThisExiledCard = !isSpectator &&
            zoneKey.zoneType == Zone.EXILE &&
            state.hasMayPlayFor(entityId, viewingPlayerId, conditionEvaluator, cardRegistry)
        val isFaceDown = isInFaceDownZone &&
            (container.has<FaceDownComponent>() || spellOnStack?.castFaceDown == true) &&
            !viewerMayPlayThisExiledCard

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        // Which face-down mechanic this object is *drawn* as: it decides the helper card every
        // surface shows in place of the hidden art (morph's helmet, the Manifest token, "A
        // Mysterious Creature" for disguise/cloak). A permanent carries the mode as a component
        // from the moment it enters, but a spell still on the stack does not — it is stamped when
        // it resolves — so derive that one from the keyword it was cast under. Without the
        // fallback a creature *being cast* with disguise would be drawn as a morph.
        val faceDownDisplayMode = container.get<FaceDownModeComponent>()?.mode
            ?: if (spellOnStack?.castFaceDown == true) FaceDownTurnUp.castMode(cardDef) else null

        return CardFrame(
            state = state,
            entityId = entityId,
            zoneKey = zoneKey,
            projectedState = projectedState,
            viewingPlayerId = viewingPlayerId,
            isSpectator = isSpectator,
            container = container,
            cardComponent = cardComponent,
            cardDef = cardDef,
            ownerId = ownerId,
            // Use projected controller for battlefield permanents (accounts for control-changing effects)
            controllerId = projectedValues?.controllerId ?: baseControllerId,
            projectedValues = projectedValues,
            spellOnStack = spellOnStack,
            isFaceDown = isFaceDown,
            faceDownDisplayMode = faceDownDisplayMode,
            faceDownHelperCardImageUri = faceDownDisplayMode?.helperCardImageUri ?: MORPH_HELPER_CARD_IMAGE_URI,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Stage 2: characteristics and battlefield status
    // ---------------------------------------------------------------------------------------------

    private class Characteristics(
        val power: Int?,
        val toughness: Int?,
        /** Keywords before PROTECTION is added for a card with protections. */
        val rawKeywords: Set<Keyword>,
        val keywords: Set<Keyword>,
        val abilityFlags: Set<AbilityFlag>,
        val protections: List<Color>,
        val hexproofFromColors: List<Color>,
        val hexproofFromMonocolored: Boolean,
        val hexproofFromMulticolored: Boolean,
        val colors: Set<Color>,
        val isTapped: Boolean,
        val isExerted: Boolean,
        val isPhasedOut: Boolean,
        val hasSummoningSickness: Boolean,
    )

    private fun characteristics(frame: CardFrame): Characteristics {
        val projectedValues = frame.projectedValues
        val cardComponent = frame.cardComponent
        val container = frame.container

        // Use projected P/T which correctly handles face-down base 2/2 + any modifications.
        // CR 208.3: a noncreature permanent has no power or toughness — even one with a printed P/T
        // (a Vehicle), and even a creature turned into an artifact/land by a type-changing effect
        // (Kitesail Larcenist's Treasure, Song of the Dryads' Forest). On the battlefield we
        // therefore only surface P/T for permanents that are creatures in projected state (face-down
        // permanents are always 2/2 creatures), so a transformed permanent stops showing a stat box.
        // Non-battlefield zones keep base P/T (a creature card in hand still shows its printed P/T).
        val onBattlefield = frame.zoneType == Zone.BATTLEFIELD
        val showsPowerToughness = !onBattlefield || frame.isFaceDown || frame.projectedState.isCreature(frame.entityId)
        val power = if (!showsPowerToughness) null
            else projectedValues?.power ?: if (frame.isFaceDown) 2 else cardComponent.baseStats?.basePower
        val toughness = if (!showsPowerToughness) null
            else projectedValues?.toughness ?: if (frame.isFaceDown) 2 else cardComponent.baseStats?.baseToughness
        val rawKeywords = projectedValues?.keywords?.mapNotNull {
            when {
                // Granted toxic floats as TOXIC_<N> (e.g. Skrelv's activated ability); collapse
                // to the bare TOXIC keyword so the icon-render path picks it up.
                it.startsWith("TOXIC_") -> Keyword.TOXIC
                else -> try { Keyword.valueOf(it) } catch (_: Exception) { null }
            }
        }?.toSet() ?: cardComponent.baseKeywords
        val abilityFlags = projectedValues?.keywords?.let(::abilityFlagsOf) ?: cardComponent.baseFlags.toSet()

        // Extract protection colors from projected keywords (PROTECTION_FROM_*) and card definition
        val projectedProtections = colorsWithPrefix(projectedValues?.keywords, PROTECTION_PREFIX)
        val staticProtections = frame.cardDef?.keywordAbilities
            ?.filterIsInstance<KeywordAbility.Protection>()
            ?.mapNotNull { (it.scope as? ProtectionScope.Color)?.color }
            ?: emptyList()
        val protections = (projectedProtections.ifEmpty { staticProtections }).distinct()

        // Add PROTECTION keyword when protections are present
        val keywords = if (protections.isNotEmpty()) rawKeywords + Keyword.PROTECTION else rawKeywords

        // Summoning sickness doesn't affect creatures with haste. The engine attaches the
        // marker to every entering permanent (so Vehicles / animated lands inherit it when
        // they become creatures), so gate on projected creature-ness here too — otherwise a
        // freshly played Mountain or Equipment would report summoning sickness to the client.
        val hasSummoningSicknessComponent = container.has<SummoningSicknessComponent>()
        val hasHaste = keywords.contains(Keyword.HASTE)
        val hasSummoningSickness = hasSummoningSicknessComponent && !hasHaste &&
            frame.projectedState.isCreature(frame.entityId)

        return Characteristics(
            power = power,
            toughness = toughness,
            rawKeywords = rawKeywords,
            keywords = keywords,
            abilityFlags = abilityFlags,
            protections = protections,
            // Extract hexproof-from-color colors from projected keywords (HEXPROOF_FROM_*).
            // Both intrinsic per-color hexproof (HexproofFromComponent) and dynamically granted
            // hexproof (Tam, Mindful First-Year) flow through the same projection keywords.
            // Non-color scopes in that namespace (HEXPROOF_FROM_CARDTYPE_INSTANT, HEXPROOF_FROM_MONOCOLORED)
            // fail Color.valueOf and drop out here — like protection-from-card-type they surface to the
            // player through the card's oracle text rather than a per-color badge.
            hexproofFromColors = colorsWithPrefix(projectedValues?.keywords, HEXPROOF_FROM_PREFIX).distinct(),
            // Hexproof from monocolored (CR 105.2) — a quality, not a color, so it rides its own flag.
            hexproofFromMonocolored = projectedValues?.keywords?.contains("HEXPROOF_FROM_MONOCOLORED") ?: false,
            // Hexproof from multicolored (CR 105.2b) — the other half of the quality pair, same shape.
            hexproofFromMulticolored = projectedValues?.keywords?.contains("HEXPROOF_FROM_MULTICOLORED") ?: false,
            colors = projectedValues?.colors?.mapNotNull {
                try { Color.valueOf(it) } catch (_: Exception) { null }
            }?.toSet() ?: cardComponent.colors,
            isTapped = container.has<TappedComponent>(),
            isExerted = container.has<ExertedComponent>(),
            isPhasedOut = container.has<PhasedOutComponent>(),
            hasSummoningSickness = hasSummoningSickness,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Stage 3: face-down masking
    // ---------------------------------------------------------------------------------------------

    private fun hiddenExiledCard(frame: CardFrame, isRevealedToViewer: Boolean): ClientCard =
        ClientCard(
            id = frame.entityId,
            name = FACE_DOWN_CARD_DISPLAY_NAME,
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
            controllerId = frame.controllerId,
            ownerId = frame.ownerId,
            isToken = false,
            zone = frame.zoneKey,
            attachedTo = null,
            attachments = emptyList(),
            isFaceDown = true,
            faceDownMode = frame.faceDownDisplayMode?.name,
            morphCost = null,
            imageUri = frame.faceDownHelperCardImageUri,
            activeEffects = emptyList(),
            revealedName = if (isRevealedToViewer) frame.cardComponent.name else null,
            revealedImageUri = revealedImageUri(frame, isRevealedToViewer)
        )

    /** A face-down battlefield/stack creature (morph) seen by someone who may not know what it is. */
    private fun hiddenFaceDownPermanent(
        frame: CardFrame,
        characteristics: Characteristics,
        isRevealedToViewer: Boolean
    ): ClientCard {
        val container = frame.container
        val keywords = frame.projectedValues?.keywords
        // Use projected keywords — granted keywords (e.g., flying from an aura) are public information
        val faceDownKeywords = keywords?.mapNotNull {
            try { Keyword.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: emptySet()
        // Extract protection colors from projected keywords for face-down creatures
        val faceDownProtections = colorsWithPrefix(keywords, PROTECTION_PREFIX)
        val faceDownKeywordsWithProtection =
            if (faceDownProtections.isNotEmpty()) faceDownKeywords + Keyword.PROTECTION else faceDownKeywords
        return ClientCard(
            id = frame.entityId,
            name = FACE_DOWN_DISPLAY_NAME,
            manaCost = "",
            manaValue = 0,
            typeLine = "Creature",
            cardTypes = setOf("CREATURE"),
            subtypes = emptySet(),
            colors = emptySet(),
            oracleText = "",
            power = characteristics.power,
            toughness = characteristics.toughness,
            basePower = 2,
            baseToughness = 2,
            damage = container.get<DamageComponent>()?.amount,
            keywords = faceDownKeywordsWithProtection,
            abilityFlags = keywords?.let(::abilityFlagsOf) ?: emptySet(),
            protections = faceDownProtections,
            hexproofFromColors = colorsWithPrefix(keywords, HEXPROOF_FROM_PREFIX).distinct(),
            counters = container.get<CountersComponent>()?.counters ?: emptyMap(),
            isTapped = characteristics.isTapped,
            isExerted = characteristics.isExerted,
            hasSummoningSickness = characteristics.hasSummoningSickness,
            isTransformed = false,
            isPhasedOut = characteristics.isPhasedOut,
            isAttacking = container.get<AttackingComponent>() != null,
            isBlocking = container.get<BlockingComponent>() != null,
            attackingTarget = container.get<AttackingComponent>()?.defenderId,
            blockingTarget = container.get<BlockingComponent>()?.blockedAttackerIds?.firstOrNull(),
            controllerId = frame.controllerId,
            ownerId = frame.ownerId,
            isToken = false,
            zone = frame.zoneKey,
            attachedTo = container.get<AttachedToComponent>()?.targetId,
            attachments = attachmentsOf(frame.state, frame.entityId),
            linkedExile = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList(),
            isFaceDown = true,
            faceDownMode = frame.faceDownDisplayMode?.name,
            morphCost = null, // Opponent can't see morph cost
            imageUri = frame.faceDownHelperCardImageUri,
            // No projected state: the projection-derived badges would describe the hidden card.
            activeEffects = activeEffects.project(frame.state, frame.entityId),
            revealedName = if (isRevealedToViewer) frame.cardComponent.name else null,
            revealedImageUri = revealedImageUri(frame, isRevealedToViewer)
        )
    }

    private fun revealedImageUri(frame: CardFrame, isRevealedToViewer: Boolean): String? =
        if (isRevealedToViewer) (frame.cardComponent.imageUri ?: frame.cardDef?.metadata?.imageUri) else null

    // ---------------------------------------------------------------------------------------------
    // Stage 4: the full card
    // ---------------------------------------------------------------------------------------------

    private fun visibleCard(frame: CardFrame, characteristics: Characteristics): ClientCard {
        val state = frame.state
        val entityId = frame.entityId
        val container = frame.container
        val cardComponent = frame.cardComponent
        val cardDef = frame.cardDef
        val spellOnStack = frame.spellOnStack
        val onBattlefield = frame.zoneType == Zone.BATTLEFIELD

        // Get combat state
        val attackingComponent = container.get<AttackingComponent>()
        val blockingComponent = container.get<BlockingComponent>()
        val attachments = attachmentsOf(state, entityId)

        val castFace = castFace(frame)
        val typeLine = displayedTypeLine(frame, castFace, characteristics.rawKeywords)
        val choices = chosenValues(frame)
        val zoneStatus = zoneStatus(frame)
        val spell = spellOnStackProjector.project(
            state, entityId, frame.zoneKey, spellOnStack, cardDef, cardComponent,
            frame.viewingPlayerId, frame.isSpectator
        )
        val faces = facesProjector.project(state, entityId, frame.zoneKey, container, cardComponent, cardDef)

        // Build active effects from floating effects
        val cardEffects = activeEffects.project(state, entityId, frame.projectedState) +
            activeEffects.notedCreatureTypeBadges(container, frame.viewingPlayerId, frame.isSpectator)

        return ClientCard(
            id = entityId,
            // A Layer-3 SetName continuous effect (Witness Protection's TransformPermanent
            // setName) overwrites the displayed name, mirroring how subtypes/types
            // already prefer the projected value over the base CardComponent. A non-permanent
            // cast face (Omen/Adventure/split half) on the stack still wins, since it has no
            // battlefield projection entry to overwrite.
            name = castFace?.name ?: frame.projectedValues?.name ?: cardComponent.name,
            manaCost = (castFace?.manaCost ?: cardComponent.manaCost).toString(),
            manaValue = (castFace?.manaCost ?: cardComponent.manaCost).cmc,
            typeLine = typeLine.text,
            cardTypes = typeLine.cardTypes.map { it.name }.toSet(),
            subtypes = typeLine.subtypes.toSet(),
            colors = if (castFace != null) castFace.manaCost.colors else characteristics.colors,
            grantedColors = grantedColors(state, attachments),
            oracleText = castFace?.oracleText ?: cardComponent.oracleText,
            // A non-permanent cast face (Omen/Adventure/split half) has no power/toughness.
            power = if (castFace != null) null else characteristics.power,
            toughness = if (castFace != null) null else characteristics.toughness,
            basePower = cardComponent.baseStats?.basePower,
            baseToughness = cardComponent.baseStats?.baseToughness,
            damage = container.get<DamageComponent>()?.amount,
            keywords = characteristics.keywords,
            abilityFlags = characteristics.abilityFlags,
            protections = characteristics.protections,
            hexproofFromColors = characteristics.hexproofFromColors,
            hexproofFromMonocolored = characteristics.hexproofFromMonocolored,
            hexproofFromMulticolored = characteristics.hexproofFromMulticolored,
            counters = container.get<CountersComponent>()?.counters ?: emptyMap(),
            isTapped = characteristics.isTapped,
            isExerted = characteristics.isExerted,
            hasSummoningSickness = characteristics.hasSummoningSickness,
            isTransformed = false, // TODO: Add transformed support
            isPhasedOut = characteristics.isPhasedOut,
            isAttacking = attackingComponent != null,
            isBlocking = blockingComponent != null,
            attackingTarget = attackingComponent?.defenderId,
            blockingTarget = blockingComponent?.blockedAttackerIds?.firstOrNull(),
            controllerId = frame.controllerId,
            // A battle's protector (CR 310.9). Absent on every other permanent.
            protectorId = container.get<ProtectorComponent>()?.playerId,
            ownerId = frame.ownerId,
            isToken = container.has<TokenComponent>(),
            // Commander flag — surfaced to the client so the UI can render a crown / gold border on
            // the commander even after it lands on the battlefield (where the gold halo on the
            // command-zone widget no longer shows). Token copies never carry CommanderComponent
            // (CR 903.10a) so this is naturally false on token clones.
            isCommander = container.has<CommanderComponent>(),
            // Ring-bearer flag — surfaced so the UI can render a prominent golden Ring icon on the
            // creature a player designated as their Ring-bearer (CR 701.54). The designation is stripped
            // on a real control change (see RingBearerComponent), so the presence of the component is
            // enough — a stolen permanent or token copy never falsely carries it.
            isRingBearer = container.has<RingBearerComponent>(),
            // Soulbond partner (CR 702.95b) — surfaced so the UI can draw the bond between the two
            // paired battlefield slots. Read through SoulbondPairing so a pair that has broken but not
            // yet been tidied up by `SoulbondPairingCheck` never surfaces a bond to a departed creature.
            pairedWithId = SoulbondPairing.partnerOf(state, entityId),
            zone = frame.zoneKey,
            attachedTo = container.get<AttachedToComponent>()?.targetId,
            attachments = attachments,
            // Linked exile (cards exiled by this permanent, e.g., Suspension Field)
            linkedExile = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList(),
            isFaceDown = frame.isFaceDown,
            faceDownMode = if (frame.isFaceDown) frame.faceDownDisplayMode?.name else null,
            isSuspected = frame.projectedValues?.isSuspected == true,
            isSolved = container.has<SolvedComponent>(),
            isRenowned = container.has<RenownedComponent>(),
            saddleRequirement = zoneStatus.saddleRequirement,
            isSaddled = zoneStatus.isSaddled,
            isPlotted = zoneStatus.isPlotted,
            isParadigm = zoneStatus.isParadigm,
            isSuspended = zoneStatus.isSuspended,
            isPrepared = zoneStatus.isPrepared,
            isPreparedSpell = zoneStatus.isPreparedSpell,
            isWarped = zoneStatus.isWarped,
            isDashed = zoneStatus.isDashed,
            morphCost = if (frame.isFaceDown) container.get<MorphDataComponent>()?.morphCost?.description else null,
            // Targets for spells/abilities on stack (for targeting arrows)
            targets = StackTextRenderer.toClientTargets(container.get<TargetsComponent>()),
            imageUri = state.imageOverrideFor(entityId)
                ?: cardDef?.metadata?.imageUriByCreatureSubtype
                    ?.entries
                    ?.firstOrNull { (subtype) -> subtype in typeLine.subtypes }
                    ?.value
                ?: cardComponent.imageUri
                ?: cardDef?.metadata?.imageUri,
            // A flipped flip card (CR 710) is the same physical card turned upside down, so its
            // single image is rotated 180° to read the flip half.
            imageRotation = ((cardDef?.metadata?.imageRotation ?: 0) +
                (if (container.has<FlippedComponent>()) 180 else 0)) % 360,
            activeEffects = cardEffects,
            rulings = cardDef?.metadata?.rulings?.map {
                ClientRuling(date = it.date, text = it.text)
            } ?: emptyList(),
            optionalCostLabel = spell.optionalCostLabel,
            castProvenanceLabel = spell.castProvenanceLabel,
            costSacrificeLabel = spell.costSacrificeLabel,
            manaPaidCost = spell.manaPaidCost,
            giftPromised = spell.giftPromised,
            wasBlightPaid = spell.wasBlightPaid,
            chosenX = spell.chosenX,
            chosenCreatureType = choices.creatureType,
            chosenColor = choices.color,
            chosenMode = choices.mode,
            chosenCardName = choices.cardName,
            chosenCardType = choices.cardType,
            sacrificedCreatureTypes = spell.sacrificedCreatureTypes,
            playableFromExile = zoneStatus.playableFromExile,
            copyOf = container.get<CopyOfComponent>()?.let { copyComp ->
                cardRegistry.getCard(copyComp.originalCardDefinitionId)?.name
            },
            // The two legendary flags are complements, and the `!in supertypes` clause is what
            // makes them so: a non-legendary copy that an effect then makes legendary again (Impostor
            // Syndrome's copy designated Ring-bearer) is simply legendary, so "not legendary" is a lie
            // about it and the client would otherwise badge it both ways at once.
            nonLegendaryCopy = onBattlefield
                && cardDef != null
                && Supertype.LEGENDARY in cardDef.typeLine.supertypes
                && Supertype.LEGENDARY !in cardComponent.typeLine.supertypes
                && Supertype.LEGENDARY !in typeLine.supertypes,
            legendaryByEffect = onBattlefield
                && Supertype.LEGENDARY in typeLine.supertypes
                && Supertype.LEGENDARY !in cardComponent.typeLine.supertypes,
            grantedSubtypes = if (onBattlefield && !typeLine.hasAllCreatureTypes) {
                grantedSubtypes(frame, typeLine.subtypes)
            } else emptySet(),
            grantedCardTypes = if (onBattlefield) {
                val printed = cardComponent.typeLine.cardTypes.map { it.name }.toSet()
                typeLine.cardTypes.map { it.name }.filterNot { it in printed }.toSet()
            } else emptySet(),
            damageDistribution = (spellOnStack?.damageDistribution ?: container.get<TriggeredAbilityOnStackComponent>()?.damageDistribution)?.takeIf { it.isNotEmpty() },
            sagaTotalChapters = cardDef?.finalChapter,
            classLevel = container.get<ClassLevelComponent>()?.currentLevel,
            classMaxLevel = cardDef?.maxClassLevel,
            thresholdInfo = conditionBadges.thresholdInfo(state, cardDef, frame.controllerId),
            deliriumInfo = conditionBadges.deliriumInfo(state, cardDef, frame.controllerId),
            stackText = spell.stackText,
            chosenModeDescriptions = spell.chosenModeDescriptions,
            perModeTargets = spell.perModeTargets,
            isDoubleFaced = faces.isDoubleFaced,
            currentFace = faces.currentFace,
            backFaceName = faces.backFaceName,
            backFaceTypeLine = faces.backFaceTypeLine,
            backFaceOracleText = faces.backFaceOracleText,
            backFaceImageUri = faces.backFaceImageUri,
            backFacePower = faces.backFacePower,
            backFaceToughness = faces.backFaceToughness,
            backFaceKeywords = faces.backFaceKeywords,
            planeswalkerAbilities = faces.planeswalkerAbilities,
            isRoom = faces.isRoom,
            isLandscapeFace = faces.isLandscapeFace,
            backFaceIsLandscape = faces.backFaceIsLandscape,
            cardFaces = faces.cardFaces,
            castFaceIndex = spellOnStack?.faceIndex,
            // Impending (CR 702.176): expose the reduced cost + time-counter count so the client can
            // always present the impending cast option (graying it out when unaffordable). Intrinsic
            // to the card definition, so it's surfaced in every zone.
            impending = cardDef?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.Impending>()
                ?.firstOrNull()
                ?.let { ClientImpending(cost = it.cost.toString(), time = it.time) },
            // Evoke (CR 702.74): same reason as impending — the alternative cost has to be on the
            // card so the menu can offer both prices even when only one of them is affordable.
            evoke = cardDef?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.Evoke>()
                ?.firstOrNull()
                ?.cost
                ?.toString()
        )
    }

    /** Compute what's attached to this card. */
    private fun attachmentsOf(state: GameState, entityId: EntityId): List<EntityId> =
        state.getBattlefield().filter { otherId ->
            state.getEntity(otherId)?.get<AttachedToComponent>()?.targetId == entityId
        }

    /**
     * Surface colours granted to this permanent by an attached "choose a colour" aura
     * (Shimmerwilds Growth: "Enchanted land is the chosen color"). The chosen colour is
     * stored on the *aura's* CastChoicesComponent, and the aura sits hidden behind its host,
     * so without this the host shows no sign of the colour it has become. We only surface a
     * colour from auras that actually grant the chosen colour to their host (they carry a
     * GrantChosenColor static ability), so an aura that picks a colour for some other reason
     * doesn't paint a misleading pip on the host.
     */
    private fun grantedColors(state: GameState, attachments: List<EntityId>): Set<Color> =
        attachments.mapNotNull { auraId ->
            val auraContainer = state.getEntity(auraId) ?: return@mapNotNull null
            val grantsColor = auraContainer.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.script?.staticAbilities?.any { it is GrantChosenColor } == true
            if (grantsColor) auraContainer.chosenColor() else null
        }.toSet()

    /**
     * A spell cast as a non-permanent secondary face (an Omen, an Adventure, or a split half) is
     * — while it sits on the stack — that face, not the card's default permanent characteristics.
     * Per [com.wingedsheep.sdk.model.CardLayout.OMEN]: "from every zone other than the stack the
     * card is just the Dragon"; on the stack it's the Omen spell (e.g. Petty Revenge). Without
     * this, casting the Omen showed the Dragon's name/type/text/P-T on the stack. We only swap in
     * spell faces (instant/sorcery) — a modal-DFC permanent back keeps its own handling.
     */
    private fun castFace(frame: CardFrame): CardFace? =
        if (frame.zoneType == Zone.STACK) {
            frame.spellOnStack?.faceIndex
                ?.let { frame.cardDef?.cardFaces?.getOrNull(it) }
                ?.takeIf { !it.typeLine.isPermanent }
        } else null

    // ---- type line --------------------------------------------------------------------------

    private class DisplayedTypeLine(
        val text: String,
        val cardTypes: List<CardType>,
        /** The full (projected) subtype list — what the DTO's `subtypes` carries. */
        val subtypes: List<String>,
        val supertypes: List<Supertype>,
        val hasAllCreatureTypes: Boolean,
    )

    /** Build type line string from TypeLine, using projected types/subtypes if available. */
    private fun displayedTypeLine(frame: CardFrame, castFace: CardFace?, rawKeywords: Set<Keyword>): DisplayedTypeLine {
        val typeLine: TypeLine = castFace?.typeLine ?: frame.cardComponent.typeLine
        val projectedSubtypes = frame.projectedValues?.subtypes?.toList()
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
        val projectedTypes = frame.projectedValues?.types
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
        return DisplayedTypeLine(
            text = typeLineString,
            cardTypes = displayCardTypes,
            subtypes = displaySubtypes,
            supertypes = displaySupertypes,
            hasAllCreatureTypes = hasAllCreatureTypes,
        )
    }

    /**
     * Subtypes the permanent has beyond its printed ones — the same projected-minus-printed shape as
     * `legendaryByEffect`. Skipped by the caller for the all-creature-types case, which the type line
     * already collapses.
     */
    private fun grantedSubtypes(frame: CardFrame, displaySubtypes: List<String>): Set<String> {
        val printed = frame.cardComponent.typeLine.subtypes.map { it.value }.toSet()
        // Subtypes a *floating* effect is responsible for already have their own badge —
        // "+Hero" for AddSubtype (`type_added`) and the full list for SetCreatureSubtypes
        // (`type_changed`), both built by CardActiveEffectsProjector from those effects directly.
        // Repeating them here would show the same grant twice in the preview. What this field is
        // actually for is the case those badges miss: a grant from a continuous *static* ability,
        // such as an Aura's "is a legendary Soldier in addition to its other types".
        val alreadyBadged = frame.state.floatingEffects
            .filter { frame.entityId in it.effect.affectedEntities }
            .flatMap {
                when (val mod = it.effect.modification) {
                    is SerializableModification.AddSubtype -> listOf(mod.subtype)
                    is SerializableModification.SetCreatureSubtypes -> displaySubtypes
                    else -> emptyList()
                }
            }
            .toSet()
        return displaySubtypes.filterNot { it in printed || it in alreadyBadged }.toSet()
    }

    // ---- choices ----------------------------------------------------------------------------

    private class ChosenValues(
        val creatureType: String?,
        val color: String?,
        val mode: String?,
        val cardName: String?,
        val cardType: String?,
    )

    private fun chosenValues(frame: CardFrame): ChosenValues {
        val container = frame.container
        return ChosenValues(
            // Get chosen creature type for "as enters" permanents (e.g., Doom Cannon) or spells on stack (e.g., Aphetto Dredging).
            // Note: temporary type changes from floating SetCreatureSubtypes effects (e.g., Mistform Wall, Figure of Fable)
            // are surfaced via the "type-change" active-effect badge in CardActiveEffectsProjector, not as a chosen-type label.
            creatureType = container.chosenCreatureType() ?: frame.spellOnStack?.chosenCreatureType,
            // Get chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator)
            color = container.chosenColor()?.displayName,
            mode = chosenModeLabel(frame),
            // Get chosen card name for "as enters, choose a card name" permanents (e.g., Petrified Hamlet)
            cardName = container.chosenCardName(),
            // Get chosen card type for "choose a card type" permanents (e.g., Arachne, Psionic Weaver)
            cardType = container.chosenCardType(),
        )
    }

    /**
     * Get chosen mode label for "as enters, choose X or Y" permanents (e.g., Outpost Siege).
     * Resolve the stored mode id back to the display label declared in the card's
     * EntersWithChoice(modeOptions = [...]) so the UI can show the human-friendly name.
     * Riot's counter/haste mode is a one-time enters-with choice, not an ongoing mode — its
     * effect is already visible as a +1/+1 counter or the haste keyword — so it gets no badge.
     */
    private fun chosenModeLabel(frame: CardFrame): String? = frame.container
        .chosenModeId()
        ?.takeUnless { it == RIOT_MODE_COUNTER || it == RIOT_MODE_HASTE }
        ?.let { modeId ->
            val modeOptions = frame.cardDef?.script?.replacementEffects
                ?.filterIsInstance<EntersWithChoice>()
                ?.firstOrNull { it.choiceType == ChoiceType.MODE }
                ?.modeOptions
                .orEmpty()
            modeOptions.firstOrNull { it.id == modeId }?.label ?: modeId
        }

    // ---- zone-specific status ---------------------------------------------------------------

    private class ZoneStatus(
        val playableFromExile: Boolean,
        val isPlotted: Boolean,
        val isParadigm: Boolean,
        val isSuspended: Boolean,
        val isPrepared: Boolean,
        val isPreparedSpell: Boolean,
        val isWarped: Boolean,
        val isDashed: Boolean,
        val saddleRequirement: Int?,
        val isSaddled: Boolean,
    )

    private fun zoneStatus(frame: CardFrame): ZoneStatus {
        val container = frame.container
        val inExile = frame.zoneType == Zone.EXILE
        val onBattlefield = frame.zoneType == Zone.BATTLEFIELD
        return ZoneStatus(
            // Check if this card is playable from exile (impulse draw like Mind's Desire,
            // or cast-from-linked-exile like Rona / Dawnhand Dissident).
            playableFromExile = inExile && (
                frame.state.hasMayPlayFor(frame.entityId, frame.viewingPlayerId, conditionEvaluator, cardRegistry) ||
                    // The legality kernel's linked-exile answer, not a view-layer re-derivation. The
                    // "right now" gates (during your turn, once per turn, the mana-value cap) are
                    // skipped: the flag marks a pile the viewer can cast from, even if not this moment.
                    legality.linkedExileGranterFor(frame.state, frame.viewingPlayerId, frame.entityId, usableNow = false) != null
                ),
            // Plotted cards (CR 718) sit face-up in exile with a PlottedComponent; surface a flag so the
            // client can badge them as plotted (otherwise indistinguishable from any other exiled card).
            isPlotted = inExile && container.has<PlottedComponent>(),
            // Active paradigm cards (Secrets of Strixhaven) sit face-up in exile with a ParadigmComponent,
            // recasting a free copy of themselves each precombat main; surface a flag so the client can show
            // them in a dedicated public pile (otherwise indistinguishable from any other exiled card).
            isParadigm = inExile && container.has<ParadigmComponent>(),
            // Suspended cards (CR 702.62) sit face-up in exile with a SuspendedComponent, counting down at
            // the owner's upkeep; surface a flag so the client can show them in a dedicated public pile
            // (otherwise indistinguishable from any other exiled card). CR 702.62b: "suspended" also
            // requires at least one time counter — the marker alone lingers after the owner declines the
            // free cast at zero counters (see SuspendCardFromHandHandler), and that leftover shouldn't
            // read as an active countdown.
            isSuspended = inExile &&
                container.has<SuspendedComponent>() &&
                (container.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0) > 0,
            // Prepared permanents (Secrets of Strixhaven) carry a PreparedComponent while a copy of their
            // prepare spell waits castable in exile; surface a flag so the client can badge the creature.
            isPrepared = onBattlefield && container.has<PreparedComponent>(),
            // The exiled prepare-spell copy carries a PreparedSpellCopyComponent. It surfaces as a castable
            // ghost card in the controller's hand; flag it so the client can badge it as coming from a
            // prepared creature (rather than reading like a generic impulse-draw exile card).
            isPreparedSpell = inExile && container.has<PreparedSpellCopyComponent>(),
            // Warped permanents (CR 702.185, Edge of Eternities) carry a WarpedComponent until they're
            // exiled at the next end step; surface a flag so the client can show the cosmic warp cue.
            isWarped = onBattlefield && container.has<WarpedComponent>(),
            // Dashed permanents (CR 702.109, Khans of Tarkir) carry a DashedComponent until they're
            // returned to hand at the next end step; surface a flag so the client can show a dash cue.
            isDashed = onBattlefield && container.has<DashedComponent>(),
            // Mounts (CR 702.171): surface the printed Saddle N and whether the permanent currently
            // carries the saddled designation, so the client can badge both states. Read from the card
            // definition for the same reason `SaddleEnumerator` does — the handler resolves the saddle
            // keyword by definition id, so a renamed copy (CR 707.9) still saddles.
            saddleRequirement = if (onBattlefield) {
                frame.cardDef?.keywordAbilities
                    ?.filterIsInstance<KeywordAbility.Numeric>()
                    ?.firstOrNull { it.keyword == Keyword.SADDLE }
                    ?.n
            } else {
                null
            },
            isSaddled = onBattlefield && container.has<SaddledComponent>(),
        )
    }

    private companion object {
        const val PROTECTION_PREFIX = "PROTECTION_FROM_"
        const val HEXPROOF_FROM_PREFIX = "HEXPROOF_FROM_"

        /** The colours named by projected keywords of the form `<prefix><COLOR>`; other scopes drop out. */
        fun colorsWithPrefix(keywords: Set<String>?, prefix: String): List<Color> =
            keywords
                ?.filter { it.startsWith(prefix) }
                ?.mapNotNull { try { Color.valueOf(it.removePrefix(prefix)) } catch (_: Exception) { null } }
                ?: emptyList()

        fun abilityFlagsOf(keywords: Set<String>): Set<AbilityFlag> =
            keywords.mapNotNull { try { AbilityFlag.valueOf(it) } catch (_: Exception) { null } }.toSet()
    }
}
