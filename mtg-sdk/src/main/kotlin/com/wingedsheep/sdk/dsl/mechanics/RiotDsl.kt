package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** The two Riot mode ids, shared between the printed DSL and the engine's granted-riot synthesis. */
const val RIOT_MODE_COUNTER = "counter"
const val RIOT_MODE_HASTE = "haste"

/** The card-defined Riot mode options (a +1/+1 counter, or haste). */
val RIOT_MODE_OPTIONS: List<ModeOption> = listOf(
    ModeOption(id = RIOT_MODE_COUNTER, label = "A +1/+1 counter", iconKey = "plusOneCounter"),
    ModeOption(id = RIOT_MODE_HASTE, label = "Haste", iconKey = "haste"),
)

/**
 * Add Riot (CR 702.136) — "This creature enters the battlefield with your choice of a +1/+1 counter
 * or haste."
 *
 * The keyword is display-only; the mechanic is composed from existing primitives via the Khans-Siege
 * [EntersWithChoice] `MODE` pattern (cf. Outpost Siege):
 *  - an [EntersWithChoice] (`ChoiceType.MODE`, options `counter`/`haste`) records the choice on the
 *    entering permanent's cast-choices bag as it enters (CR 614.12);
 *  - a mode-gated [EntersWithCounters] (`selfOnly`, `condition = SourceChosenModeIs("counter")`)
 *    adds the +1/+1 counter when the counter mode was chosen;
 *  - a mode-gated [ConditionalStaticAbility] granting [Keyword.HASTE] to the source when the haste
 *    mode was chosen.
 *
 * When Riot is *granted* to other permanents ("Other Spiders you control have riot"), the engine
 * synthesizes the same [EntersWithChoice] and applies the chosen branch directly (see the
 * granted-riot synthesis in the stack/permanent entry seams) — a granted creature has none of the
 * printed replacement/static abilities above.
 */
fun CardBuilder.riot() {
    keywordSet.add(Keyword.RIOT)
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = RIOT_MODE_OPTIONS,
        )
    )
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
            selfOnly = true,
            condition = SourceChosenModeIs(RIOT_MODE_COUNTER),
        )
    )
    staticAbilities.add(
        ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = SourceChosenModeIs(RIOT_MODE_HASTE),
        )
    )
}
