package com.wingedsheep.engine.support

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.core.HandRevealedEvent
import com.wingedsheep.engine.core.LookedAtCardsEvent
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PlayCardDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.hidden.HiddenSlotRewrite
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ReplacementEffectIdentity
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.DecisionEnricher
import com.wingedsheep.engine.view.DecisionMasker
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.view.OpponentDecisionStatusView
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.full.memberProperties

/**
 * Forks a game state into a second world in which a hidden card is a different card, replays the
 * same actions in both, and reports the first thing that tells the worlds apart for a player who
 * may not see the card — the check behind issues #2780 and #3340.
 *
 * Two properties are checked, for the players for whom the two worlds start indistinguishable
 * ([Fork.seats]):
 *
 * - **Control flow** (#3340): whether a player is asked a question may depend only on information
 *   every player has. The pending question's [QuestionHeader] must be the same in both worlds for
 *   every seat until that seat learns a forked card *in the real game* — through [Visibility], a
 *   question it answers displaying the card, or a reveal or look event in the real world. A
 *   disclosure in the swapped world only tells the real seat nothing, and a divergence, however
 *   authorised, retires no seat. A skip that reads only public facts, such as an empty hand, is
 *   the same in both worlds and needs no special case.
 * - **Data** (#2780): nothing a player is sent may depend on the identity of a card that player may
 *   not see. A seat's whole [SeatView] must be the same in both worlds until it sees a forked card
 *   in either world. After an actor who could see the card has made the worlds go differently, a
 *   view difference may be that choice's legitimate consequence: the seat's view comparison stops
 *   ([Outcome.dataStopped]) and its headers are still checked. A divergence explained only by a
 *   question displaying the card to its decider excuses nothing another seat sees. A divergence
 *   nothing explains — no actor or decider saw the card, nobody was shown it — is a leak: a choice
 *   the engine made for that player depended on the card.
 *
 * Each action is checked in this order. The replay ends only where [Outcome.end] says:
 *
 * 1. **Refused** in one world or both ([End.Refused]). The states before it were already
 *    compared, so a refusal itself is not reported as a leak.
 * 2. **Public to every seat** in both worlds — moved to a public zone, turned face up, revealed to
 *    all ([End.PublicToAll]): what follows may depend on a card everyone has seen.
 * 3. **Headers**, then seats that learned the card in the real game retire ([End.AllInformed] when
 *    none is left). Comparing first means a disclosure the same action made can't excuse it —
 *    group A's auto-pick mills the card it didn't ask about.
 * 4. **Views**, for seats still compared, as above.
 * 5. **Unexplained divergence**, as above.
 *
 * The script's end is [End.Completed], and a difference found is [End.Leaked].
 *
 * Whether a question displays a card is read off what its decider is sent:
 * [DecisionMasker.maskFor] for that player, and the fields of the masked decision that carry a
 * card's identity — the [SearchCardInfo] maps, the source name, the combat board and mana-source
 * names. An id on its own displays nothing, because the client resolves ids through its
 * already-masked state. When a decision names a card by id *and* its free text contains the
 * card's name, the check can't tell whether the text is about that card and stops with
 * [End.Undetermined], naming the decision type, rather than guess.
 *
 * **What the server sends, and what this compares.** [SeatView] holds the client state, the
 * events, the decision a seat answers, and the opponent-decision status otherwise — both read
 * through [DecisionEnricher], the projection the game server sends. The
 * legal actions are compared only for the seat holding priority while no question but a
 * [PlayCardDecision] is pending; the server also serves a mana-payment window's abilities to a
 * seat without priority, and resolves the acting seat through teams and hijacks — neither is
 * compared.
 *
 * The engine mints entity ids from a counter in [GameState], so both worlds allocate the same ids
 * and the comparison needs no normalisation.
 */
class HiddenIdentityForkChecker(private val registry: CardRegistry) {
    private val processor = ActionProcessor(registry)
    private val enumerator = LegalActionEnumerator.create(registry)
    private val predicateEvaluator = PredicateEvaluator(cardRegistry = registry)
    val visibility = Visibility(registry, conditionEvaluator = predicateEvaluator.conditions)
    private val masker = DecisionMasker(visibility)
    private val stateTransformer = ClientStateTransformer(registry, predicateEvaluator = predicateEvaluator)
    private val enricher = LegalActionEnricher(ManaSolver(registry, predicateEvaluator), registry)
    private val decisionEnricher = DecisionEnricher(registry)

