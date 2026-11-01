package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Untimely Malfunction
 * {1}{R}
 * Instant
 * Choose one —
 * • Destroy target artifact.
 * • Change the target of target spell or ability with a single target.
 * • One or two target creatures can't block this turn.
 *
 * "Choose one" modal spell. Mode 2 reuses the change-target machinery from Willbender /
 * Return the Favor: [Effects.ChangeTarget] + `TargetFilter.SpellOrAbilityOnStack.withSingleTarget()`.
 * "With a single target" is a targeting restriction; the executor enforces the "must change if
 * possible" ruling at resolution. Mode 3 ("one or two target creatures") is a variable target count (minCount = 1,
 * count = 2) with a per-target [Effects.CantBlock] applied via [ForEachTargetEffect] (same shape
 * as Amazing Acrobatics' "tap one or two target creatures").
 */
val UntimelyMalfunction = card("Untimely Malfunction") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Change the target of target spell or ability with a single target.\n" +
        "• One or two target creatures can't block this turn."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("Destroy target artifact") {
                    val artifact = target(TargetFilter.Artifact)
                    effect = Effects.Destroy(artifact)
                },
                Mode.withTarget(
                    effect = Effects.ChangeTarget(),
                    target = TargetObject(filter = TargetFilter.SpellOrAbilityOnStack.withSingleTarget()),
                    description = "Change the target of target spell or ability with a single target"
                ),
                Mode.withTarget(
                    effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0))),
                    target = TargetObject(filter = TargetFilter.Creature, count = 2, minCount = 1),
                    description = "One or two target creatures can't block this turn"
                )
            ),
            chooseCount = 1,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Jarel Threat"
        flavorText = "\"No! This worked perfectly in the lab!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/857bfb0e-17dc-4dda-bc37-3df927a9eae6.jpg?1726286456"
    }
}
