package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionResult
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import kotlin.reflect.KClass

/**
 * Executor for [TransformEffect] (CR 701.27).
 *
 * Swaps the target's [CardComponent] to the opposite face of its [DoubleFacedComponent].
 * Counters, damage, attachments, controller, and timestamp all persist — only the identity
 * characteristics (name, type line, P/T, keywords, colors, oracle text, abilities) change.
 *
 * The card's static abilities' [ContinuousEffectSourceComponent] and
 * [ReplacementEffectSourceComponent] are rebuilt from the new face's definition so that
 * layer projection picks up the new face's static abilities immediately.
 *
 * Emits a [TransformedEvent] so triggered abilities keyed on "when this transforms"
 * fire through the standard trigger pipeline.
 */
class TransformEffectExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry
) : EffectExecutor<TransformEffect> {

    override val effectType: KClass<TransformEffect> = TransformEffect::class

    override fun execute(
        state: GameState,
        effect: TransformEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for transform")

        if (state.getEntity(targetId) == null) {
            return EffectResult.error(state, "Target entity not found")
        }

        // CR 702.145b#3 / 702.145e#2 — a permanent with daybound or nightbound "can't transform except
        // due to its daybound/nightbound ability." Those keyword-driven transforms don't come through
        // this executor (they route through DayNightService.flipDfcInPlace on a day/night change), so
        // any TransformEffect reaching a daybound/nightbound permanent is a disallowed "other" cause
        // and does nothing.
        val projected = state.projectedState
        if (projected.hasKeyword(targetId, Keyword.DAYBOUND) ||
            projected.hasKeyword(targetId, Keyword.NIGHTBOUND)
        ) {
            return EffectResult.success(state)
        }

        // CR 701.28f — an activated or triggered ability of a permanent that tries to transform
        // *that same permanent* does so only if the permanent hasn't transformed or converted
        // since the ability was put onto the stack; otherwise the instruction is ignored. The
        // shape it protects is a countdown card whose triggers can stack up: two creature cards
        // hit the graveyard at once while Soulcipher Board is on its last omen counter, and
        // without this the first trigger flips it to Cipherbound Spirit and the second — finding
        // no counters to remove and so passing its own "if it has no omen counters" check —
        // flips it straight back. Same story for Thing in the Ice on one ice counter with two
        // spells on the stack.
        if (targetId == context.sourceId &&
            context.sourceFaceChanges != null &&
            context.sourceFaceChanges != state.getEntity(targetId)
                ?.get<DoubleFacedComponent>()?.faceChanges
        ) {
            return EffectResult.success(state)
        }

        // A non-DFC target is a silent no-op (CR 701.27b — "if a permanent that isn't a
        // transforming double-faced permanent would transform, nothing happens").
        val (newState, event) = flipDfcInPlace(state, cardRegistry, targetId)
            ?: return EffectResult.success(state)

        return EffectResult.success(newState, listOf(event))
    }

}

/**
 * Flip the double-faced permanent [entityId] to its opposite face **in place** on the battlefield
 * (CR 701.27a) and return the new state paired with the [TransformedEvent] the flip emits, or `null`
 * when [entityId] is not a double-faced permanent (the caller decides whether that's a no-op or an
 * error). The entity id is stable; counters, damage, attachments, controller, and timestamp persist
 * — only the identity characteristics change, and the new face's static/replacement effects are
 * re-registered so layer projection picks them up on the next projection.
 *
 * The single shared implementation behind both [TransformEffectExecutor] (the [TransformEffect]
 * one-shot) and [com.wingedsheep.engine.mechanics.daynight.DayNightService] (the immediate
 * daybound/nightbound transforms of CR 702.145c/f). Routing both through here guarantees a day/night
 * transform is byte-identical to any other transform and emits the same [TransformedEvent], so
 * "whenever this transforms" triggers (e.g. Wildsong Howler) fire identically however the flip was
 * caused — and it makes this the one place a "can't transform" restriction has to be honored.
 */
