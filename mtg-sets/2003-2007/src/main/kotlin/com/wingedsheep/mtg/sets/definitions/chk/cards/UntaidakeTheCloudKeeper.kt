package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Untaidake, the Cloud Keeper
 * Legendary Land
 *
 * Untaidake enters tapped.
 * {T}, Pay 2 life: Add {C}{C}. Spend this mana only to cast legendary spells.
 *
 * The entry clause is the [EntersTapped] replacement effect rather than a trigger — it is a
 * replacement on the zone change, so the land never touches the battlefield untapped. The mana
 * ability carries [ManaRestriction.LegendarySpellsOnly], which `ManaPool` enforces at spend time,
 * and is marked `manaAbility` with [TimingRule.ManaAbility] so it never uses the stack.
 */
val UntaidakeTheCloudKeeper = card("Untaidake, the Cloud Keeper") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "Untaidake enters tapped.\n" +
        "{T}, Pay 2 life: Add {C}{C}. Spend this mana only to cast legendary spells."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(2))
        effect = Effects.AddColorlessMana(2, restriction = ManaRestriction.LegendarySpellsOnly())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "John Avon"
        flavorText = "Untaidake is the needle that weaves the fabric of creation."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c571f66-7a00-4cb0-9da9-8271083f49d3.jpg?1783944272"
    }
}
