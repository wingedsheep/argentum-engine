package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Maraleaf Pixie — Throne of Eldraine #196
 * {G}{U} · Creature — Faerie · 2/2
 *
 * Flying
 * {T}: Add {G} or {U}.
 */
val MaraleafPixie = card("Maraleaf Pixie") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Faerie"
    power = 2
    toughness = 2
    oracleText = "Flying\n{T}: Add {G} or {U}."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Matt Cavotta"
        flavorText = "Neither hostile nor friendly, pixies flit through the forest seeking the treasures of the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6d7f9c9-dd83-4684-a949-1c22f316138a.jpg?1783932595"
    }
}
