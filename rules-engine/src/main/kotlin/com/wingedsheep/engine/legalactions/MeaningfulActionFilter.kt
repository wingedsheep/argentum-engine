package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * The minimal shape of a legal action that the meaningful-action and auto-pass rules read.
 *
 * Two types answer these questions: the engine's own [LegalAction] and the server DTO
 * `LegalActionInfo` the client is sent. Both live in this module, but only one of them may be
 * changed freely — the DTO is a wire contract — so [LegalAction] implements this directly and the
 * DTO is adapted by `LegalActionInfo.asPriorityAction()` (in the `view` package, which already
 * depends on this one).
 */
interface PriorityAction {
    val actionType: String
    val requiresTargets: Boolean
    val validTargets: List<EntityId>?
    val validAttackers: List<EntityId>?
    val validBlockers: List<EntityId>?
    val isManaAbility: Boolean
    val holdPriority: Boolean

    /** Whether the player can actually pay for this action right now. */
    val isAffordableAction: Boolean

    /** [AdditionalCostData.costType], flattened so the DTO's parallel type can answer it too. */
    val additionalCostType: String?

    /**
     * True when some **mandatory** target requirement has no legal target, so the action cannot
     * legally be taken however the player fills the rest in.
     *
     * Not the same question as [validTargets] being empty. That field only ever mirrors the
     * *first* requirement, so a two-requirement spell ("destroy target creature and target
     * artifact") whose second slot is empty reads as perfectly castable through it — and the
     * engine then rejects the cast with "No valid targets available". Phase 1's arena measured
     * that rejection at ~0.9 per game, 889 of 945 rejections, and this is its shape.
     *
     * An *optional* requirement (`minTargets == 0`) with no legal targets is fine and does not
     * count: the player simply picks none.
     */
    val hasUnfillableTargetRequirement: Boolean
}

/**
 * Why a priority window was (or was not) auto-passed.
 *
 * The reason is not decoration: the game-server logs it at debug level, and it is the only way to
 * tell "auto-passed because nothing was meaningful" from "auto-passed because our own spell is on
 * top of the stack" when a player reports the game speeding past a window they wanted.
 */
data class AutoPassVerdict(val autoPass: Boolean, val reason: String)

/**
 * Arena-style priority rules — which actions are worth stopping for, and which whole priority
 * windows can be skipped outright.
 *
 * This used to live in `game-server`'s `AutoPassManager` over the client DTO, which meant the AI
 * and the gym could not call it: both hold engine [LegalAction]s and never build a DTO. The rules
 * are engine rules — nothing here is presentation — so they live here and `AutoPassManager`
 * delegates. One source of truth: a window the client speeds past and a window the AI thinks about
 * are now decided by the same code.
 *
 * Three entry points, in increasing order of how much they save:
 *
 * - [isMeaningful] / [filterMeaningful] — shrink a candidate list. Drops `PassPriority`, mana
 *   abilities without a sacrifice cost, targeted spells with no legal targets, unaffordable
 *   casts/cycles/crews, and empty combat declarations.
 * - [autoPassVerdict] — decide a whole window from the enumerated actions. The AI can return
 *   `PassPriority` without scoring anything.
 * - [canAutoPassWithoutEnumerating] — decide a whole window from the **state alone**, so the
 *   caller never pays for `LegalActionEnumerator.enumerate` at all. This is the one that matters
 *   for search: Phase 0 measured 76.2% of priority windows offering zero candidates while still
 *   paying ~0.40 ms to find that out, and a rollout crosses ~20-30 windows per playout.
 *
 * The third is a strict subset of the second by construction — it fires only on the
 * (turn, step) combinations where [autoPassVerdict] ignores the action list entirely — and
 * `AutoPassParityTest` holds that invariant over a corpus of real game states.
 */
object MeaningfulActionFilter {