    /**
     * How the replay ended: [checked] actions went through the header check, [leak] is the first
     * difference if there was one, and [end] says why the replay stopped. [dataStopped] says why the
     * comparison of a seat's whole view stopped before the end, if it did; the headers were still
     * checked through [checked].
     */
    data class Outcome(val checked: Int, val leak: String?, val end: End, val dataStopped: String? = null)

    /** Why a replay stopped. Only [Completed] means every scripted action was checked. */
    sealed interface End {
        /** Every scripted action was checked. */
        data object Completed : End

        /** An action was refused in one world or both. A refusal is not itself reported as a leak. */
        data class Refused(val action: Int) : End

        /** A forked card became visible to every seat in both worlds. */
        data class PublicToAll(val action: Int) : End

        /** Every seat of the fork has learned a forked card in the real game. */
        data class AllInformed(val action: Int) : End

        /** A difference was found; [Outcome.leak] has it. */
        data class Leaked(val action: Int) : End

        /** Whether a question displays a card couldn't be read off the masked decision. */
        data class Undetermined(val action: Int) : End
    }

    /** What one seat is sent after one action; the two worlds must agree on every field. */
    data class SeatView(
        val state: ClientGameState,
        val events: List<ClientEvent>,
        val legalActions: List<LegalActionInfo>,
        val decision: PendingDecision?,
        val opponentDecision: OpponentDecisionStatusView?,
    ) {
        /** The first field on which [other] differs, with the text around where they part; null when equal. */
        fun diff(other: SeatView): String? {
            val fields = listOf(
                "state" to (state to other.state),
                "events" to (events to other.events),
                "legalActions" to (legalActions to other.legalActions),
                "decision" to (decision to other.decision),
                "opponentDecision" to (opponentDecision to other.opponentDecision),
            )
            val (field, values) = fields.firstOrNull { (_, v) -> v.first != v.second } ?: return null
            // Name the card when the difference is one card's view, which is the usual case, or
            // else the client state's property that differs.
            val card = (state.cards.keys + other.state.cards.keys)
                .firstOrNull { field == "state" && state.cards[it] != other.state.cards[it] }
            val property = if (field == "state" && card == null) {
                ClientGameState::class.memberProperties.firstOrNull { it.get(state) != it.get(other.state) }
            } else {
                null
            }
            val name = when {
                card != null -> "state.cards[$card]"
                property != null -> "state.${property.name}"
                else -> field
            }
            val a = (if (card != null) state.cards[card] else if (property != null) property.get(state) else values.first).toString()
            val b = (if (card != null) other.state.cards[card] else if (property != null) property.get(other.state) else values.second).toString()
            val at = a.indices.firstOrNull { it >= b.length || a[it] != b[it] } ?: a.length
            val from = maxOf(0, at - CONTEXT)
            return "$name differs\n  real:    …${a.substring(from, minOf(a.length, at + CONTEXT))}…" +
                "\n  swapped: …${b.substring(from, minOf(b.length, at + CONTEXT))}…"
        }

        private companion object {
            const val CONTEXT = 160
        }
    }

    /**
     * The part of a pending question every seat learns, as [seat] is sent it: that it exists, the
     * seat it belongs to and the seat acting for that one, what kind it is, and its source's id and
     * masked name. Null stands for "no question pending".
     */
    data class QuestionHeader(
        val playerId: EntityId,
        val decider: EntityId,
        val type: String,
        val sourceId: EntityId?,
        val sourceName: String?,
    )

    /**
     * A forked world: the [cards] are other cards in it, hidden from [viewer]. [seats] are the
     * players for whom the two worlds start indistinguishable, the viewer among them. [swap] maps a
     * state of the real world to what the swapped world should be if nothing has depended on the
     * cards yet.
     */
    class Fork internal constructor(
        val cards: List<EntityId>,
        val viewer: EntityId,
        val seats: Set<EntityId>,
        val faceDown: Boolean,
        internal val swap: (GameState) -> GameState,
    )

    /**
     * A face-down object [card] made into [identity] underneath, hidden from [viewer]. [knownTo]
     * names seats that already saw it in a way [Visibility] keeps no trace of.
     */
    fun faceDownFork(
        state: GameState,
        card: EntityId,
        identity: CardDefinition,
        viewer: EntityId,
        knownTo: Set<EntityId> = emptySet(),
    ): Fork {
        require(state.getEntity(card)?.has<FaceDownComponent>() == true) { "$card is not face down" }
        require(visibility.isCardIdentityHiddenFrom(state, card, viewer)) { "$card is not hidden from $viewer" }
        val seats = state.turnOrder.filter { it !in knownTo && visibility.isCardIdentityHiddenFrom(state, card, it) }
        return Fork(listOf(card), viewer, seats.toSet() + viewer, faceDown = true) { withIdentity(it, card, identity) }
    }

