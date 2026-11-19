package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.references.Player

/** The two Tribute mode ids recorded on the entering permanent's cast-choices bag. */
const val TRIBUTE_MODE_PAID = "tribute_paid"
const val TRIBUTE_MODE_NOT_PAID = "tribute_not_paid"

/** The Tribute choice the opponent makes, for tribute [n]. */
fun tributeModeOptions(n: Int): List<ModeOption> = listOf(
    ModeOption(
        id = TRIBUTE_MODE_PAID,
        label = "Pay tribute",
        description = "Put ${if (n == 1) "a +1/+1 counter" else "$n +1/+1 counters"} on it",
        iconKey = "plusOneCounter",
    ),
    ModeOption(
        id = TRIBUTE_MODE_NOT_PAID,
        label = "Don't pay tribute",
        description = "Its \"if tribute wasn't paid\" ability triggers",
    ),
)

/**
 * `Conditions.TributeNotPaid` — "if tribute wasn't paid" (CR 702.104b) — true unless the opponent chose to put the tribute
 * counters on the source as it entered. Read off the source's cast-choices bag. Once the creature has
 * left the battlefield that bag is gone, so the condition reads true — which is what the rulings want:
 * the trigger only exists when tribute wasn't paid, and it still resolves after the creature leaves.
 */
internal val TributeNotPaidCondition: Condition = NotCondition(SourceChosenModeIs(TRIBUTE_MODE_PAID))

/**
 * Add Tribute N (CR 702.104a) — "As this creature enters the battlefield, an opponent of your
 * choice may put N +1/+1 counters on it."
 *
 * The keyword is display-only; the mechanic is composed from existing primitives, the same
 * [EntersWithChoice] `MODE` pattern Riot uses:
 *  - an [EntersWithChoice] (`ChoiceType.MODE`, chosen by an opponent) records whether tribute was
 *    paid on the entering permanent as it enters (a replacement effect — CR 614.12, no stack);
 *  - a mode-gated [EntersWithCounters] (`selfOnly`) puts the N +1/+1 counters on it when the
 *    opponent paid, so it *enters* with them.
 *
 * With more than one opponent the controller first chooses which opponent is offered the tribute
 * (recorded under `ChoiceSlot.OPPONENT`); that opponent answers and puts the counters on.
 *
 * The card's "When this creature enters, if tribute wasn't paid, …" ability is an ordinary enters
 * trigger with `interveningIf = Conditions.TributeNotPaid` (CR 702.104b).
 */
fun CardBuilder.tribute(n: Int) {
    require(n > 0) { "Tribute N needs N > 0, got $n" }
    keywordAbility(KeywordAbility.tribute(n))
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            chooser = Player.AnOpponent,
            modeOptions = tributeModeOptions(n),
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = n,
            selfOnly = true,
            condition = SourceChosenModeIs(TRIBUTE_MODE_PAID),
            placedBy = Player.AnOpponent,
        )
    )
}
