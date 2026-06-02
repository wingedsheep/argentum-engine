package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barrensteppe Siege
 * {2}{W}{B}
 * Enchantment
 *
 * As this enchantment enters, choose Abzan or Mardu.
 * • Abzan — At the beginning of your end step, put a +1/+1 counter on each creature you control.
 * • Mardu — At the beginning of your end step, if a creature died under your control this turn,
 *   each opponent sacrifices a creature of their choice.
 *
 * Implementation: the cast-time choice is recorded via the generic [EntersWithChoice]
 * (ChoiceType.MODE), which writes a stable mode id onto the permanent. Each end-step
 * triggered ability is gated by [SourceChosenModeIs] so only the active mode fires.
 *
 * The Mardu intervening-if uses [Conditions.ControlledCreatureDiedThisTurn] (scoped to the
 * source's controller, per the "under your control" wording — distinct from the global
 * "a creature died this turn"). Per CR 603.4 the condition is checked both as the end step
 * begins (so the ability won't trigger at all if no creature died) and again on resolution.
 */
val BarrensteppeSiege = card("Barrensteppe Siege") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Abzan or Mardu.\n" +
        "• Abzan — At the beginning of your end step, put a +1/+1 counter on each creature you control.\n" +
        "• Mardu — At the beginning of your end step, if a creature died under your control this turn, " +
        "each opponent sacrifices a creature of their choice."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "abzan",
                    label = "Abzan",
                    description = "At your end step, put a +1/+1 counter on each creature you control.",
                    iconKey = "abzan"
                ),
                ModeOption(
                    id = "mardu",
                    label = "Mardu",
                    description = "At your end step, if a creature died under your control this turn, each opponent sacrifices a creature.",
                    iconKey = "mardu"
                )
            )
        )
    )

    // Abzan — At the beginning of your end step, put a +1/+1 counter on each creature you control.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = SourceChosenModeIs("abzan")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    // Mardu — At the beginning of your end step, if a creature died under your control this turn,
    // each opponent sacrifices a creature of their choice.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = SourceChosenModeIs("mardu")
        effect = ConditionalEffect(
            condition = Conditions.ControlledCreatureDiedThisTurn,
            effect = Effects.Sacrifice(
                filter = GameObjectFilter.Creature,
                count = 1,
                target = EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.EachOpponent)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2556a35b-2229-42c7-8cb3-c8c668403dd2.jpg?1743204657"
        ruling("2025-04-04", "If you somehow control Barrensteppe Siege and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two abilities.")
        ruling("2025-04-04", "Barrensteppe Siege's Mardu ability will check as the end step starts to see if a creature died under your control this turn. If none did, the ability won't trigger at all.")
    }
}