    /**
     * A card in a hand or library made into [identity] through [HiddenSlotRewrite], hidden from
     * [viewer]. A player knows their own decklist — the deck tracker they are sent shows it — so
     * the owner of a library card is never a seat of this fork; [orderFork] forks their library.
     */
    fun zoneFork(
        state: GameState,
        card: EntityId,
        identity: CardDefinition,
        viewer: EntityId,
        knownTo: Set<EntityId> = emptySet(),
    ): Fork {
        val zone = requireNotNull(hiddenZoneOf(state, card)) { "$card is not in a hand or library" }
        fun ownsLibrary(seat: EntityId) =
            zone.zoneType == Zone.LIBRARY && (seat == zone.ownerId || state.actorFor(zone.ownerId) == seat)
        require(!ownsLibrary(viewer)) { "a library card's owner knows their decklist; fork the order with orderFork" }
        require(hiddenFrom(state, card, viewer, faceDownFork = false)) { "$card is not hidden from $viewer" }
        rewriteRefusal(state, card)?.let { throw IllegalArgumentException("$card can't be rewritten: $it") }
        val seats = state.turnOrder.filter {
            it !in knownTo && !ownsLibrary(it) && hiddenFrom(state, card, it, faceDownFork = false)
        }
        return Fork(listOf(card), viewer, seats.toSet() + viewer, faceDown = false, slotSwap(state, listOf(card to identity)))
    }

    /**
     * Two cards in the library of [owner] exchange identities, so its composition — which the
     * owner knows — stays the same and only the order is forked. The owner is the viewer.
     */
    fun orderFork(
        state: GameState,
        card: EntityId,
        partner: EntityId,
        owner: EntityId,
        knownTo: Set<EntityId> = emptySet(),
    ): Fork {
        val library = state.getLibrary(owner)
        require(card in library && partner in library) { "both cards must be in $owner's library" }
        val cardDef = definitionOf(state, card)
        val partnerDef = definitionOf(state, partner)
        require(cardDef.name != partnerDef.name) { "an exchange of two copies of one card forks nothing" }
        for (id in listOf(card, partner)) {
            require(hiddenFrom(state, id, owner, faceDownFork = false)) { "$id is not hidden from $owner" }
            rewriteRefusal(state, id)?.let { throw IllegalArgumentException("$id can't be rewritten: $it") }
        }
        val seats = state.turnOrder.filter { seat ->
            seat !in knownTo && listOf(card, partner).all { hiddenFrom(state, it, seat, faceDownFork = false) }
        }
        return Fork(
            listOf(card, partner), owner, seats.toSet() + owner, faceDown = false,
            slotSwap(state, listOf(card to partnerDef, partner to cardDef)),
        )
    }

    /** [zoneFork] and [replay] in one step, for a scripted state just before a site's question. */
    fun checkZoneFork(
        start: GameState,
        card: EntityId,
        identity: CardDefinition,
        viewer: EntityId,
        actions: List<GameAction>,
    ): Outcome = replay(start, actions, zoneFork(start, card, identity, viewer))

    /** [orderFork] and [replay] in one step. */
    fun checkOrderFork(
        start: GameState,
        card: EntityId,
        partner: EntityId,
        owner: EntityId,
        actions: List<GameAction>,
    ): Outcome = replay(start, actions, orderFork(start, card, partner, owner))

    /** [faceDownFork] and [replay] in one step. */
    fun checkFaceDownFork(
        start: GameState,
        card: EntityId,
        identity: CardDefinition,
        viewer: EntityId,
        actions: List<GameAction>,
    ): Outcome = replay(start, actions, faceDownFork(start, card, identity, viewer))

