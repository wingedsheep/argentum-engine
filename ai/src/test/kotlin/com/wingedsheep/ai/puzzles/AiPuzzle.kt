package com.wingedsheep.ai.puzzles

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId

/**
 * The things the suite is built to catch. Categories are the localizing signal: an arena run says
 * *that* the AI got worse, a category pass-rate says *what* got worse.
 *
 * The first eight are Phase 2's. The last three are Phase 2b's, added because at 44/48 the original
 * suite had four points of headroom left and two of them were the same Phase 9 constant — so the
 * plan's most expensive phase had a two-puzzle localizing signal. Each of the three closes a gap
 * that all 48 shared by construction rather than by choice: nothing was ever on the stack, no
 * puzzle asserted on an [ActivateAbility] even though [PuzzleMove] already spoke it, and combat
 * coverage stopped at flying / deathtouch / first strike / vigilance.
 *
 * The last two are Phase 2c's, and they are the *timing* half of tactics — the question of which
 * window a play belongs in, which the first eleven only ever ask inside combat. Everything before
 * them is answerable from a board: this position, these cards, best move. `timing` and `lastchance`
 * are not, because the same cast is right at the opponent's end step and wrong in our own main, and
 * because a spell on the stack is a deadline that makes plays correct which are wrong a step
 * earlier.
 *
 * See `backlog/engine-ai-improvement.md` § "How we measure" and § Phase 2b.
 */
enum class PuzzleCategory(val id: String, val catches: String) {
    LETHAL_DETECTION("lethal", "Missing an alpha strike / burn-to-face kill"),
    BLOCKING("blocking", "Chump vs trade vs no-block; deathtouch / first strike"),
    REMOVAL_TARGETING("removal", "Shooting the 1/1 instead of the bomb"),
    HOLDING_INSTANTS("instants", "Casting a combat trick in your own main phase"),
    SEQUENCING("sequencing", "Land before spell; the land that unlocks the spell"),
    BOARD_WIPE_TIMING("wipe", "Wrathing while ahead"),
    RACE_MATH("race", "Attack-vs-hold when both players are on a clock"),
    NON_CREATURE_VALUATION("noncreature", "Ignoring an opposing O-Ring / mana rock / anthem"),
    STACK_RESPONSE("respond", "Never answering a spell that is already on the stack"),
    ACTIVATED_ABILITIES("activate", "Pingers, tappers and pump abilities left unused"),
    COMBAT_KEYWORDS("keywords", "Trample / menace / reach / indestructible read as ordinary stats"),
    PRIORITY_TIMING("timing", "The right play in the wrong window — tapping out, or spending an instant on our own turn"),
    LAST_CHANCE("lastchance", "Deadlines on the stack: abilities unused under removal, two-for-ones missed"),
}

/**
 * One tactical position plus a predicate over the move the AI makes in it.
 *
 * **Never assert exact action equality.** A puzzle says *"removal targets the 4/4, not the 1/1"* or
 * *"attacks with at least the evasive creature"*; it does not name a `GameAction`. Exact assertions
 * break on harmless tie-break changes, and a suite that cries wolf is a suite you learn to ignore.
 * [PuzzleMove] carries the vocabulary the checks are written in.
 */
class AiPuzzle(
    /** Stable id, `"<category>-NN"`. It is what `KNOWN_FAILURES` records, so treat it as an API. */
    val id: String,
    val category: PuzzleCategory,
    /** What a competent player does here, in one line. Printed on failure. */
    val expectation: String,
    /** Which seat the AI plays — 1 or 2. Blocking and race puzzles usually want seat 2. */
    val aiSeat: Int,
    /**
     * Build the position. The returned game's `state` is what the AI is asked about, so it must
     * leave [aiSeat] holding priority with no pending decision — [PuzzleRunner] enforces that
     * rather than letting a mis-built position silently score as a pass.
     */
    val position: (ScenarioTestBase.ScenarioBuilder) -> ScenarioTestBase.TestGame,
    /** The predicate. Throws [AssertionError] (via the `should…` helpers) when the move is wrong. */
    val check: PuzzleMove.() -> Unit,
)

/**
 * The move the AI actually chose, in the vocabulary a puzzle asserts over.
 *
 * Everything is keyed by **card name** rather than [EntityId]: a puzzle reads
 * `shouldTarget("Craw Wurm")`, and the ids are scenario-build artifacts nobody should have to
 * thread through an assertion.
 */