internal fun flipDfcInPlace(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId
): Pair<GameState, TransformedEvent>? {
    // CR 701.27b — a permanent that can't transform simply doesn't (Bound by Moonsilver). Checked
    // here rather than in the callers so it covers every cause of a transform, including the
    // daybound/nightbound day-change flips that bypass TransformEffectExecutor entirely.
    if (state.projectedState.hasKeyword(entityId, AbilityFlag.CANT_TRANSFORM)) return null

    val container = state.getEntity(entityId) ?: return null
    val dfc = container.get<DoubleFacedComponent>() ?: return null
    val currentCard = container.get<CardComponent>() ?: return null

    // Rule 701.27a: transforming a DFC flips to the opposite face.
    val nextFace = when (dfc.currentFace) {
        DoubleFacedComponent.Face.FRONT -> DoubleFacedComponent.Face.BACK
        DoubleFacedComponent.Face.BACK -> DoubleFacedComponent.Face.FRONT
    }
    val intoBackFace = nextFace == DoubleFacedComponent.Face.BACK

    val nextDefinitionId = when (nextFace) {
        DoubleFacedComponent.Face.FRONT -> dfc.frontCardDefinitionId
        DoubleFacedComponent.Face.BACK -> dfc.backCardDefinitionId
    }
    val nextCardDef = cardRegistry.getCard(nextDefinitionId) ?: return null
    // CR 712.10 — transforming into an instant or sorcery face does nothing (a Siege whose back is
    // a sorcery, Invasion of Kylem). That face is reachable only by casting the card transformed.
    if (!nextCardDef.isPermanent) return null

    // A DFC on the battlefield always has a controller; fall back to owner, and treat a truly
    // owner-less object as un-flippable (null → the caller's no-op contract) rather than fabricate an id.
    val controllerId = container.get<ControllerComponent>()?.playerId ?: currentCard.ownerId ?: return null

    // CR 712.8e: a nonmodal DFC keeps its *front* face's mana value while the back is up.
    // `currentCard` is the front face on the way in, so its mana value is that number; flipping to
    // the front instead clears the override, since the front's own cost is the answer again.
    val newState = setDfcFace(
        state, cardRegistry, entityId, nextFace,
        manaValueOverride = if (intoBackFace) {
            dfcBackFaceManaValue(cardRegistry.getCard(dfc.frontCardDefinitionId), currentCard.manaValue)
        } else null,
    ) ?: return null

    return newState to TransformedEvent(
        entityId = entityId,
        intoBackFace = intoBackFace,
        newFaceName = nextCardDef.name,
        controllerId = controllerId
    )
}

/**
 * Put the double-faced permanent [entityId] onto [nextFace] **in place**: swap its
 * [CardComponent] to that face's definition, record the face on its [DoubleFacedComponent], and
 * re-register the face's static / replacement effects so layer projection picks them up on the
 * next projection. Returns null when [entityId] is not a face-tracked card or the face's
 * definition is not in the registry.
 *
 * Deliberately says nothing about *why* the face changed: it emits no event, checks no
 * "can't transform" restriction, and does not care whether the target face is the current one.
 * The two callers want different things around it —
 *
 *  - [flipDfcInPlace] adds the CR 701.27b restriction check and the [TransformedEvent] that
 *    "whenever this transforms" triggers key on;
 *  - the play-land path (CR 712.12 — *"A player playing a modal double-faced card as a land
 *    chooses one of its faces that's a land before putting it onto the battlefield. It enters the
 *    battlefield with that face up."*) wants neither. Choosing a face on the way in is not a
 *    transform (CR 712.9 excludes modal DFCs from transforming at all), so it must not fire
 *    transform triggers and must not be stoppable by a "can't transform" effect.
 *
 * @param manaValueOverride Pins the resulting [CardComponent]'s mana value, for the CR 712.8e case
 *   where a nonmodal DFC's back face keeps the *front* face's mana value. Null leaves the face's
 *   own cost to answer, which is what CR 712.8f says for a modal DFC.
 */