    /**
     * Why [HiddenSlotRewrite] may not give [id] another identity, or null when it may: pinned by
     * in-flight execution, runtime state on the card, or a reference held outside the card.
     *
     * The outside references checked are the lists the rewrite's KDoc names
     * (`grantedKeywordAbilities`, `mayPlayPermissions`, `lastCardDrawnThisTurnByPlayer`) and these
     * `GameState` fields: every other `granted*` list, `playerActionPermissions`, `delayedTriggers`,
     * `pendingTriggers`, `floatingEffects`, the `pending*Spell*` lists, `spellCostModifiers`,
     * `spellsCastThisTurnByPlayer`, `enlistmentsThisCombat`, `resolutionControls`, `zoneReturns`,
     * `departedLinkedExile`, `sourceObjectRecords`, `pendingSacrificeIds`,
     * `pendingDiscardCauseControllers`, `pendingReplacementRiders` and `activeReplacementChain`.
     * A field not named here is not checked. The list belongs in [HiddenSlotRewrite] itself, which
     * the AI's determinizer would then share; that is an engine change and left for later.
     */
    fun rewriteRefusal(state: GameState, id: EntityId): String? {
        when (val pins = HiddenSlotRewrite.identitySensitiveInFlightPins(state)) {
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Complete ->
                if (id in pins.entityIds) return "pinned by in-flight execution"
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete -> return "in-flight analysis incomplete: ${pins.reason}"
        }
        externalReference(state, id)?.let { return "named by $it" }
        val container = state.getEntity(id) ?: return "no entity"
        val owner = container.get<OwnerComponent>()?.playerId ?: return "no owner"
        val definition = container.get<CardComponent>()?.let { registry.getCard(it.cardDefinitionId) }
            ?: return "definition not registered"
        return HiddenSlotRewrite.runtimeBlockers(container, definition, owner)
            .takeIf { it.isNotEmpty() }
            ?.let { "runtime state $it" }
    }

    private fun externalReference(state: GameState, id: EntityId): String? = when {
        state.grantedTriggeredAbilities.any { it.entityId == id || it.sourceId == id } -> "grantedTriggeredAbilities"
        state.grantedStateTriggeredAbilities.any { it.entityId == id } -> "grantedStateTriggeredAbilities"
        state.grantedActivatedAbilities.any { it.entityId == id || it.sourceId == id } -> "grantedActivatedAbilities"
        state.grantedStaticAbilities.any { it.entityId == id || it.sourceId == id } -> "grantedStaticAbilities"
        state.grantedReplacementEffects.any { it.entityId == id } -> "grantedReplacementEffects"
        state.grantedKeywordAbilities.any { it.entityId == id } -> "grantedKeywordAbilities"
        state.globalGrantedTriggeredAbilities.any { it.sourceId == id } -> "globalGrantedTriggeredAbilities"
        state.mayPlayPermissions.any { id in it.cardIds || it.sourceId == id } -> "mayPlayPermissions"
        state.playerActionPermissions.any { permission ->
            id in permission.references.keys ||
                permission.references.values.any { it?.entityId == id } ||
                permission.context.sourceId == id ||
                permission.context.targets.any { targetId(it) == id }
        } -> "playerActionPermissions"
        id in state.lastCardDrawnThisTurnByPlayer.values -> "lastCardDrawnThisTurnByPlayer"
        state.delayedTriggers.any { it.sourceId == id || it.watchedEntityId == id || it.watchedRecipientId == id } -> "delayedTriggers"
        state.pendingTriggers.any { it.sourceId == id || it.granterId == id } -> "pendingTriggers"
        state.floatingEffects.any { effect -> effect.sourceId == id || effect.referencedObjects.any { it.entityId == id } } -> "floatingEffects"
        state.pendingFreeCastSpells.any { it.sourceId == id } -> "pendingFreeCastSpells"
        state.pendingAnyManaTypeSpells.any { it.sourceId == id } -> "pendingAnyManaTypeSpells"
        state.spellCostModifiers.any { it.sourceId == id } -> "spellCostModifiers"
        state.pendingNextSpellAffinities.any { it.sourceId == id } -> "pendingNextSpellAffinities"
        state.pendingNextSpellKeywords.any { it.sourceId == id } -> "pendingNextSpellKeywords"
        state.pendingSpellCopies.any { it.sourceId == id } -> "pendingSpellCopies"
        state.pendingUncounterableSpells.any { it.sourceId == id } -> "pendingUncounterableSpells"
        state.enlistmentsThisCombat.any { it.entityId == id } -> "enlistmentsThisCombat"
        state.resolutionControls.any { it.resolvingObject.entityId == id } -> "resolutionControls"
        state.zoneReturns.any { it.source.entityId == id || it.movedObject.entityId == id } -> "zoneReturns"
        state.departedLinkedExile.values.any { id in it } -> "departedLinkedExile"
        state.sourceObjectRecords.values.any { record ->
            record.source.entityId == id || record.slots.values.any { refs -> refs.any { it.entityId == id } }
        } -> "sourceObjectRecords"
        state.pendingReplacementRiders.any { it.hostId == id || it.subjectId == id } -> "pendingReplacementRiders"
        state.spellsCastThisTurnByPlayer.values.any { records -> records.any { it.sourceEntityId == id } } -> "spellsCastThisTurnByPlayer"
        id in state.pendingSacrificeIds -> "pendingSacrificeIds"
        id in state.pendingDiscardCauseControllers.keys -> "pendingDiscardCauseControllers"
        state.activeReplacementChain.orEmpty().any { identity ->
            (identity is ReplacementEffectIdentity.BattlefieldIdentity && identity.sourceEntityId == id) ||
                (identity is ReplacementEffectIdentity.CardZoneIdentity && identity.sourceEntityId == id)
        } -> "activeReplacementChain"
        else -> null
    }

