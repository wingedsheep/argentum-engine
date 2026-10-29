package com.wingedsheep.mtg.sets.definitions.plc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Imp's Mischief
 * {1}{B}
 * Instant
 * Change the target of target spell with a single target. You lose life equal to that spell's mana
 * value.
 *
 * "With a single target" is a targeting restriction (`withSingleTarget()`, Hydroelectric
 * Specimen), so a spell with several targets can't be chosen even if all but one have become
 * illegal (ruling). The redirect is Willbender's [Effects.ChangeTarget] — the new target is chosen
 * on resolution among the spell's other legal targets. The life loss reads the targeted spell's
 * mana value while it is still on the stack.
 */
val ImpsMischief = card("Imp's Mischief") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Change the target of target spell with a single target. You lose life equal to " +
        "that spell's mana value."

    spell {
        val spellWithSingleTarget = target(TargetFilter.SpellOnStack.withSingleTarget())
        effect = Effects.ChangeTarget() then Effects.LoseLife(
            DynamicAmounts.manaValueOf(spellWithSingleTarget),
            EffectTarget.Controller,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Thomas M. Baxa"
        flavorText = "\"Do the innocent pay for the crimes of the guilty? Of course they do. That's the fate of the weak.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22ec70a6-40b7-41da-a6c0-c140cadf5509.jpg?1783943155"
        ruling("2024-04-12", "You don't choose the new target for the spell until Imp's Mischief resolves. You must change the target if possible. However, you can't change the target to an illegal target. If there are no legal targets to choose from, the target isn't changed. It doesn't matter if the original target has somehow become illegal itself.")
        ruling("2024-04-12", "If a spell targets multiple things, you can't target it with Imp's Mischief, even if all but one of those targets have become illegal.")
        ruling("2004-10-04", "You can't make a spell which is on the stack target itself.")
    }
}
