package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tonic Peddler
 * {1}{W}
 * Creature — Human Spellshaper
 * 1 / 1
 */
val TonicPeddler = card("Tonic Peddler") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{W}, {T}, Discard a card: Target player gains 3 life."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Player)
        effect = Effects.GainLife(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Adam Rex"
        flavorText = "\"The price is written at the bottom of the cup.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/334bbd9d-3549-4352-9635-d772aab28503.jpg"
    }
}