    private fun targetId(target: ChosenTarget): EntityId = when (target) {
        is ChosenTarget.Player -> target.playerId
        is ChosenTarget.Permanent -> target.entityId
        is ChosenTarget.Card -> target.cardId
        is ChosenTarget.Spell -> target.spellEntityId
    }

    /** Replays [actions] from [start] in the real world and in [fork]'s, checking each as the class KDoc describes. */
    fun replay(start: GameState, actions: List<GameAction>, fork: Fork): Outcome {
        val progress = Progress()
        return try {
            replayChecked(start, actions, fork, progress)
        } catch (e: UndeterminedDisclosure) {
            Outcome(progress.checked, e.message, End.Undetermined(progress.action))
        }
    }

    private class Progress {
        var checked = 0
        var action = 0
    }

    private fun replayChecked(start: GameState, actions: List<GameAction>, fork: Fork, progress: Progress): Outcome {
        var real = start
        var swapped = fork.swap(real)
        // Seats whose headers are checked: retired only once they learn a card in the real game.
        val headerSeats = fork.seats.toMutableSet()
        // Seats whose whole view is compared: retired once they see a card in either world, or
        // once an informed actor's choice has made their view legitimately different.
        val viewSeats = fork.seats.toMutableSet()
        headerLeak(real, swapped, headerSeats)?.let { return Outcome(0, "at the fork itself: $it", End.Leaked(0)) }
        for (seat in viewSeats.sortedBy { it.value }) {
            view(real, emptyList(), seat).diff(view(swapped, emptyList(), seat))?.let {
                return Outcome(0, "at the fork itself, for $seat: $it", End.Leaked(0))
            }
        }
        var dataStopped: String? = null
        // An informed actor's choice has made the worlds go differently: from then on a view
        // difference may be its legitimate consequence.
        var actorDivergedAt: Int? = null
        // Some disclosure explains the worlds going differently, so a divergence is no longer
        // evidence of an engine choice made on hidden information.
        var explained = false
        for ((index, action) in actions.withIndex()) {
            val step = index + 1
            progress.action = step
            val at = "after action $step ($action)"
            val actorSees = fork.cards.any { sees(real, it, action.playerId, fork.faceDown) }
            val a = processor.process(real, action).result
            val b = processor.process(swapped, action).result
            // The states before it passed the header check, so a refusal itself is no leak.
            if (a.error != null || b.error != null) return Outcome(progress.checked, null, End.Refused(step), dataStopped)
            val realBefore = real
            real = a.state
            swapped = b.state
            if (fork.cards.any { publicToAll(real, a.events, it, fork.faceDown) && publicToAll(swapped, b.events, it, fork.faceDown) }) {
                return Outcome(progress.checked, null, End.PublicToAll(step), dataStopped)
            }
            headerLeak(real, swapped, headerSeats)?.let { return Outcome(progress.checked, "$at: $it", End.Leaked(step), dataStopped) }
            progress.checked = step
            headerSeats.removeAll { seat -> fork.cards.any { sees(real, it, seat, fork.faceDown) || revealedBy(a.events, it, seat) } }
            if (headerSeats.isEmpty()) return Outcome(progress.checked, null, End.AllInformed(step), dataStopped)
            val disclosed = viewSeats.filter { seat ->
                fork.cards.any { sees(real, it, seat, fork.faceDown) || sees(swapped, it, seat, fork.faceDown) || revealedBy(b.events, it, seat) } ||
                    seat !in headerSeats
            }
            viewSeats -= disclosed.toSet()
            if (disclosed.isNotEmpty()) explained = true
            // The game itself went differently, not just its rendering.
            val diverged = fork.swap(real) != swapped
            if (diverged && actorSees && actorDivergedAt == null) actorDivergedAt = step
            for (seat in viewSeats.sortedBy { it.value }) {
                val difference = view(real, a.events, seat).diff(view(swapped, b.events, seat)) ?: continue
                val actorStep = actorDivergedAt
                if (diverged && actorStep != null) {
                    viewSeats -= seat
                    dataStopped = dataStopped ?: "$seat's view differs at action $step, after the informed actor's divergence at action $actorStep"
                    continue
                }
                return Outcome(progress.checked, "$at, for $seat: $difference", End.Leaked(step), dataStopped)
            }
            if (diverged && !explained) {
                val seenByDecider = fork.cards.any { card ->
                    seenByDecider(real, card, fork.faceDown) || seenByDecider(swapped, card, fork.faceDown)
                }
                if (!actorSees && !seenByDecider) {
                    return Outcome(
                        progress.checked,
                        "$at: the game diverged on an action by a player who may not see the card " +
                            "(${realBefore.pendingDecision?.let { it::class.simpleName } ?: "no question"} before it)",
                        End.Leaked(step),
                        dataStopped,
                    )
                }
                explained = true
            }
        }
        return Outcome(progress.checked, null, End.Completed, dataStopped)
    }

