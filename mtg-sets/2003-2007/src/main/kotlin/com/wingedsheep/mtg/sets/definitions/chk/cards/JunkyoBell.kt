package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Junkyo Bell
 * {4}
 * Artifact
 * At the beginning of your upkeep, you may have target creature you control get +X/+X until end of
 * turn, where X is the number of creatures you control. If you do, sacrifice that creature at the
 * beginning of the next end step.
 *
 * The engine asks the "you may" of this optional trigger as it goes on the stack, then the target;
 * one yes both pumps the creature and arms the delayed sacrifice ("if you do" — the Attack-in-the-Box
 * shape). X is counted as the pump resolves and locked in for the turn. The delayed sacrifice bakes
 * the bound target into a concrete entity id when scheduled (the Lowland Oaf shape), so it hits the
 * creature that was pumped — and finds nothing if that creature has already left the battlefield.
 */
val JunkyoBell = card("Junkyo Bell") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, you may have target creature you control get +X/+X " +
        "until end of turn, where X is the number of creatures you control. If you do, sacrifice that " +
        "creature at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.May(
            Effects.ModifyStats(
                DynamicAmounts.creaturesYouControl(),
                DynamicAmounts.creaturesYouControl(),
                creature,
            ) then Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.SacrificeTarget(creature),
            ),
            descriptionOverride = "Have the target creature get +X/+X until end of turn, where X is the " +
                "number of creatures you control? (If you do, sacrifice it at the beginning of the next end step.)",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Kensuke Okabayashi"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bd002c7-21ec-4f77-81be-2788fa267f21.jpg?1783944278"
    }
}
