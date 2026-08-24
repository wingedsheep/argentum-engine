package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Balloon Peddler
 * {2}{U}
 * Creature — Human Spellshaper
 * 2 / 2
 * {U}, {T}, Discard a card: Target creature gains flying until end of turn.
 *
 * The Spellshaper cost shape: mana, tap, discard a card — [Costs.DiscardCard] is the whole
 * "Discard a card" atom, so no filter is needed.
 */
val BalloonPeddler = card("Balloon Peddler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{U}, {T}, Discard a card: Target creature gains flying until end of turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Paolo Parente"
        flavorText = "The market festival turned out to be the high point of Jaffy's visit."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c34963e6-850e-4ce4-b04f-5e623ce5b73f.jpg"
    }
}
