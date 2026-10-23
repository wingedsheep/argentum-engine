package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Lord of the Forsaken (Innistrad: Midnight Hunt #110)
 * {4}{B}{B} · Creature — Demon 6/6
 * Flying, trample
 * {B}, Sacrifice another creature: Target player mills three cards.
 * Pay 1 life: Add {C}. Spend this mana only to cast a spell from your graveyard.
 *
 * The restricted {C} is [ManaRestriction.CastFromGraveyardOnly] — any cast from your own
 * graveyard (flashback, disturb, escape, a graveyard-cast permission), the same restriction
 * Rootcoil Creeper's mana carries.
 */
val LordOfTheForsaken = card("Lord of the Forsaken") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying, trample\n" +
        "{B}, Sacrifice another creature: Target player mills three cards.\n" +
        "Pay 1 life: Add {C}. Spend this mana only to cast a spell from your graveyard."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }

    activatedAbility {
        cost = Costs.PayLife(1)
        effect = Effects.AddColorlessMana(1, restriction = ManaRestriction.CastFromGraveyardOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5a8846c-3950-4588-97e8-3aa70bcd2403.jpg?1783925614"
    }
}
