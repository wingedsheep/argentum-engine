package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** The two Unleash mode ids recorded on the entering permanent's cast-choices bag. */
const val UNLEASH_MODE_COUNTER = "unleash_counter"
const val UNLEASH_MODE_NONE = "unleash_none"

/** The Unleash choice: enter with a +1/+1 counter (and can't block), or enter without one. */
val UNLEASH_MODE_OPTIONS: List<ModeOption> = listOf(
    ModeOption(
        id = UNLEASH_MODE_COUNTER,
        label = "Unleash",
        description = "+1/+1 counter; can't block while it has one",
        iconKey = "plusOneCounter",
    ),
    ModeOption(
        id = UNLEASH_MODE_NONE,
        label = "Don't unleash",
        description = "No counter; can block",
    ),
)

/**
 * Add Unleash (CR 702.98a) — "You may have this permanent enter with an additional +1/+1 counter on
 * it" and "This permanent can't block as long as it has a +1/+1 counter on it."
 *
 * The keyword is display-only; the mechanic is composed from existing primitives, the same
 * [EntersWithChoice] `MODE` pattern Riot and Tribute use:
 *  - an [EntersWithChoice] (`ChoiceType.MODE`, options unleash / don't) records the controller's
 *    choice on the entering permanent as it enters (a replacement effect — CR 614.12, no stack);
 *  - a mode-gated [EntersWithCounters] (`selfOnly`) puts the +1/+1 counter on when unleashed;
 *  - a [CantBlock] gated on the source having **any** +1/+1 counter — not on the choice: a counter
 *    from any other source also stops it blocking, and once its counters are gone it can block
 *    again, even if it was unleashed.
 */
fun CardBuilder.unleash() {
    keywordSet.add(Keyword.UNLEASH)
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = UNLEASH_MODE_OPTIONS,
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs(UNLEASH_MODE_COUNTER),
        )
    )
    staticAbilities.add(
        ConditionalStaticAbility(
            ability = CantBlock(GroupFilter.source()),
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        )
    )
}