internal fun setDfcFace(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
    nextFace: DoubleFacedComponent.Face,
    manaValueOverride: Int? = null,
): GameState? {
    val container = state.getEntity(entityId) ?: return null
    val dfc = container.get<DoubleFacedComponent>() ?: return null
    val currentCard = container.get<CardComponent>() ?: return null

    val nextDefinitionId = when (nextFace) {
        DoubleFacedComponent.Face.FRONT -> dfc.frontCardDefinitionId
        DoubleFacedComponent.Face.BACK -> dfc.backCardDefinitionId
    }
    val nextCardDef = cardRegistry.getCard(nextDefinitionId) ?: return null

    val swappedCard = buildCardComponentForDfcFace(currentCard, nextCardDef, manaValueOverride)

    // Rule 712.8a: save the front face card so ZoneTransitionService can restore it
    // without a registry lookup when the DFC leaves the battlefield on its back face.
    // The face-change tally advances on every real swap — CR 701.28f's clock, see
    // [DoubleFacedComponent.faceChanges].
    val faceChanges = dfc.faceChanges + if (nextFace == dfc.currentFace) 0 else 1
    val updatedDfc = when (nextFace) {
        DoubleFacedComponent.Face.BACK ->
            dfc.copy(currentFace = nextFace, frontFaceCard = currentCard, faceChanges = faceChanges)
        DoubleFacedComponent.Face.FRONT ->
            dfc.copy(currentFace = nextFace, frontFaceCard = null, faceChanges = faceChanges)
    }

    val staticAbilityHandler = StaticAbilityHandler(cardRegistry)
    return state.updateEntity(entityId) { c ->
        var updated = c
            .with(swappedCard)
            .with(updatedDfc)
            // Strip stale static-ability effect components so the layer projector stops
            // applying the old face's static abilities on the very next projection.
            .without<ContinuousEffectSourceComponent>()
            .without<ReplacementEffectSourceComponent>()

        // Re-register the new face's static and replacement effects.
        updated = staticAbilityHandler.addContinuousEffectComponent(updated, nextCardDef)
        updated = staticAbilityHandler.addReplacementEffectComponent(updated, nextCardDef)
        updated = withFaceIntrinsicComponents(updated, nextCardDef)
        updated
    }
}

/**
 * Turn [entityId] back to its front face if it is a double-faced card sitting on its back face —
 * CR 712.8a: outside the battlefield and the stack a double-faced card has only its front face's
 * characteristics. Restores the front-face [CardComponent] stashed on the [DoubleFacedComponent]
 * when the card went to its back face; a no-op for anything else.
 *
 * The front face's own "from anywhere" self-replacements come back with it — and, just as
 * importantly, the back face's stop applying. A disturbed creature that is exiled by its own
 * back-face clause reverts to a plain front face.
 *
 * [ZoneTransitionService] calls this on every move to a zone other than the battlefield or stack.
 * The spell resolver's own graveyard moves call it too: a card cast transformed whose back face is
 * an instant or sorcery (Invasion of Kylem's Valor's Reach Tag Team) leaves the stack through them,
 * not through the service.
 */
internal fun restoreDfcFrontFace(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
): GameState {
    val dfc = state.getEntity(entityId)?.get<DoubleFacedComponent>() ?: return state
    val frontFaceCard = dfc.frontFaceCard ?: return state
    if (!dfc.isBack) return state
    val frontDef = cardRegistry.getCard(dfc.frontCardDefinitionId)
    return state.updateEntity(entityId) { c ->
        val reverted = c.with(frontFaceCard)
            .with(dfc.copy(currentFace = DoubleFacedComponent.Face.FRONT, frontFaceCard = null))
        if (frontDef != null) withFaceIntrinsicComponents(reverted, frontDef) else reverted
    }
}

/**
 * Give [entityId] the front-face [DoubleFacedComponent] it needs to be turned over later, if its
 * card definition has a back face and it doesn't already carry one. The two guards are exactly
 * those: a definition with no `backFace` is a no-op, and so is an entity whose face is already
 * being tracked (so a caller that deliberately stamped a *back* face — [returnDfcFace] — is never
 * clobbered).
 *
 * Note there is deliberately **no token guard**, because "is this a card" is not the question here.
 * Face tracking is about the *object*, and a token copy of a double-faced permanent is a
 * double-faced token that can transform (CR 707.8a / 712.9) even though it is not a double-faced
 * card (CR 111.1) — the card-level question is [CardComponent.isDoubleFaced], which the token-copy
 * executors clear. In practice every token path stamps its own [DoubleFacedComponent] (copying the
 * original's `currentFace`, so a back-face token copy stays on its back face) and therefore hits
 * the already-tracked guard before reaching this; a future token path that didn't would get the
 * front-face default, which is the right answer for a token created face up.
 *
 * That component is what [flipDfcInPlace] keys on, so without it a transform is silently a no-op.
 * The cast pipeline stamps it as a resolving permanent spell enters (CR 712.13, `StackResolver`),
 * but nothing else did: a double-faced card put onto the battlefield by an *effect* — reanimated,
 * fetched out of a library, returned from exile — arrived untracked, and "you may transform it"
 * quietly did nothing to it. Called from the two places that between them cover every battlefield
 * entry a cast doesn't:
 *  - [com.wingedsheep.engine.handlers.effects.ZoneTransitionService.applyBattlefieldEntry], the
 *    funnel every effect-driven entry goes through, and
 *  - `PlayLandHandler`, because playing a land is a special action that deliberately bypasses that
 *    funnel — so a double-faced land (Balamb Garden, SeeD Academy) needed its own call.
 *
 * Front face by construction: CR 712.14 — "a double-faced card put onto the battlefield from a zone
 * other than the stack enters the battlefield with its front face up by default" (CR 712.8a says the
 * same thing from the other side: outside the battlefield and stack the card has only its front
 * face's characteristics). A caller that wants a *back*-face entry stamps it itself before the move.
 */
