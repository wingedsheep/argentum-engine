package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skaab Wrangler
 * {1}{U}
 * Creature — Human Wizard — Uncommon (MID #75)
 * 2/1
 *
 * "Tap three untapped creatures you control: Tap target creature."
 *
 * The cost is [Costs.TapPermanents] (3 creatures), not `{T}`: Skaab Wrangler may be one of the
 * three tapped creatures, and summoning sickness doesn't gate the activation.
 */
val SkaabWrangler = card("Skaab Wrangler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "Tap three untapped creatures you control: Tap target creature."

    activatedAbility {
        cost = Costs.TapPermanents(3, Filters.Creature)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Billy Christian"
        flavorText = "After Uda's hat boutique in Selhoff was destroyed by werewolves, she relocated outside of town and took up a new type of sewing."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9771d9e8-97d5-44af-8612-dc5c44d65ceb.jpg?1783925630"
    }
}
