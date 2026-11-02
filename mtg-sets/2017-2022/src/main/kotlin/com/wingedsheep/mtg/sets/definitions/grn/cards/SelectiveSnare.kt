package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selective Snare
 * {X}{U}
 * Sorcery
 * Return X target creatures of the creature type of your choice to their owner's hand.
 *
 * Exactly X targets (`exactly = X`, Builder's Bane's shape). "Of the creature type of your choice"
 * is the targets' shared creature type, as on Aphetto Dredging: naming a type and targeting only
 * creatures of it admits exactly the target sets whose members all share one, so the requirement
 * carries `sameCreatureType` (projected types on the battlefield; changelings share every type).
 */
val SelectiveSnare = card("Selective Snare") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return X target creatures of the creature type of your choice to their owner's hand."

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue(), sameCreatureType = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Alex Konstad"
        flavorText = "\"How thoughtful of the Legion to train its soldiers to fit our spell's specifications.\"\n—Nefara, Dimir agent"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2adfc0ac-08a9-487a-b01c-5de52fae0312.jpg?1783934183"
    }
}