internal fun stampDoubleFacedFrontFace(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId
): GameState {
    val container = state.getEntity(entityId) ?: return state
    if (container.get<DoubleFacedComponent>() != null) return state
    val cardDefinitionId = container.get<CardComponent>()?.cardDefinitionId ?: return state
    val cardDef = cardRegistry.getCard(cardDefinitionId) ?: return state
    val backFace = cardDef.backFace ?: return state
    return state.updateEntity(entityId) { c ->
        c.with(
            DoubleFacedComponent(
                frontCardDefinitionId = cardDef.name,
                backCardDefinitionId = backFace.name,
                currentFace = DoubleFacedComponent.Face.FRONT
            )
        )
    }
}

/**
 * Re-derive the entity's face-intrinsic components from [face]: the card-intrinsic "would be put
 * into [zone] from anywhere → redirect instead" self-replacements ([SelfZoneRedirectComponent]) and
 * the printed protection keywords ([ProtectionComponent]).
 *
 * Those components are normally built once, at entity creation, from the printed front face — so
 * without this a face swap would leave the wrong face's redirects in place. It matters for the
 * disturb cycle, whose back faces each print "If ~ would be put into a graveyard from anywhere,
 * exile it instead": the clause has to start applying the moment the card becomes a back-face
 * object (CR 614.12 — it functions in every zone, so a countered disturb spell is exiled rather
 * than put into the graveyard), and stop applying when Rule 712.8a turns the card back over.
 *
 * Called from every face swap: [flipDfcInPlace], [returnDfcFace], `flipPermanent` (flip cards), the disturb cast in
 * `StackResolver.castSpell`, and the 712.8a / 710.4 restores in `ZoneTransitionService`.
 */
internal fun withFaceIntrinsicComponents(
    container: ComponentContainer,
    face: CardDefinition
): ComponentContainer {
    val redirects = face.script.replacementEffects
        .filterIsInstance<RedirectZoneChange>()
        .filter { it.selfOnly }
    val withRedirects = if (redirects.isEmpty()) {
        container.without<SelfZoneRedirectComponent>()
    } else {
        container.with(SelfZoneRedirectComponent(redirects))
    }
    // Printed protection is likewise stamped once at creation; a face that prints it (Tok-Tok,
    // Volcano Born) must gain it, and a face that doesn't must lose it.
    return CardEntityFactory.protectionComponentFor(face)
        ?.let { withRedirects.with(it) }
        ?: withRedirects.without<ProtectionComponent>()
}

/**
 * The mana value a double-faced object keeps while its **back** face is up, or null when the back
 * face's own mana cost is already the right answer. The one place the modal/nonmodal split is
 * decided; pass the result to [buildCardComponentForDfcFace] as `manaValueOverride`.
 *
 * A **nonmodal** DFC calculates its mana value from the *front* face's mana cost while the back is
 * up — CR 712.8c says so for a spell on the stack (a disturb cast) and CR 712.8e for a permanent on
 * the battlefield. Since a transform card's back prints no mana cost at all, taking the back's own
 * cost would make a transformed Delver of Secrets mana value 0 instead of 1.
 *
 * A **modal** DFC has no such exception: CR 712.8f gives it "only the characteristics of the face
 * that's up", so The Sensational She-Hulk keeps the 6 from her own `{3}{G}{W}{W}` however she got
 * there. That is the whole difference, and it is why this takes the front *definition* rather than
 * just a number.
 *
 * An unresolvable [frontDef] falls back to the nonmodal rule, which covers all but a handful of
 * cards.
 *
 * Not called when flipping *to* the front face: there the front's own cost is the answer and the
 * override must be cleared, which [buildCardComponentForDfcFace]'s default already does.
 *
 * (CR 712.8e's other clause — a permanent *copying* a nonmodal back face has mana value 0, CR
 * 202.3b — is a property of the copy, not of the flip, and is not modelled here.)
 */
internal fun dfcBackFaceManaValue(frontDef: CardDefinition?, frontManaValue: Int): Int? =
    if (frontDef?.layout == CardLayout.MODAL_DFC) null else frontManaValue