    /**
     * Every action type that represents casting a spell, across *all* cost paths. The enumerators
     * emit a distinct `actionType` per cost variant (a plain cast, each modal shape, and every
     * alternative cost — flashback, harmonize, warp, evoke/impending/granted via
     * `CastWithAlternativeCost`, kicker, conspire, a free cast, morph). The auto-pass logic must
     * treat them all as "a spell you can cast"; missing one (e.g. Sneak, which is only ever a
     * `CastWithAlternativeCost`) makes the client speed past the window where it's legal. Only
     * legal actions are ever passed in, so any of these present means the player can act now.
     */
    val SPELL_CAST_ACTION_TYPES = setOf(
        "CastSpell",
        "CastSpellMode",
        "CastSpellModal",
        "CastWithAlternativeCost",
        "CastWithFlashback",
        "CastWithHarmonize",
        "CastWithWarp",
        "CastWithKicker",
        // Gift (CR 702.174a) — the "promise a gift" twin of a normal cast.
        "CastWithGift",
        // Splice (CR 702.47a) — the "reveal a card from hand and add its text" twin of a normal cast.
        // An Arcane instant spliced onto at instant speed is exactly the window auto-pass must not skip.
        "CastWithSplice",
        "CastWithConspire",
        "CastWithoutPayingManaCost",
        "CastFaceDown",
    )

    /** Spell casts plus the activated / special abilities that count as instant-speed responses. */
    val INSTANT_RESPONSE_ACTION_TYPES = SPELL_CAST_ACTION_TYPES + setOf(
        "ActivateAbility",
        "CycleCard",
        "TypecycleCard",
        "PlotCard",
        "ForetellCard",
        // Crew is an activated ability (CR 702.122a) with no timing restriction, so it can be
        // activated any time you have priority (CR 117.1b) — making it a valid instant-speed
        // response (e.g. crewing during the opponent's declare-attackers window). Saddle is
        // sorcery-speed and deliberately absent.
        "CrewVehicle",
    )

    /** Combat steps the engine auto-skips when no creatures are attacking (CR 508.8). */
    val COMBAT_STEPS_SKIPPED_WITHOUT_ATTACKERS = setOf(
        Step.DECLARE_BLOCKERS,
        Step.FIRST_STRIKE_COMBAT_DAMAGE,
        Step.COMBAT_DAMAGE,
    )

    /**
     * Steps on the player's **own** turn where [autoPassVerdict] passes no matter what actions are
     * available — the basis of [canAutoPassWithoutEnumerating]. Derived from
     * [shouldAutoPassOnMyTurn]: every branch there that ignores its `meaningfulActions` argument.
     */
    private val UNCONDITIONAL_PASS_ON_MY_TURN = setOf(
        Step.UNTAP, Step.UPKEEP, Step.DRAW,
        Step.BEGIN_COMBAT, Step.COMBAT_DAMAGE, Step.END_COMBAT,
        Step.END, Step.CLEANUP,
    )

