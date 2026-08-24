package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Rushwood Herbalist
 * {2}{G}
 * Creature — Human Spellshaper
 * 2 / 2
 */
val RushwoodHerbalist = card("Rushwood Herbalist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{G}, {T}, Discard a card: Regenerate target creature."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Creature)
        effect = RegenerateEffect(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "265"
        artist = "Terese Nielsen"
        flavorText = "When generals gather their armies, healers gather their herbs."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9afde98f-a429-4eff-9d06-8582267ac74b.jpg"
    }
}