class PuzzleMove internal constructor(
    val state: GameState,
    val aiId: EntityId,
    val opponentId: EntityId,
    val action: GameAction,
) {
    private fun nameOf(entityId: EntityId): String =
        state.getEntity(entityId)?.get<CardComponent>()?.name
            ?: when (entityId) {
                aiId -> "you"
                opponentId -> "the opponent"
                else -> entityId.value
            }

    /** Name of the card this action puts into play or onto the stack, if any. */
    val playedCard: String? = when (action) {
        is CastSpell -> nameOf(action.cardId)
        is PlayLand -> nameOf(action.cardId)
        is CycleCard -> nameOf(action.cardId)
        is ActivateAbility -> nameOf(action.sourceId)
        else -> null
    }

    /** Names of everything the action targets. Players read as `"you"` / `"the opponent"`. */
    val targetNames: List<String> = when (action) {
        is CastSpell -> action.targets
        is ActivateAbility -> action.targets
        else -> emptyList()
    }.map { target ->
        when (target) {
            is ChosenTarget.Player -> nameOf(target.playerId)
            is ChosenTarget.Permanent -> nameOf(target.entityId)
            is ChosenTarget.Card -> nameOf(target.cardId)
            is ChosenTarget.Spell -> nameOf(target.spellEntityId)
        }
    }

    /** Declared attackers by card name. Empty when the action is not an attack declaration. */
    val attackerNames: List<String> =
        (action as? DeclareAttackers)?.attackers?.keys?.map(::nameOf).orEmpty()

    /**
     * Declared blocks as `blocker name -> attackers it blocks`, **one entry per blocker**.
     *
     * A list rather than a map on purpose: two creatures with the same name blocking the same
     * attacker is exactly what a gang block against menace looks like, and keying by name collapses
     * them into one entry. That silently turned a correct double block into a reported single one.
     */
    val blockAssignments: List<Pair<String, List<String>>> =
        (action as? DeclareBlockers)?.blockers.orEmpty()
            .map { (blocker, attackers) -> nameOf(blocker) to attackers.map(::nameOf) }

    /** Total projected power of the declared attackers — what a lethal-detection puzzle counts. */
    val attackingPower: Int = (action as? DeclareAttackers)?.attackers?.keys
        ?.sumOf { state.projectedState.getPower(it) ?: 0 } ?: 0

    /** A one-line rendering of the move, shown on every failure. */
    fun describe(): String = when (action) {
        is PassPriority -> "PassPriority"
        is DeclareAttackers ->
            if (attackerNames.isEmpty()) "DeclareAttackers(none)"
            else "DeclareAttackers(${attackerNames.sorted().joinToString(", ")}) — $attackingPower power"
        is DeclareBlockers ->
            if (blockAssignments.isEmpty()) "DeclareBlockers(none)"
            else "DeclareBlockers(" + blockAssignments.joinToString("; ") { (b, a) ->
                "$b blocks ${a.joinToString(" & ")}"
            } + ")"
        else -> {
            val base = "${action::class.simpleName}($playedCard)"
            if (targetNames.isEmpty()) base else "$base → ${targetNames.joinToString(", ")}"
        }
    }

    private fun fail(what: String): Nothing = throw AssertionError("$what, but chose ${describe()}")

    // ── Assertions. Each has many callers across the 66 puzzles; that is what earns them a name. ──

    fun shouldCast(cardName: String) {
        if (action !is CastSpell || playedCard != cardName) fail("Expected to cast $cardName")
    }

    fun shouldNotCast(cardName: String) {
        if (action is CastSpell && playedCard == cardName) fail("Expected NOT to cast $cardName")
    }

    /**
     * Activates [cardName]'s ability. Pair it with [shouldTarget] when the ability is targeted —
     * `shouldActivate` alone only says the AI reached for the right permanent, not that it pointed
     * the ability anywhere sensible.
     */
    fun shouldActivate(cardName: String) {
        if (action !is ActivateAbility || playedCard != cardName) {
            fail("Expected to activate $cardName")
        }
    }

    /** The "don't pay for it here" half of [shouldActivate] — see `instants-14`. */
    fun shouldNotActivate(cardName: String) {
        if (action is ActivateAbility && playedCard == cardName) {
            fail("Expected NOT to activate $cardName")
        }
    }

    fun shouldPlayLand(landName: String? = null) {
        if (action !is PlayLand) fail("Expected to play a land")
        if (landName != null && playedCard != landName) fail("Expected to play $landName")
    }

    /** The action must target [cardName] — and, when it targets several things, [cardName] among them. */
    fun shouldTarget(cardName: String) {
        if (cardName !in targetNames) fail("Expected a target on $cardName")
    }

    fun shouldNotTarget(cardName: String) {
        if (cardName in targetNames) fail("Expected NOT to target $cardName")
    }

    /** Targets the opposing player's face rather than any permanent. */
    fun shouldTargetOpponentFace() = shouldTarget("the opponent")

    fun shouldPass() {
        if (action !is PassPriority) fail("Expected to pass priority")
    }

    /** Attacks with exactly this set of creatures — by name, so duplicates are compared as counts. */
    fun shouldAttackWithExactly(vararg cardNames: String) {
        if (attackerNames.sorted() != cardNames.sorted()) {
            fail("Expected to attack with exactly ${cardNames.sorted().joinToString(", ")}")
        }
    }

    fun shouldAttackWithAtLeast(vararg cardNames: String) {
        val missing = cardNames.toList() - attackerNames.toSet()
        if (missing.isNotEmpty()) fail("Expected to attack with at least ${missing.joinToString(", ")}")
    }

    fun shouldNotAttack() {
        if (action !is DeclareAttackers) fail("Expected an attack declaration")
        if (attackerNames.isNotEmpty()) fail("Expected to declare no attackers")
    }

    /** Lethal detection: the declared attack must put at least [damage] power on the table. */
    fun shouldAttackForAtLeast(damage: Int) {
        if (action !is DeclareAttackers) fail("Expected an attack declaration")
        if (attackingPower < damage) fail("Expected to attack for at least $damage")
    }

    fun shouldBlock(blocker: String, attacker: String) {
        if (blockAssignments.none { (b, attackers) -> b == blocker && attacker in attackers }) {
            fail("Expected $blocker to block $attacker")
        }
    }

    /** At least [count] creatures must be blocking [attacker] — the double-block assertion. */
    fun shouldBlockWithAtLeast(count: Int, attacker: String) {
        val blocking = blockAssignments.count { (_, attackers) -> attacker in attackers }
        if (blocking < count) fail("Expected at least $count blockers on $attacker")
    }

    fun shouldNotBlock() {
        if (action !is DeclareBlockers) fail("Expected a block declaration")
        if (blockAssignments.isNotEmpty()) fail("Expected to declare no blockers")
    }
}