    private fun headerLeak(real: GameState, swapped: GameState, seats: Set<EntityId>): String? {
        for (seat in seats.sortedBy { it.value }) {
            val realHeader = header(real, seat)
            val swappedHeader = header(swapped, seat)
            if (realHeader != swappedHeader) {
                return "question header differs, for $seat\n  real:    $realHeader\n  swapped: $swappedHeader"
            }
        }
        return null
    }

    fun header(state: GameState, seat: EntityId): QuestionHeader? = state.pendingDecision?.let {
        QuestionHeader(
            playerId = it.playerId,
            decider = state.actorFor(it.playerId),
            type = it::class.simpleName ?: "",
            sourceId = it.context.sourceId,
            sourceName = masker.maskFor(it, state, seat).context.sourceName,
        )
    }

    /**
     * Whether [card] is hidden from [player] in [state] by [Visibility] alone.
     *
     * A face-down card is hidden while [Visibility] says so. A card forked face down stops being
     * hidden once it is face up or has left the battlefield, which reveals it (CR 708.9). A card in
     * its owner's hand or library — including a library card just drawn, which its owner now sees
     * and the opponent still doesn't — is hidden unless [Visibility] shows it to [player]. In any
     * other zone it is public.
     */
    fun hiddenFrom(state: GameState, card: EntityId, player: EntityId, faceDownFork: Boolean): Boolean {
        val container = state.getEntity(card) ?: return false
        if (container.has<FaceDownComponent>()) return visibility.isCardIdentityHiddenFrom(state, card, player)
        if (faceDownFork) return false
        val zone = hiddenZoneOf(state, card) ?: return false
        return !visibility.isCardIdentityVisibleTo(state, zone, card, player)
    }

    /** [player] sees [card]: [Visibility] shows it, or the question [player] is sent displays it. */
    private fun sees(state: GameState, card: EntityId, player: EntityId, faceDownFork: Boolean): Boolean =
        !hiddenFrom(state, card, player, faceDownFork) || displays(state, card, player)

    /** Every seat may see [card] in [state], or [events] revealed it to all of them. */
    private fun publicToAll(state: GameState, events: List<GameEvent>, card: EntityId, faceDownFork: Boolean): Boolean =
        state.turnOrder.all { !hiddenFrom(state, card, it, faceDownFork) } ||
            events.any { event ->
                when (event) {
                    is CardsRevealedEvent -> card in event.cardIds
                    is HandRevealedEvent -> card in event.cardIds
                    else -> false
                }
            }

    /**
     * Whether [events] showed [card] to [viewer] in passing: a reveal or a look that leaves no trace
     * in the state for [Visibility] to read, such as "reveal target face-down permanent".
     */
    fun revealedBy(events: List<GameEvent>, card: EntityId, viewer: EntityId): Boolean = events.any { event ->
        when (event) {
            is CardsRevealedEvent -> card in event.cardIds && (event.revealToSelf || event.revealingPlayerId != viewer)
            is HandRevealedEvent -> card in event.cardIds
            is HandLookedAtEvent -> card in event.cardIds &&
                (event.viewingPlayerId == viewer || viewer in event.identityViewers)
            is LookedAtCardsEvent -> card in event.cardIds && event.playerId == viewer
            else -> false
        }
    }

    /** The player answering [state]'s question sees [card] — by [Visibility], or because the question displays it. */
    private fun seenByDecider(state: GameState, card: EntityId, faceDownFork: Boolean): Boolean {
        val decision = state.pendingDecision ?: return false
        return sees(state, card, state.actorFor(decision.playerId), faceDownFork)
    }

