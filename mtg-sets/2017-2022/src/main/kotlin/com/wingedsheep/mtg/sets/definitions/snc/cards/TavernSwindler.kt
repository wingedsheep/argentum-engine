package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tavern Swindler
 * {1}{B}
 * Creature — Human Rogue
 * 2/2
 * {T}, Pay 3 life: Flip a coin. If you win the flip, you gain 6 life.
 */
val TavernSwindler = card("Tavern Swindler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "{T}, Pay 3 life: Flip a coin. If you win the flip, you gain 6 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(3))
        effect = Effects.FlipCoin(wonEffect = Effects.GainLife(6))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Pauline Voss"
        flavorText = "\"Only a chump stops after one unlucky loss. You're not a chump, are you?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b17114ed-f98f-4b82-ba03-ef7ec5f572ba.jpg?1783923124"
    }
}