/**
 * Build a fresh [CardComponent] for the given DFC face while preserving the permanent's
 * owner identity and the art of the printing the player actually brought. Shared by
 * [TransformEffectExecutor] (CR 701.27 transform on the battlefield) and
 * [ReturnSelfFromExileTransformedExecutor] (CR 702.167 Craft return).
 *
 * **The two image fields swap on every flip.** `CardEntityFactory` stamps both from the chosen
 * printing — `imageUri` is the face that's up, `backFaceImageUri` the other one — so trading them
 * is exactly what turning the card over means, in either direction, and the pinned printing's art
 * survives any number of flips. Falling through to `face.metadata.imageUri` instead (the old
 * behaviour) reached for the *canonical* printing's art, which is why a transformed Innistrad
 * Remastered Garruk Relentless came back wearing his original Innistrad face.
 *
 * @param manaValueOverride What [dfcBackFaceManaValue] returned when [face] is the **back** face;
 *   null when flipping to the front, which clears any override the back face carried.
 */
internal fun buildCardComponentForDfcFace(
    current: CardComponent,
    face: CardDefinition,
    manaValueOverride: Int? = null,
): CardComponent = CardComponent(
    cardDefinitionId = face.name,
    name = face.name,
    manaCost = face.manaCost,
    typeLine = face.typeLine,
    oracleText = face.oracleText,
    baseStats = face.creatureStats,
    baseKeywords = face.keywords,
    baseFlags = face.flags,
    colors = face.colors,
    ownerId = current.ownerId,
    spellEffect = face.spellEffect,
    imageUri = current.backFaceImageUri ?: face.metadata.imageUri ?: current.imageUri,
    backFaceImageUri = current.imageUri,
    // Presentation and rules identity both belong to the whole card, not to the face that's up:
    // the printing keeps minting its own token art after a flip, and "originally printed in X"
    // (City in a Bottle) keeps matching a werewolf that transformed.
    printingSetCode = current.printingSetCode,
    originalSetCode = current.originalSetCode,
    // Being double-faced belongs to the card, not the face that's up — and it can't be re-derived
    // from [face], since a back face's own definition carries no back face. Carrying it keeps
    // `CardPredicate.IsDoubleFaced` true on a permanent sitting on its back face.
    isDoubleFaced = current.isDoubleFaced,
    // Carry the destination face's precomputed ability flags so predicates like
    // CardPredicate.HasActivatedAbility / HasNonManaActivatedAbility see the face that's actually up
    // (otherwise a transformed permanent silently reports the default `false`).
    hasNonManaActivatedAbility = face.hasNonManaActivatedAbility,
    hasActivatedAbility = face.hasActivatedAbility,
    manaValueOverride = manaValueOverride,
)

/**
 * Flip a double-faced entity that is currently in a non-battlefield zone (exile or graveyard) to
 * [destinationFace] and return it to the battlefield as a new object under its owner's control.
 *
 * The face swap is applied while the entity is still in its source zone so the → BATTLEFIELD move
 * registers the destination face's static abilities and (for a Saga face) the lore-counter entry
 * setup cleanly. Per Rule 712.8a the front face's [CardComponent] is stashed on the
 * [DoubleFacedComponent] when going to the back face, so the restore-on-leave path can swap back
 * without a registry lookup.
 *
 * Shared by [ReturnSelfFromExileTransformedExecutor] (CR 702.167a Craft return — always to the
 * back face), [ExileAndReturnTransformedExecutor] (FIN Dominant / eikon — either direction), and
 * [ReturnSelfFromZoneTransformedExecutor] (graveyard return — LCI god cycle). The caller is
 * responsible for the entity already being in the zone it is to be returned from.
 */
internal fun returnDfcFace(
    zones: ZoneTransitionService,
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
    destinationFace: DoubleFacedComponent.Face,
    tapped: Boolean = false
): ZoneTransitionResult {
    val container = state.getEntity(entityId)
        ?: return ZoneTransitionResult(state, emptyList())
    val currentCard = container.get<CardComponent>()
        ?: return ZoneTransitionResult(state, emptyList())
    val ownerId = container.get<OwnerComponent>()?.playerId ?: currentCard.ownerId
        ?: return ZoneTransitionResult(state, emptyList())
    val prepared = prepareDfcFaceSwap(state, cardRegistry, entityId, destinationFace)
        ?: return ZoneTransitionResult(state, emptyList())
    return zones.moveToZone(
        prepared,
        entityId,
        Zone.BATTLEFIELD,
        options = ZoneEntryOptions(controllerId = ownerId, tapped = tapped)
    )
}