    /** The decider of [state]'s question and the cards its masked decision displays to them. */
    fun displayedByQuestion(state: GameState): Pair<EntityId, Set<EntityId>>? {
        val decision = state.pendingDecision ?: return null
        val decider = state.actorFor(decision.playerId)
        val masked = masker.maskFor(decision, state, decider)
        val shown = displayedNames(masked).filter { (id, name) -> state.getEntity(id)?.get<CardComponent>()?.name == name }
        return decider to shown.keys
    }

    /**
     * Whether the question [player] answers in [state] displays [card]'s identity to them. Fails
     * with [UndeterminedDisclosure] when the decision names the card by id and its text contains
     * the card's name, since the text may or may not be about that card.
     */
    private fun displays(state: GameState, card: EntityId, player: EntityId): Boolean {
        val decision = state.pendingDecision ?: return false
        if (state.actorFor(decision.playerId) != player) return false
        val name = state.getEntity(card)?.get<CardComponent>()?.name ?: return false
        val masked = masker.maskFor(decision, state, player)
        if (displayedNames(masked)[card] == name) return true
        val context = masked.context
        val named = card in listedIds(masked) || card in context.targetIds ||
            card == context.sourceId || card == context.triggeringEntityId || card == context.subjectEntityId
        if (named && textOf(masked).any { name in it }) {
            throw UndeterminedDisclosure(
                "can't tell whether a ${masked::class.simpleName} shows $card ($name) to $player: it names " +
                    "the card by id and its text contains the name",
            )
        }
        return false
    }

    class UndeterminedDisclosure(message: String) : RuntimeException(message)

    /** The identities a masked decision displays, by card: name per id. */
    private fun displayedNames(masked: PendingDecision): Map<EntityId, String> {
        val context = masked.context
        val sourceId = context.sourceId
        val sourceName = context.sourceName
        val fromContext = if (sourceId != null && sourceName != null) mapOf(sourceId to sourceName) else emptyMap()
        val fromFields: Map<EntityId, String> = when (masked) {
            is SelectCardsDecision -> masked.cardInfo.orEmpty().mapValues { it.value.name }
            is OrderObjectsDecision -> masked.cardInfo.orEmpty().mapValues { it.value.name }
            is SplitPilesDecision -> masked.cardInfo.orEmpty().mapValues { it.value.name }
            is SearchLibraryDecision -> masked.cards.mapValues { it.value.name }
            is ReorderLibraryDecision -> masked.cardInfo.mapValues { it.value.name }
            is CombatResolutionDecision ->
                masked.attackers.associate { it.id to it.name } + masked.blockers.associate { it.id to it.name }
            is SelectManaSourcesDecision -> masked.availableSources.associate { it.entityId to it.name }
            is ChooseTargetsDecision, is YesNoDecision, is BatchYesNoDecision, is ChooseModeDecision,
            is ChooseColorDecision, is ChooseNumberDecision, is DistributeDecision, is ChooseOptionDecision,
            is ChooseReplacementDecision, is AssignDamageDecision, is BudgetModalDecision,
            is FlipOntoBattlefieldDecision, is PlayCardDecision -> emptyMap()
        }
        return fromContext + fromFields
    }

    /** The cards a masked decision names by id only. */
    private fun listedIds(masked: PendingDecision): Set<EntityId> = when (masked) {
        is SelectCardsDecision -> (masked.options + masked.nonSelectableOptions).toSet()
        is OrderObjectsDecision -> masked.objects.toSet()
        is SplitPilesDecision -> masked.cards.toSet()
        is SearchLibraryDecision -> masked.options.toSet()
        is ReorderLibraryDecision -> masked.cards.toSet()
        is CombatResolutionDecision -> emptySet()
        is SelectManaSourcesDecision -> masked.autoPaySuggestion.toSet()
        is ChooseTargetsDecision -> masked.legalTargets.values.flatten().toSet()
        is DistributeDecision -> masked.targets.toSet()
        is ChooseOptionDecision -> masked.optionCardIds.orEmpty().values.flatten().toSet()
        is AssignDamageDecision -> (masked.orderedTargets + masked.attackerId).toSet()
        is PlayCardDecision -> setOf(masked.cardId)
        is YesNoDecision, is BatchYesNoDecision, is ChooseModeDecision, is ChooseColorDecision,
        is ChooseNumberDecision, is ChooseReplacementDecision, is BudgetModalDecision,
        is FlipOntoBattlefieldDecision -> emptySet()
    }

