package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.state.components.identity.copiableContainer
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.engine.state.components.identity.copiableDoubleFacedComponent
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.LkiPolicy
import com.wingedsheep.engine.handlers.effects.lkiPolicyFor
import com.wingedsheep.engine.handlers.effects.lkiSnapshotFor
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.stack.snapshotFor
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfTargetEffect.
 *
 * Creates N token copies of a targeted permanent (resolved via EffectTarget).
 * Used for "Create X tokens that are copies of target token you control."
 *
 * **Aura copies (CR 303.4f).** A token copy of an Aura is put onto the battlefield without being
 * cast, so it doesn't target — its controller instead *chooses* what it enchants as it enters,
 * bound by the copied Aura's own enchant restriction (CR 303.4f; targeting restrictions such as
 * hexproof and shroud are ignored). The choice is raised *before* the token exists, so the token
 * enters already attached and its enters-the-battlefield triggers see the attachment. If there is
 * no legal object to enchant, the token isn't created at all (CR 303.4g) — Yenna, Redtooth Regent
 * copying an Aura whose only legal hosts have left the battlefield.
 *
 * **Prescribed host.** When the effect names the host ([CreateTokenCopyOfTargetEffect.attachedTo])
 * there is no choice: each token enters attached to that object if it legally can. An Aura copy that
 * can't is not created (CR 303.4i); an Equipment copy that can't is created unattached (CR 301.5e);
 * anything else simply enters unattached (CR 303.4h).
 */
class CreateTokenCopyOfTargetExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry? = null,
    private val targetFinder: TargetFinder
) : EffectExecutor<CreateTokenCopyOfTargetEffect> {

    override val effectType: KClass<CreateTokenCopyOfTargetEffect> = CreateTokenCopyOfTargetEffect::class

    /** Builds the substitute tokens of a `ReplaceTokenCreationWithToken` (Draconic Visitor). */
    private val substituteExecutor by lazy {
        CreateTokenExecutor(amountEvaluator, staticAbilityHandler, cardRegistry)
    }

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetContainer = copySource(state, effect, context)
            ?: return EffectResult.success(state)

        val targetCard = targetContainer.copiableCardComponent()
            ?: return EffectResult.success(state)

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return EffectResult.success(state)

        // Who creates (controls + owns) the token. Defaults to the effect's controller; a player
        // target (e.g. "Target player creates a token …" — Echocasting Symposium) puts the token
        // under that player's control instead.
        val controllerId = effect.controller
            ?.let { context.resolvePlayerTargets(it, state).firstOrNull() }
            ?: context.controllerId

        // Check for token creation replacement effects (e.g., Mirrormind Crown).
        // Mirrormind's replacement copies the equipped creature instead of this
        // effect's intended copy, dropping any added keywords / triggered abilities.
        val replacementResult = TokenCreationReplacementHelper.checkReplacement(
            state, effect, context, count, controllerId, cardRegistry, staticAbilityHandler,
            predicateEvaluator = amountEvaluator.predicates
        )
        if (replacementResult != null) return replacementResult

        // "If one or more artifact tokens would be created under your control, that many … are
        // created instead" (Draconic Visitor): a token copy of an artifact is an artifact token.
        // The copy's characteristics are the copiable values plus this effect's exceptions.
        val prospective = CopyExceptionApplier.apply(targetCard, resolvedExceptions(state, effect, context))
            .copy(ownerId = controllerId, isDoubleFaced = false)
        TokenCreationReplacementHelper.findTokenSubstitution(
            state, controllerId, prospective,
            predicateEvaluator = amountEvaluator.predicates,
            choices = context.pipeline.chosenValues,
        )?.let { lookup ->
            val tokenCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "target-copy tokens")
            return when (lookup) {
                is TokenSubstitution -> {
                    // The end-step riders go on through this executor's own path, which also
                    // carries "your next end step" and "unless it's your Ring-bearer".
                    val created = substituteExecutor.createSubstituteTokens(
                        state, lookup, context, tokenCount, controllerId,
                        riders = TokenRiders(
                            tapped = effect.tapped, attacking = effect.attacking,
                            attackDefender = effect.attackDefender, attackedPlayer = effect.attackedPlayer,
                        ),
                        rootEffect = effect
                    )
                    if (created.outcome !is com.wingedsheep.engine.core.Outcome.Done) created
                    else created.copy(state = installEndStepRiders(
                        created.state, state, effect, context, controllerId,
                        created.updatedCollections[com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS].orEmpty()
                    ))
                }
                is TokenSubstitutionChoice ->
                    TokenCreationReplacementHelper.askForSubstitution(state, lookup, effect, context, tokenCount)
            }
        }

        // A prescribed host ("… attached to that creature") replaces the Aura host choice; the
        // per-token legality check happens in createTokens, once the token's characteristics exist.
        effect.attachedTo?.let { host ->
            return createTokens(
                state, effect, context, controllerId, count, auraHostId = null,
                prescribedHostId = context.resolveTarget(host, state),
            )
        }

        // An Aura token needs its host chosen before it can be created (CR 303.4f) — the copy's
        // type line decides, so read it off the copied CardComponent (copiable values only).
        if (auraTypeLineOf(effect, targetCard).isAura) {
            return AuraTokenHostChooser.pause(
                state = state,
                effect = effect,
                context = context,
                auraDefinitionId = targetCard.cardDefinitionId,
                auraName = targetCard.name,
                controllerId = controllerId,
                remaining = com.wingedsheep.engine.core.GameLimits
                    .cappedTokenCount(count, "target-copy tokens"),
                cardRegistry = cardRegistry,
                targetFinder = targetFinder
            )
        }

        return createTokens(state, effect, context, controllerId, count, auraHostId = null)
    }

    /**
     * Create [count] token copies of the effect's target. When [auraHostId] is non-null every
     * created token enters attached to it (the Aura path — see the class docs); otherwise the
     * tokens enter unattached. Split out of [execute] so the Aura host-choice continuation can
     * re-enter here once the controller has picked a host.
     *
     * When the effect prescribes a host ([CreateTokenCopyOfTargetEffect.attachedTo]),
     * [prescribedHostId] is that host as resolved (null if it no longer exists); each token is
     * attached to it only if it could legally be, and an Aura token that couldn't is not created.
     */
    /**
     * The object whose copiable values the tokens copy. Normally the live object the effect's
     * target names. When that is the **triggering** object and it has left the battlefield since
     * the ability triggered ("Whenever a creature … enters, create a token that's a copy of that
     * creature" with the creature killed in response — Molten Echoes, Necroduality), the token
     * copies it as it last existed on the battlefield (CR 608.2h, CR 707.2): the copiable
     * [CardComponent] frozen into its [LastKnownPermanentComponent] snapshot at departure, so a
     * creature that was itself a copy is copied as what it was copying. The snapshot is stripped
     * on the card's next zone change, so it only ever describes the departed battlefield object.
     * A token that left has ceased to exist (CR 704.5d) and leaves nothing to copy.
     */
    private fun copySource(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
    ): ComponentContainer? {
        // A value read (CR 608.2h), not an action: a source that died is a new object or gone, but
        // "a copy of it" still names it.
        val capturedId = if (effect.target == EffectTarget.IterationEntity) context.iterationEntityId
        else (effect.target as? EffectTarget.SingleEntity)
            ?.let { com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveEntity(it, context, state) }
        capturedId?.let { departedCapturedEntity(state, context, it) }?.let { return it }
        departedSnapshotReference(state, effect, context)?.let { return it }
        context.resolveTarget(effect.target, state)?.let { id ->
            val entity = state.getEntity(id) ?: return null
            return departedPermanentTarget(state, effect, context, id, entity) ?: entity
        }
        if (effect.target != EffectTarget.TriggeringEntity) return null
        // The departure snapshot belongs to the captured object, even after a blink or a
        // second zone change has erased the card's temporary last-known component.
        context.triggerContext?.triggeringLastKnownSnapshot?.copiableContainer()?.let { return it }
        val departed = context.triggeringEntityId?.let { state.getEntity(it) } ?: return null
        return departed.get<LastKnownPermanentComponent>()?.snapshot?.copiableContainer()
    }

    /**
     * A permanent the trigger captured ("a spell that targets only a single artifact or creature
     * you control, … a copy of that artifact or creature" — Vesuvan Duplimancy), or that a delayed
     * trigger captured as it was scheduled ("when this dies, … create a token that's a copy of it at
     * the beginning of the next end step" — Ochre Jelly), that is no longer the same object on the
     * battlefield when the payoff resolves. It is copied as it last
     * existed there (CR 608.2h): its departure snapshot while that still describes the captured
     * visit, else the copiable values frozen when the ability triggered — all a token that has
     * ceased to exist (CR 704.5d) leaves behind. Null while the captured permanent is still there.
     */
    private fun departedCapturedEntity(state: GameState, context: EffectContext, id: EntityId): ComponentContainer? {
        val captured = context.triggerContext?.capturedEntitySnapshots?.snapshotFor(id) ?: return null
        val visit = captured.battlefieldEntryTimestamp
        val entity = state.getEntity(id)
        val sameVisit = entity?.get<BattlefieldEntryTimestampComponent>()?.timestamp == visit
        if (id in state.getBattlefield() && sameVisit) return null
        entity?.get<LastKnownPermanentComponent>()?.snapshot
            ?.takeIf { it.battlefieldEntryTimestamp == visit }
            ?.copiableContainer()
            ?.let { return it }
        return captured.copiableContainer()
    }

    /**
     * A permanent named by a last-known-information reference that carries its own frozen
     * snapshot — "sacrifice an Aura … When you sacrifice an Aura this way, … create a token that's
     * a copy of that Aura" (Three Dog, Galaxy News DJ) reads [EffectTarget.SacrificedAsCost] — and
     * that is no longer that object on the battlefield. It is copied from the copiable values the
     * snapshot froze as it left (CR 608.2h, CR 707.2): a sacrificed token has ceased to exist
     * (CR 704.5d), and a card in the graveyard is a new object (CR 400.7) whose own values — not
     * what it was copying — are not the ones "that Aura" names. Null while it is still the same
     * battlefield object (read live) or when the snapshot froze no copiable values.
     */
    private fun departedSnapshotReference(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
    ): ComponentContainer? {
        val reference = effect.target as? EffectTarget.SingleEntity ?: return null
        // The source and the triggering object keep their own departure handling (copySource).
        if (reference == EffectTarget.Self || reference == EffectTarget.TriggeringEntity) return null
        if (lkiPolicyFor(reference) != LkiPolicy.LIVE_THEN_LKI) return null
        val id = context.resolveTarget(reference, state) ?: return null
        val snapshot = context.lkiSnapshotFor(reference, id)?.takeIf { it.copiableCard != null } ?: return null
        val sameVisit = id in state.getBattlefield() && (snapshot.battlefieldEntryTimestamp == null ||
            state.getEntity(id)?.get<BattlefieldEntryTimestampComponent>()?.timestamp == snapshot.battlefieldEntryTimestamp)
        return if (sameVisit) null else snapshot.copiableContainer()
    }

    /**
     * A **permanent** chosen as a target that has since left the battlefield — "Destroy target
     * creature. If that creature dies this way, … create two tokens that are copies of that
     * creature" (Saw in Half). "That creature" is the object that was on the battlefield, so the
     * copy uses its last-known copiable values (CR 608.2h): a dead Clone is copied as what it was
     * copying, not as the Clone card now in the graveyard. Only a target chosen *as a permanent*
     * qualifies — a creature **card** targeted in a graveyard is copied as the card it is, even if
     * an old departure snapshot is still riding on it.
     */
    private fun departedPermanentTarget(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        id: EntityId,
        entity: ComponentContainer,
    ): ComponentContainer? {
        val chosen = when (val target = effect.target) {
            is EffectTarget.ContextTarget -> context.positionalTarget(target.index)
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[target.name]
            else -> return null
        }
        if (chosen !is ChosenTarget.Permanent) return null
        if (id in state.getBattlefield()) return null
        return entity.get<LastKnownPermanentComponent>()?.snapshot?.copiableContainer()
    }

    /**
     * The effect's copy exceptions with a game-dependent base P/T
     * ([CreateTokenCopyOfTargetEffect.basePower] / [CreateTokenCopyOfTargetEffect.baseToughness])
     * evaluated into fixed values. Re-evaluated on a resumed batch, which reads the same snapshotted
     * pipeline number, so every token of one effect gets the same P/T.
     */
    private fun resolvedExceptions(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
    ): CopyExceptions {
        val power = effect.basePower ?: return effect.copyExceptions
        val toughness = effect.baseToughness ?: return effect.copyExceptions
        return CopyExceptions(
            powerOverride = amountEvaluator.evaluate(state, power, context),
            toughnessOverride = amountEvaluator.evaluate(state, toughness, context),
        ).over(effect.copyExceptions)
    }

    internal fun createTokens(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        controllerId: EntityId,
        count: Int,
        auraHostId: EntityId?,
        prescribedHostId: EntityId? = null,
    ): EffectResult {
        val targetContainer = copySource(state, effect, context)
            ?: return EffectResult.success(state)
        val targetCard = targetContainer.copiableCardComponent()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val createdTokens = mutableListOf<EntityId>()

        // Every "except …" clause (CR 707.9) — added/removed types, added keywords, base P/T,
        // colors, no-mana-cost — is applied by the shared CopyExceptionApplier, so the token
        // path and the "permanent becomes a copy" path can't drift. Both the exceptions view and
        // the resulting component are loop-invariant, so they are built once rather than per token.
        val exceptions = resolvedExceptions(state, effect, context)
        // "The token enters with half that many +1/+1 counters" — evaluated once per batch, so every
        // copy gets the same count.
        val initialCounters = effect.initialCounters.mapValues { (_, amount) -> amountEvaluator.evaluate(state, amount, context) }
        // `isDoubleFaced` is cleared rather than inherited: it answers "is this a double-faced
        // *card*", and a token is not a card (CR 111.1) — nor is being double-faced a copiable value
        // (CR 707.2 lists them; layout isn't one). A token copy of a double-faced permanent is a
        // double-faced *token* and can still transform (CR 707.8a / 712.9), which is what the
        // DoubleFacedComponent copied below is for.
        fun withAddedStatics(card: CardComponent): CardComponent = card.copy(
            copyStaticAbilities = card.copyStaticAbilities + effect.addedStaticAbilities,
            flipSide = card.flipSide?.let(::withAddedStatics),
        )
        val tokenCard = withAddedStatics(CopyExceptionApplier.apply(targetCard, exceptions, context.resolvingTriggeredAbility))
            .copy(ownerId = controllerId, isDoubleFaced = false)

        val cappedCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "target-copy tokens")
        // CR 508.4: "its controller chooses ... as it enters" — every copy's defender is settled
        // before the first one is created, one question per token. The answers re-run the whole
        // effect from the top (EnteringAttackDefenderContinuation), so an Aura copy — whose host
        // continuation re-enters here directly — keeps the default.
        // With a named player the choice narrows to "that player or a planeswalker they control".
        val namedDefendingPlayer = effect.attackedPlayer?.let {
            com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                .resolvePlayerTarget(it, context, state)
                ?.takeIf { player -> player in state.getOpponents(controllerId) }
        }
        val chosenDefenders = if (effect.attacking && tokenCard.typeLine.isCreature && auraHostId == null &&
            effect.attackDefender == com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender.CONTROLLER_CHOOSES
        ) {
            if (effect.attackedPlayer != null && namedDefendingPlayer == null) List(cappedCount) { null }
            else EnteringAttackDefenderChooser.defenders(state, context, controllerId, cappedCount, namedDefendingPlayer)
                ?: return EnteringAttackDefenderChooser.pause(
                    state, effect, context, controllerId, cappedCount, tokenCard.name, namedDefendingPlayer
                )
        } else null
        for (index in 0 until cappedCount) {
            val stateBeforeToken = newState
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            // Provenance: record the creating permanent so `createdBySource()` can recognize this
            // token later — the same stamp CreateTokenExecutor applies (Tetravus, Dance of Many).
            if (effect.stampCreator) {
                context.sourceId?.let { creatorId ->
                    components.add(
                        com.wingedsheep.engine.state.components.identity.CreatedByComponent(creatorId)
                    )
                }
            }
            // Only creatures can be attacking. A copy of a card whose printed type line isn't a
            // creature (e.g. an animated permanent exiled and reverted to its printed type) still
            // enters tapped but never attacking — see Mardu Siegebreaker's rulings.
            if (effect.attacking && tokenCard.typeLine.isCreature) {
                // A specified defender ("attacking that player" — myriad) is honoured only while
                // it is still in the game and one of the creator's opponents; otherwise the copy
                // enters but never attacking (CR 508.4a). Unspecified, the token joins the
                // source's attack (CR 802.2a) — see CreateTokenExecutor.
                val defenderId = if (chosenDefenders != null) {
                    chosenDefenders[index]
                } else if (effect.attackedPlayer != null) {
                    com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                        .resolvePlayerTarget(effect.attackedPlayer!!, context, newState)
                        ?.takeIf { it in newState.getOpponents(controllerId) }
                } else {
                    com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                        .defenderForEnteringAttacker(context, newState, controllerId)
                }
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId, defendingPlayerId = com.wingedsheep.engine.mechanics.combat.CombatDefenders.defendingPlayerOf(newState, defenderId)))
                }
            }
            // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
            // with the same face up as the source. Counters
            // are intentionally not copied (handled by the absence of CountersComponent copy
            // throughout this executor).
            targetContainer.copiableDoubleFacedComponent {
                withAddedStatics(CopyExceptionApplier.apply(it, exceptions, context.resolvingTriggeredAbility))
                    .copy(ownerId = controllerId, isDoubleFaced = false)
            }?.let { components.add(it) }

            var container = ComponentContainer.of(*components.toTypedArray())
            // Toxic N / bushido N ride components, not the CardComponent — carry them over too.
            container = CopyExceptionApplier.withNumericKeywords(container, targetContainer, exceptions, cardRegistry)

            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)
            // A token copy honors global "[filter] enter tapped" replacements (Authority of the
            // Consuls / Dauntless Dismantler on an opponent's token copy).
            newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .applyCreatedTokenEntryTap(
                    newState, tokenId, controllerId, definedTapped = effect.tapped,
                    predicateEvaluator = amountEvaluator.predicates
                )
            // The host the token enters attached to: the one its controller chose for an Aura copy
            // (CR 303.4f), or the effect's prescribed host when the token could legally be attached
            // to it. Legality is judged on the token's own characteristics, now that it exists.
            val hostId = if (effect.attachedTo == null) auraHostId else prescribedHostId?.takeIf { host ->
                cardRegistry != null && com.wingedsheep.engine.handlers.effects.permanent.attachments
                    .AttachmentMover.canAttach(newState, amountEvaluator.predicates, cardRegistry, tokenId, host)
            }
            // CR 303.4i: an Aura that can't legally enchant the prescribed host (or whose host is
            // gone) isn't created. An Equipment or anything else enters unattached (CR 301.5e,
            // CR 303.4h).
            if (effect.attachedTo != null && hostId == null &&
                newState.projectedState.hasSubtype(tokenId, "Aura")
            ) {
                newState = stateBeforeToken
                continue
            }

            // The token enters already attached, so the attachment is in place for any
            // enters-the-battlefield trigger and for the very first state-based check. Wire the
            // host side of the attachment and announce it, so "becomes attached" triggers (Eriette,
            // the Beguiler) fire for an Aura token the same way they do when an Aura card is put
            // onto the battlefield attached (CR 603.2f).
            if (hostId != null) {
                newState = newState.updateEntity(tokenId) { it.with(AttachedToComponent(hostId)) }
                newState = newState.updateEntity(hostId) { hostContainer ->
                    val existing = hostContainer.get<AttachmentsComponent>()
                    hostContainer.with(
                        AttachmentsComponent((existing?.attachedIds ?: emptyList()) + tokenId)
                    )
                }
                events.add(
                    com.wingedsheep.engine.core.PermanentAttachedEvent(
                        attachmentId = tokenId,
                        attachmentName = tokenCard.name,
                        attachedToId = hostId,
                        controllerId = controllerId,
                    )
                )
            }

            // As-enters "enters with counters" (CR 614.1c): the copied card's own
            // EntersWithCounters (a copy of a creature that "enters with a +1/+1 counter"), plus
            // global grants from other permanents (Gev, Scaled Scorch). BattlefieldEntry.place skips
            // this setup, so apply it here the way the standard entry pipeline does. Non-pausing.
            val (afterCounters, counterEvents) = if (cardRegistry != null) {
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .applyOnEntry(newState, tokenId, controllerId, cardRegistry, predicateEvaluator = amountEvaluator.predicates)
            } else {
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .applyGlobal(newState, tokenId, controllerId, cardRegistry, predicateEvaluator = amountEvaluator.predicates)
            }
            newState = afterCounters
            events.addAll(counterEvents)
            newState = TokenEntryReplacements.applyInitialCounters(
                newState, tokenId, preCreationState = state, controllerId = controllerId,
                initialCounters = initialCounters, predicateEvaluator = amountEvaluator.predicates
            )

            for (ability in effect.triggeredAbilities) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }

            // Activated abilities granted to the copy (e.g. Shelob's Food sacrifice ability). Tokens
            // have no CardDefinition, so granted abilities live in GameState.grantedActivatedAbilities,
            // which the legal-action enumerator and ActivateAbilityHandler consult for any entity.
            for (ability in effect.activatedAbilities) {
                val grant = GrantedActivatedAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedActivatedAbilities = newState.grantedActivatedAbilities + grant
                )
            }

            // Static abilities granted to the copy — the "except it has \"[static ability]\"" copy
            // clause (Firion, Wild Rose Warrior: "except it has \"This Equipment's equip abilities
            // cost {2} less to activate\""). Like triggered/activated grants, these live in
            // GameState.grantedStaticAbilities since tokens have no CardDefinition; each static
            // reader (e.g. the equip-cost reducer) unions granted statics with printed ones.
            for (ability in effect.addedStaticAbilities) {
                val grant = GrantedStaticAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedStaticAbilities = newState.grantedStaticAbilities + grant
                )
            }

            // As-enters "choose X as this enters" (CR 614.12) + granted riot (CR 702.136/702.136b).
            // A token copy of a creature that "enters with your choice of …" — or a Spider entering
            // while Spider-Punk grants it riot — pauses for a player decision. We deliberately do NOT
            // emit this token's entry ZoneChangeEvent when we pause: the choice resumer synthesizes it
            // after the choice resolves, so ETB triggers fire exactly once (mirroring
            // TokenFromDefinition). Counters already added ride along as carryEvents.
            val choicePlan = if (cardRegistry != null) {
                TokenEntryReplacements.firstEntersWithChoice(newState, tokenId, cardRegistry, predicateEvaluator = amountEvaluator.predicates)
            } else null
            if (choicePlan != null) {
                val remaining = cappedCount - (index + 1)
                var pausedState = newState
                if (remaining > 0) {
                    // The rest of the batch resumes below the choice's continuation once this token's
                    // choice (and every granted-riot instance) has fully resolved.
                    pausedState = newState.pushContinuation(
                        com.wingedsheep.engine.core.CreateTokenCopyRemainingContinuation(
                            effect = effect,
                            context = context,
                            controllerId = controllerId,
                            remaining = remaining,
                        )
                    )
                }
                val paused = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .pauseForEntersWithChoice(
                        state = pausedState,
                        entityId = tokenId,
                        controllerId = controllerId,
                        cardComponent = tokenCard,
                        choice = choicePlan.choice,
                        fromZone = null,
                        carryEvents = events,
                        syntheticRiot = choicePlan.syntheticRiot,
                        syntheticRiotRemaining = choicePlan.syntheticRiotRemaining,
                    )
                if (paused != null) return EffectResult.from(paused)
                // A null pause means the choice couldn't actually be presented (e.g. no legal object) —
                // fall through and complete this token's entry normally.
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId,
                    oldObject = null,
                    newObject = newState.objectRef(tokenId)
                )
            )

            // CR 714.2b/714.3a: a token copy of a Saga enters as a Saga and gets its on-enter lore
            // counter (chapter I then triggers). BattlefieldEntry.place is the ad-hoc insertion path
            // and intentionally skips enters-with-counters setup, so apply the shared Saga-entry
            // helper here — the same one the standard moveToZone pipeline uses. No-op for non-Sagas.
            val (sagaState, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, tokenId, amountEvaluator.predicates)
            newState = sagaState
            events.addAll(sagaEvents)

            // CR 306.5b: likewise a token copy of a planeswalker enters with the copied printed
            // loyalty (a copiable value, CR 707.2). Without it state-based actions (CR 704.5i)
            // bin the token the instant it enters. No-op for non-planeswalkers.
            cardRegistry?.let { registry ->
                val (loyaltyState, loyaltyEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .applyIntrinsicEntryCountersIfNeeded(newState, tokenId, controllerId, registry, predicateEvaluator = amountEvaluator.predicates)
                newState = loyaltyState
                events.addAll(loyaltyEvents)
            }

            createdTokens.add(tokenId)
        }

        newState = installEndStepRiders(newState, state, effect, context, controllerId, createdTokens)

        // Publish the created token ids into the shared CREATED_TOKENS pipeline collection so a
        // following composite step can reference them — e.g. "Create a token that's a copy of
        // target permanent ... Put six +1/+1 counters on it" composes this with
        // AddCountersToCollection(CREATED_TOKENS, ...). Mirrors CreateTokenExecutor /
        // CreateTokenCopyOfSourceExecutor, which expose their tokens the same way.
        return EffectResult(
            state = newState,
            events = events,
            updatedCollections = mapOf(com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS to createdTokens.toList())
        )
    }

    /**
     * The copy's "sacrifice / exile it at the beginning of the next end step" riders: a delayed
     * trigger per token in [createdTokens]. Also installed on the substitutes when a
     * `ReplaceTokenCreationWithToken` replaced the copies — the riders still apply to them.
     * [sourceState] is the state the effect started resolving in (for the source's name).
     */
    private fun installEndStepRiders(
        state: GameState,
        sourceState: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext,
        controllerId: EntityId,
        createdTokens: List<EntityId>
    ): GameState {
        var newState = state
        // If sacrificeAtStep is set, create a delayed trigger to sacrifice each created token
        // copy at that step (e.g. Mardu Siegebreaker: "at the beginning of your next end step,
        // sacrifice those tokens"). Mirrors CreateTokenExecutor's sacrificeAtStep handling.
        val sacrificeStep = effect.sacrificeAtStep
        if (sacrificeStep != null && createdTokens.isNotEmpty()) {
            val sourceId = context.sourceId ?: controllerId
            val sourceName = sourceState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
            for (tokenId in createdTokens) {
                val (delayedTriggerId, stateWithRoutingId) = newState.newRoutingId()
                newState = stateWithRoutingId
                val delayedTrigger = DelayedTriggeredAbility(
                    id = delayedTriggerId,
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(tokenId)),
                    fireAtStep = sacrificeStep,
                    sourceId = sourceId,
                    objectReferences = context.objectReferences,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    // "sacrifice at the beginning of your next end step" → gate the firing step
                    // to the token controller's turn (the single fireOnPlayerId gate).
                    fireOnPlayerId = if (effect.sacrificeOnlyOnControllersTurn) controllerId else null
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        // If exileAtStep is set, create a delayed trigger to exile each created token copy at that
        // step (Sauron, the Necromancer: "at the beginning of the next end step, exile that token
        // unless Sauron is your Ring-bearer"). The firing step is the next matching step of any
        // player's turn ("the next end step", so no fireOnPlayerId gate). When
        // exileUnlessSourceIsRingBearer is set the exile is wrapped in a condition that skips it
        // while the source (resolved to the delayed trigger's sourceId) is the controller's
        // Ring-bearer (CR 701.54e) — the condition is re-evaluated at fire time.
        val exileStep = effect.exileAtStep
        if (exileStep != null && createdTokens.isNotEmpty()) {
            val sourceId = context.sourceId ?: controllerId
            val sourceName = sourceState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
            for (tokenId in createdTokens) {
                val exileEffect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE)
                val delayedEffect = if (effect.exileUnlessSourceIsRingBearer) {
                    GatedEffect(
                        gate = Gate.WhenCondition(SourceIsRingBearer),
                        then = CompositeEffect(emptyList()),
                        otherwise = exileEffect
                    )
                } else {
                    exileEffect
                }
                val (delayedTriggerId, stateWithRoutingId) = newState.newRoutingId()
                newState = stateWithRoutingId
                val delayedTrigger = DelayedTriggeredAbility(
                    id = delayedTriggerId,
                    effect = delayedEffect,
                    fireAtStep = exileStep,
                    sourceId = sourceId,
                    objectReferences = context.objectReferences,
                    sourceName = sourceName,
                    controllerId = controllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        return newState
    }

    /**
     * The type line the copy will actually have, after every "except …" type clause. Read to
     * decide whether the Aura host choice applies — "except it's a creature" turns an Aura copy
     * into something that isn't an Aura and needs no host. Shares [CopyExceptionApplier.typeLine]
     * with the token that is actually built, so the two can't disagree about what the copy is.
     */
    private fun auraTypeLineOf(
        effect: CreateTokenCopyOfTargetEffect,
        targetCard: CardComponent,
    ) = CopyExceptionApplier.typeLine(targetCard.typeLine, effect.copyExceptions)
}