    /** The same, on an opponent's turn. Derived from [shouldAutoPassOnOpponentTurn]. */
    private val UNCONDITIONAL_PASS_ON_OPPONENT_TURN = setOf(
        Step.UNTAP, Step.UPKEEP, Step.DRAW,
        Step.BEGIN_COMBAT, Step.COMBAT_DAMAGE, Step.CLEANUP,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1: the meaningful-action filter
    // ─────────────────────────────────────────────────────────────────────────

    /** Whether this action is worth stopping the game — or scoring — for. */
    fun isMeaningful(action: PriorityAction): Boolean {
        // PassPriority is never meaningful (it's the default).
        if (action.actionType == "PassPriority") return false

        // Mana abilities are invisible — they don't stop the game. Exception: a mana ability with
        // a sacrifice cost (Skirk Prospector) is a real game action.
        if (action.isManaAbility && action.additionalCostType != "SacrificePermanent") return false

        // Combat declarations are meaningful only when there is something to declare.
        if (action.actionType == "DeclareAttackers") return !action.validAttackers.isNullOrEmpty()
        if (action.actionType == "DeclareBlockers") return !action.validBlockers.isNullOrEmpty()

        // A land drop is always meaningful when it's offered.
        if (action.actionType == "PlayLand") return true

        // A targeted spell that cannot be legally targeted is invisible — casting it isn't
        // possible. Both shapes count: the single-requirement one whose `validTargets` is empty,
        // and the multi-requirement one with an unfillable mandatory slot.
        if (action.requiresTargets && action.validTargets.isNullOrEmpty()) return false
        if (action.hasUnfillableTargetRequirement) return false

        // A non-mana activated ability is meaningful.
        if (action.actionType == "ActivateAbility") return true

        // Everything with a cost is meaningful only if it can be paid. (Regular spells are only
        // enumerated when affordable, but companion actions for cycling/morph cards are added
        // with isAffordable = false.)
        if (action.actionType in SPELL_CAST_ACTION_TYPES ||
            action.actionType == "CycleCard" || action.actionType == "TypecycleCard" ||
            action.actionType == "PlotCard" || action.actionType == "ForetellCard" ||
            action.actionType == "CrewVehicle"
        ) {
            return action.isAffordableAction
        }

        return true
    }

    /** [isMeaningful] over a list, preserving the caller's own action type. */
    fun <T : PriorityAction> filterMeaningful(actions: List<T>): List<T> = actions.filter(::isMeaningful)

    private fun PriorityAction.isInstantSpeedResponse(): Boolean =
        actionType in INSTANT_RESPONSE_ACTION_TYPES &&
            (!requiresTargets || !validTargets.isNullOrEmpty())

    // ─────────────────────────────────────────────────────────────────────────
    // The whole-window decision
    // ─────────────────────────────────────────────────────────────────────────

    /** [autoPassVerdict]'s boolean, for callers that don't want the reason. */
    fun shouldAutoPass(
        state: GameState,
        playerId: EntityId,
        legalActions: List<PriorityAction>,
        myTurnStops: Set<Step> = emptySet(),
        opponentTurnStops: Set<Step> = emptySet(),
        stopsMode: Boolean = false,
        cardRegistry: CardRegistry? = null,
    ): Boolean = autoPassVerdict(
        state, playerId, legalActions, myTurnStops, opponentTurnStops, stopsMode, cardRegistry
    ).autoPass

    /**
     * Should the player holding priority pass without being asked?
     *
     * @param myTurnStops / [opponentTurnStops] per-step stop overrides a human player configured.
     *   The AI passes neither.
     * @param stopsMode the human "Stops" priority mode, which is more conservative than "Auto".
     * @param cardRegistry resolves the actually-cast face of a split-layout spell on the stack
     *   (CR 709/715). Null falls back to the base card's type line — fine for a single-face test,
     *   but production must supply it so an Omen/Adventure instant face isn't mistaken for the
     *   permanent face and auto-resolved past.
     */
    fun autoPassVerdict(
        state: GameState,
        playerId: EntityId,
        legalActions: List<PriorityAction>,
        myTurnStops: Set<Step> = emptySet(),
        opponentTurnStops: Set<Step> = emptySet(),
        stopsMode: Boolean = false,
        cardRegistry: CardRegistry? = null,
    ): AutoPassVerdict {
        if (state.priorityPlayerId != playerId) return STOP_NO_PRIORITY
        if (state.pendingDecision != null) return STOP_PENDING_DECISION

        val meaningfulActions = legalActions.filter(::isMeaningful)

        // ── Rule 4: the stack response ──
        if (state.stack.isNotEmpty()) {
            return stackVerdict(state, playerId, meaningfulActions, stopsMode, cardRegistry)
        }

        // Team-aware (CR 805/810): in a shared Two-Headed Giant turn BOTH teammates are "on their
        // turn", so the non-active teammate still stops in their own main phase instead of being
        // auto-passed as if it were the opponents' turn. Reduces to `activePlayerId == playerId`
        // in a game without teams.
        val isMyTurn = state.isActiveTurnFor(playerId)

        val relevantStops = if (isMyTurn) myTurnStops else opponentTurnStops
        if (state.step in relevantStops) {
            return AutoPassVerdict(false, "STOP: Per-step stop override set for ${state.step}")
        }

        if (stopsMode && !isMyTurn &&
            (state.step == Step.COMBAT_DAMAGE || state.step == Step.FIRST_STRIKE_COMBAT_DAMAGE) &&
            hasAttackers(state)
        ) {
            return AutoPassVerdict(false, "STOP (Stops mode): Combat damage step with attackers")
        }

        // Never auto-pass the active player's own main phases.
        if (isMyTurn && (state.step == Step.PRECOMBAT_MAIN || state.step == Step.POSTCOMBAT_MAIN)) {
            return AutoPassVerdict(false, "STOP: My main phase (always stop)")
        }

        if (meaningfulActions.isEmpty()) return PASS_NOTHING_MEANINGFUL

        // On your own declare-blockers step, after the opponent has blocked, you may still want a
        // combat trick before damage. (On the opponent's turn this is handled below, which stops
        // for blockers too.)
        if (state.step == Step.DECLARE_BLOCKERS && isMyTurn &&
            meaningfulActions.any { it.isInstantSpeedResponse() }
        ) {
            return AutoPassVerdict(false, "STOP: Declare blockers step (have instant-speed responses)")
        }

        // On the opponent's declare attackers, pass if they didn't attack — nothing to respond to.
        if (!isMyTurn && state.step == Step.DECLARE_ATTACKERS && !hasAttackers(state)) {
            return PASS_NO_ATTACKERS_DECLARED
        }

        return if (isMyTurn) {
            shouldAutoPassOnMyTurn(state.step, meaningfulActions)
        } else {
            shouldAutoPassOnOpponentTurn(state.step, meaningfulActions)
        }
    }

    /**
     * Can this priority window be passed **without enumerating legal actions at all**?
     *
     * True only where [autoPassVerdict] would return `autoPass = true` for *any* action list, so a
     * caller may substitute this for the full check and never pay for enumeration. The guarantee is
     * one-directional: this returning false says nothing, and the caller must then enumerate.
     *
     * Deliberately excludes every window whose verdict depends on what the player is holding —
     * both main phases, both combat declarations, first-strike damage, end of combat, and the
     * opponent's end step. It also requires an empty stack (a non-empty one routes through the
     * stack rules, which read `holdPriority`) and no stop overrides, which is why the human-facing
     * server never calls it.
     */
    fun canAutoPassWithoutEnumerating(state: GameState, playerId: EntityId): Boolean {
        if (state.priorityPlayerId != playerId) return false
        if (state.pendingDecision != null) return false
        if (state.stack.isNotEmpty()) return false

        val isMyTurn = state.isActiveTurnFor(playerId)
        val unconditional =
            if (isMyTurn) UNCONDITIONAL_PASS_ON_MY_TURN else UNCONDITIONAL_PASS_ON_OPPONENT_TURN
        if (state.step in unconditional) return true

        // The one action-independent window left: the opponent declared attackers and there are
        // none, so there is nothing to respond to. Cheap — `getBattlefield()` is memoized.
        return !isMyTurn && state.step == Step.DECLARE_ATTACKERS && !hasAttackers(state)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 4: the stack response
    // ─────────────────────────────────────────────────────────────────────────

    private fun stackVerdict(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<PriorityAction>,
        stopsMode: Boolean,
        cardRegistry: CardRegistry?,
    ): AutoPassVerdict {
        val topOfStack = state.stack.last() // Stack is LIFO, last = top
        val topController = stackItemController(state, topOfStack)

        if (topController == playerId) {
            // Our own spell/ability is on top — pass and let the opponent respond, unless an
            // available action asked to hold priority (e.g. a copy-spell ability).
            return if (meaningfulActions.any { it.holdPriority }) {
                STOP_OWN_STACK_ITEM_BUT_HOLDING
            } else {
                PASS_OWN_STACK_ITEM
            }
        }

        // Persistent yield: the player explicitly asked to stop being stopped by this ability, so
        // pass even though it's an opponent's object. A holdPriority action — a response they
        // deliberately lined up — still wins.
        val topContainer = state.getEntity(topOfStack)
        val yieldedIdentity = topContainer?.get<TriggeredAbilityOnStackComponent>()?.abilityIdentity
            ?: topContainer?.get<ActivatedAbilityOnStackComponent>()?.abilityIdentity
        if (yieldedIdentity != null &&
            state.isYieldingTo(playerId, yieldedIdentity) &&
            meaningfulActions.none { it.holdPriority }
        ) {
            return AutoPassVerdict(true, "AUTO-PASS: Yielded to opponent's ability $yieldedIdentity")
        }

        // Stops mode: always stop on an opponent's stack item, whatever it is.
        if (stopsMode) return STOP_STOPS_MODE_OPPONENT_STACK

        val cardComponent = topContainer?.get<CardComponent>()
        val isPermanentSpell = topContainer?.get<SpellOnStackComponent>()?.let {
            isPermanentSpell(cardComponent, it, cardRegistry)
        } ?: false
        // Auras are excluded: they target, and the opponent should see what is being targeted.
        val isAura = cardComponent?.isAura ?: false

        if (isPermanentSpell && !isAura &&
            meaningfulActions.none { it.actionType in INSTANT_RESPONSE_ACTION_TYPES }
        ) {
            return PASS_OPPONENT_PERMANENT_SPELL
        }

        return STOP_OPPONENT_STACK_ITEM
    }

    /**
     * Whether a spell on the stack is a permanent spell, respecting the actually-cast face.
     *
     * Split-layout cards (Omen, Adventure, MDFC — CR 709 / 715) keep their *base* characteristics
     * (a creature) in [CardComponent] even when cast as their instant/sorcery face. Resolving the
     * type from the cast face, as `StackResolver` does, is what keeps such a spell off the
     * "permanent spell → auto-resolve" path.
     */
    private fun isPermanentSpell(
        cardComponent: CardComponent?,
        spell: SpellOnStackComponent,
        cardRegistry: CardRegistry?,
    ): Boolean {
        if (cardComponent == null) return false
        val faceTypeLine = spell.faceIndex?.let { idx ->
            cardRegistry?.getCard(cardComponent.name)?.cardFaces?.getOrNull(idx)?.typeLine
        }
        return faceTypeLine?.isPermanent ?: cardComponent.isPermanent
    }

    /** Controller of a stack item. Abilities and spells carry different components. */
    private fun stackItemController(state: GameState, entityId: EntityId): EntityId? {
        val container = state.getEntity(entityId) ?: return null
        container.get<ActivatedAbilityOnStackComponent>()?.let { return it.controllerId }
        container.get<TriggeredAbilityOnStackComponent>()?.let { return it.controllerId }
        container.get<SpellOnStackComponent>()?.let { return it.casterId }
        return container.get<ControllerComponent>()?.playerId
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rules 2 and 3: per-step compression
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rule 3 — your own turn. Arena is very aggressive here: you stop at main phases and at
     * declare attackers, and speed through everything else unless a combat trick is live.
     *
     * Every branch that ignores [meaningfulActions] is listed in [UNCONDITIONAL_PASS_ON_MY_TURN];
     * keep the two in step.
     */
    private fun shouldAutoPassOnMyTurn(
        step: Step,
        meaningfulActions: List<PriorityAction>,
    ): AutoPassVerdict = when (step) {
        Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN ->
            AutoPassVerdict(false, "STOP: My main phase")

        // Stop only while we still need to declare. Once attackers are confirmed, pass so the
        // opponent can declare blockers.
        Step.DECLARE_ATTACKERS ->
            if (meaningfulActions.any { it.actionType == "DeclareAttackers" }) {
                AutoPassVerdict(false, "STOP: My declare attackers step (need to declare)")
            } else {
                AutoPassVerdict(true, "AUTO-PASS: My declare attackers step (already declared)")
            }

        Step.DECLARE_BLOCKERS -> respondOrPass(
            meaningfulActions,
            "My declare blockers step",
            passSuffix = "no responses, moving to damage",
        )

        Step.FIRST_STRIKE_COMBAT_DAMAGE -> respondOrPass(
            meaningfulActions, "My first strike damage step",
        )

        Step.UPKEEP, Step.DRAW -> AutoPassVerdict(true, "AUTO-PASS: My upkeep/draw step")
        Step.BEGIN_COMBAT -> AutoPassVerdict(true, "AUTO-PASS: My begin combat")
        Step.COMBAT_DAMAGE, Step.END_COMBAT ->
            AutoPassVerdict(true, "AUTO-PASS: My combat damage/end combat")
        Step.END -> AutoPassVerdict(true, "AUTO-PASS: My end step")
        Step.CLEANUP, Step.UNTAP -> AutoPassVerdict(true, "AUTO-PASS: Cleanup/Untap")
    }

    /**
     * Rule 2 — an opponent's turn. You stop where you can actually act: a main phase or an end
     * step with a response, the post-declaration combat windows, and blockers.
     *
     * Every branch that ignores [meaningfulActions] is listed in
     * [UNCONDITIONAL_PASS_ON_OPPONENT_TURN]; keep the two in step.
     */
    private fun shouldAutoPassOnOpponentTurn(
        step: Step,
        meaningfulActions: List<PriorityAction>,
    ): AutoPassVerdict = when (step) {
        Step.UPKEEP, Step.DRAW -> AutoPassVerdict(true, "AUTO-PASS: Opponent's upkeep/draw")

        Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN ->
            respondOrPass(meaningfulActions, "Opponent's main phase")

        Step.BEGIN_COMBAT ->
            AutoPassVerdict(true, "AUTO-PASS: Opponent's begin combat (Arena-style)")

        // The priority window after attackers are declared (CR 508.2), where the defending player
        // can act before blockers.
        Step.DECLARE_ATTACKERS ->
            respondOrPass(meaningfulActions, "Opponent's declare attackers")

        // Stop if we have creatures that can block OR an instant-speed action we can pay for.
        Step.DECLARE_BLOCKERS -> {
            val hasBlockers = meaningfulActions.any {
                it.actionType == "DeclareBlockers" && !it.validBlockers.isNullOrEmpty()
            }
            if (hasBlockers) {
                AutoPassVerdict(false, "STOP: Opponent's declare blockers (have blockers)")
            } else {
                respondOrPass(meaningfulActions, "Opponent's declare blockers", passSuffix = "no blockers or responses")
            }
        }

        Step.FIRST_STRIKE_COMBAT_DAMAGE ->
            respondOrPass(meaningfulActions, "Opponent's first strike damage step")

        Step.COMBAT_DAMAGE -> AutoPassVerdict(true, "AUTO-PASS: Opponent's combat damage")

        // A real priority window (CR 511.1) — and the *only* one for abilities restricted to it
        // (a Desert's "Activate only during the end of combat step"), whose targets also vanish
        // once the step ends (CR 511.3).
        Step.END_COMBAT -> respondOrPass(meaningfulActions, "Opponent's end combat step")

        Step.END -> respondOrPass(meaningfulActions, "Opponent's end step")

        Step.CLEANUP, Step.UNTAP -> AutoPassVerdict(true, "AUTO-PASS: Cleanup/Untap")
    }

    private fun respondOrPass(
        meaningfulActions: List<PriorityAction>,
        window: String,
        passSuffix: String = "no responses",
    ): AutoPassVerdict =
        if (meaningfulActions.any { it.isInstantSpeedResponse() }) {
            AutoPassVerdict(false, "STOP: $window (have instant-speed responses)")
        } else {
            AutoPassVerdict(true, "AUTO-PASS: $window ($passSuffix)")
        }

    private fun hasAttackers(state: GameState): Boolean = state.getBattlefield().any { entityId ->
        state.getEntity(entityId)?.get<AttackingComponent>() != null
    }

    // Verdicts with no interpolated detail, hoisted so the hot path allocates nothing.
    private val STOP_NO_PRIORITY = AutoPassVerdict(false, "STOP: Player does not hold priority")
    private val STOP_PENDING_DECISION = AutoPassVerdict(false, "STOP: A decision is pending")
    private val PASS_NOTHING_MEANINGFUL = AutoPassVerdict(true, "AUTO-PASS: No meaningful actions available")
    private val PASS_NO_ATTACKERS_DECLARED =
        AutoPassVerdict(true, "AUTO-PASS: Opponent's declare attackers (no attackers declared)")
    private val PASS_OWN_STACK_ITEM = AutoPassVerdict(true, "AUTO-PASS: Own spell/ability on top of stack")
    private val STOP_OWN_STACK_ITEM_BUT_HOLDING =
        AutoPassVerdict(false, "STOP: Own spell/ability on top of stack but player has holdPriority action")
    private val STOP_STOPS_MODE_OPPONENT_STACK =
        AutoPassVerdict(false, "STOP (Stops mode): Opponent's spell/ability on stack")
    private val PASS_OPPONENT_PERMANENT_SPELL =
        AutoPassVerdict(true, "AUTO-PASS: Opponent's permanent spell on stack, no responses")
    private val STOP_OPPONENT_STACK_ITEM = AutoPassVerdict(false, "STOP: Opponent's spell/ability on stack")
}
