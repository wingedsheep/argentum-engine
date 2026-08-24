package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Azami, Lady of Scrolls
 * {2}{U}{U}{U}
 * Legendary Creature — Human Wizard
 * 0/2
 * Tap an untapped Wizard you control: Draw a card.
 *
 * The cost is [Costs.TapPermanents], not [Costs.Tap] — tapping as a cost rather than the `{T}`
 * symbol, so summoning sickness (CR 302.6) never applies and only untapped permanents may be
 * chosen (CR 701.26a). The bare tribal noun means *permanents*, hence
 * `GameObjectFilter.Permanent.withSubtype("Wizard")`; "you control" is carried by the atom itself,
 * and `excludeSelf` stays at its default `false` so Azami may tap herself. The payoff is the plain
 * [Effects.DrawCards] facade at its default controller target.
 */
val AzamiLadyOfScrolls = card("Azami, Lady of Scrolls") {
    manaCost = "{2}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 0
    toughness = 2
    oracleText = "Tap an untapped Wizard you control: Draw a card."

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype("Wizard"))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Ittoku"
        flavorText = "\"Choices belong to those with the luxuries of time and distance. We have neither. I recommend we proceed with the plan to destroy all shrines of the kami.\"\n—Lady Azami, letter to Sensei Hisoka"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15ea91b1-f2ff-4b99-8148-333aa27ea8cd.jpg"
    }
}