    /** A masked decision's free text, which can carry a card's name. */
    private fun textOf(masked: PendingDecision): List<String> {
        val specific = when (masked) {
            is YesNoDecision -> listOfNotNull(masked.yesText, masked.noText, masked.hint, masked.yesDetail, masked.noDetail)
            is BatchYesNoDecision -> listOf(masked.yesText, masked.noText)
            is ChooseModeDecision -> masked.modes.map { it.text }
            is ChooseOptionDecision -> masked.options
            is ChooseReplacementDecision -> masked.fromOptions + masked.toOptions
            is SelectCardsDecision -> listOfNotNull(masked.selectedLabel, masked.remainderLabel)
            is OrderObjectsDecision -> listOfNotNull(masked.orderingTitle, masked.firstLabel, masked.lastLabel)
            else -> emptyList()
        }
        return listOfNotNull(masked.prompt, masked.context.effectHint) + specific
    }

    /** The owner's hand or library holding [id], if either does. */
    fun hiddenZoneOf(state: GameState, id: EntityId): ZoneKey? = state.turnOrder.firstNotNullOfOrNull { owner ->
        listOf(ZoneKey(owner, Zone.HAND), ZoneKey(owner, Zone.LIBRARY)).firstOrNull { id in state.getZone(it) }
    }

    private fun definitionOf(state: GameState, id: EntityId): CardDefinition =
        requireNotNull(state.getEntity(id)?.get<CardComponent>()?.let { registry.getCard(it.cardDefinitionId) }) {
            "$id has no registered definition"
        }

    private fun view(state: GameState, events: List<GameEvent>, viewer: EntityId): SeatView {
        val decision = state.pendingDecision
        val decides = decision != null && state.actorFor(decision.playerId) == viewer
        val actor = (decision as? PlayCardDecision)?.playerId ?: state.priorityPlayerId
        val legalActions = if (actor == viewer && (decision == null || decision is PlayCardDecision)) {
            enricher.enrich(enumerator.enumerate(state, viewer), state, viewer)
        } else {
            emptyList()
        }
        return SeatView(
            state = stateTransformer.transform(state, viewer),
            events = ClientEventTransformer.transform(events, viewer, state, visibility),
            legalActions = legalActions,
            decision = decision?.takeIf { decides }?.let { decisionEnricher.enrich(it, state, viewer) },
            opponentDecision = decision?.takeIf { !decides }?.let { decisionEnricher.opponentDecisionStatus(it, state, viewer) },
        )
    }

    /** [state] with [entityId] made into [def] underneath — same id, owner and components otherwise. */
    private fun withIdentity(state: GameState, entityId: EntityId, def: CardDefinition): GameState =
        state.updateEntity(entityId) { container ->
            val card = container.get<CardComponent>()!!
            container.with(
                card.copy(
                    cardDefinitionId = def.name,
                    name = def.name,
                    manaCost = def.manaCost,
                    typeLine = def.typeLine,
                    oracleText = def.oracleText,
                    baseStats = def.creatureStats,
                    baseKeywords = def.keywords,
                    baseFlags = def.flags,
                    colors = def.colors,
                    imageUri = def.metadata.imageUri,
                )
            )
        }

    /**
     * The swap for hand or library slots: [HiddenSlotRewrite] rebuilds each slot as its new
     * definition in [forkState]; later, in any state, every component a rewrite changed is changed
     * the same way while it still holds its fork-time value. Components the game added or changed
     * since are kept, so a state where nothing has depended on the cards maps exactly onto the
     * swapped world's.
     */
    private fun slotSwap(forkState: GameState, rewrites: List<Pair<EntityId, CardDefinition>>): (GameState) -> GameState {
        val changes = rewrites.map { (entityId, def) ->
            val before = forkState.getEntity(entityId)!!
            val owner = before.get<OwnerComponent>()!!.playerId
            val after = HiddenSlotRewrite.rewrite(forkState, entityId, def, owner).getEntity(entityId)!!
            val was = before.byType()
            val now = after.byType()
            Triple(entityId, was, now) to (was.keys + now.keys).filter { was[it] != now[it] }
        }
        return { state ->
            changes.fold(state) { acc, (slot, changed) ->
                val (entityId, was, now) = slot
                val container = acc.getEntity(entityId) ?: return@fold acc
                val components = container.byType().toMutableMap()
                for (type in changed) {
                    if (components[type] != was[type]) continue
                    val replacement = now[type]
                    if (replacement == null) components.remove(type) else components[type] = replacement
                }
                acc.withEntity(entityId, ComponentContainer(components))
            }
        }
    }

    private fun ComponentContainer.byType(): Map<Class<*>, Component> = all().associateBy { it::class.java }
}