/**
 * Swap [entityId] to [destinationFace] **in place**, without moving it between zones — the half of
 * [returnDfcFace] that decides *which face is up*, split out so a caller that owns its own
 * battlefield-entry path can reuse it (Radiant Grace returns transformed *and* attached, and its
 * attachment/controller wiring lives in `ReturnSelfToBattlefieldAttachedExecutor`).
 *
 * Returns `null` — and leaves the caller's state untouched — when the entity isn't a double-faced
 * card that can show [destinationFace]. Callers whose card may not carry a [DoubleFacedComponent]
 * yet (anything reached from a zone the card was discarded or milled into) run
 * [ensureDoubleFacedComponent] first; this function deliberately does not, so the existing
 * transform paths keep their "no component, no flip" behavior.
 *
 * Per Rule 712.8a the front face's [CardComponent] is stashed on the [DoubleFacedComponent] when
 * going to the back face, so the restore-on-leave path can swap back without a registry lookup.
 */
internal fun prepareDfcFaceSwap(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
    destinationFace: DoubleFacedComponent.Face,
): GameState? {
    val container = state.getEntity(entityId) ?: return null
    val dfc = container.get<DoubleFacedComponent>() ?: return null
    val currentCard = container.get<CardComponent>() ?: return null

    val destinationDefinitionId = when (destinationFace) {
        DoubleFacedComponent.Face.FRONT -> dfc.frontCardDefinitionId
        DoubleFacedComponent.Face.BACK -> dfc.backCardDefinitionId
    }
    val destinationDef = cardRegistry.getCard(destinationDefinitionId)
        ?: return null
    // CR 712.14a with CR 400.4a — a card told to enter the battlefield with an instant or sorcery
    // face up stays in its current zone; `null` is exactly the callers' "don't move it" answer.
    if (!destinationDef.isPermanent) return null

    // `currentCard` is the front face here (Rule 712.8a — the entity reverted to it on leaving the
    // battlefield), so it supplies the CR 712.8e mana value a nonmodal back face keeps.
    val destinationCard = buildCardComponentForDfcFace(
        currentCard,
        destinationDef,
        manaValueOverride = if (destinationFace == DoubleFacedComponent.Face.BACK) {
            dfcBackFaceManaValue(cardRegistry.getCard(dfc.frontCardDefinitionId), currentCard.manaValue)
        } else null,
    )
    val returnFaceChanges = dfc.faceChanges + if (destinationFace == dfc.currentFace) 0 else 1
    val updatedDfc = when (destinationFace) {
        // currentCard is the front face here (the entity reverts to its front face on leaving the
        // battlefield, Rule 712.8a) — stash it so the back face can restore it on its next exit.
        DoubleFacedComponent.Face.BACK -> dfc.copy(
            currentFace = destinationFace, frontFaceCard = currentCard, faceChanges = returnFaceChanges
        )
        DoubleFacedComponent.Face.FRONT -> dfc.copy(
            currentFace = destinationFace, frontFaceCard = null, faceChanges = returnFaceChanges
        )
    }

    return state.updateEntity(entityId) { c ->
        withFaceIntrinsicComponents(c.with(destinationCard).with(updatedDfc), destinationDef)
    }
}

/**
 * Stamp a [DoubleFacedComponent] onto [entityId] when its card definition has a back face but the
 * entity carries no component yet, and return the updated state; `null` when the card is not
 * double-faced at all.
 *
 * The component is only stamped when a DFC *spell* resolves onto the battlefield, so a card that
 * was discarded, milled, or (as here) put into the graveyard as a state-based action can reach a
 * "return it transformed" effect without one. A single-faced card told to enter transformed
 * doesn't move at all (standing FIN ruling), which is what the `null` return lets callers express.
 */
internal fun ensureDoubleFacedComponent(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId,
): GameState? {
    val container = state.getEntity(entityId) ?: return null
    val cardComponent = container.get<CardComponent>() ?: return null
    val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
    val backFace = cardDef?.backFace ?: return null
    if (container.get<DoubleFacedComponent>() != null) return state
    return state.updateEntity(entityId) { c ->
        c.with(
            DoubleFacedComponent(
                frontCardDefinitionId = cardDef.name,
                backCardDefinitionId = backFace.name,
                currentFace = DoubleFacedComponent.Face.FRONT
            )
        )
    }
}
