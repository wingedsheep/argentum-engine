package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Notorious Assassin
 * {3}{B}
 * Creature — Human Spellshaper Assassin
 * 2 / 2
 */
val NotoriousAssassin = card("Notorious Assassin") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Spellshaper Assassin"
    oracleText = "{2}{B}, {T}, Discard a card: Destroy target nonblack creature. It can't be regenerated."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = Effects.Destroy(t, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Heather Hudson"
        flavorText = "He wears his infamy like a fine silk shirt."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/239e48d8-e2ba-4e25-88ef-301420c796b4.jpg"
    }
}
