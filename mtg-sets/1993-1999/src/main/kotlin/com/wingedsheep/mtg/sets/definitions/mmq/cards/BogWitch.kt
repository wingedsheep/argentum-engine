package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Witch
 * {2}{B}
 * Creature — Human Spellshaper
 * 1 / 1
 * {B}, {T}, Discard a card: Add {B}{B}{B}.
 *
 * A mana ability: it adds mana and requires no target, so `manaAbility = true`
 * — which derives the timing rule; there is no separate `timing` to author.
 */
val BogWitch = card("Bog Witch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{B}, {T}, Discard a card: Add {B}{B}{B}."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.AddMana(Color.BLACK, 3)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Gao Yan"
        flavorText = "The world is the body. The mana is the blood. The witch is the surgeon."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a926f9e-ee63-4b6e-8e5b-0650b74344a5.jpg"
    }
}
