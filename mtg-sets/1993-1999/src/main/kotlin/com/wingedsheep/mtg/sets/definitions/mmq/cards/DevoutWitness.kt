package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Devout Witness
 * {2}{W}
 * Creature — Human Spellshaper
 * 2 / 2
 * {1}{W}, {T}, Discard a card: Destroy target artifact or enchantment.
 *
 * A repeatable Disenchant — the same [TargetFilter.ArtifactOrEnchantment] single `Or` predicate.
 */
val DevoutWitness = card("Devout Witness") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{1}{W}, {T}, Discard a card: Destroy target artifact or enchantment."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", TargetPermanent(filter = TargetFilter.ArtifactOrEnchantment))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Don Hazeltine"
        flavorText = "The Cho-Arrim fought Mercadia's decadence with more than just swords."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48ca7aeb-09db-4409-9ba2-c5c5500ad72f.jpg"
    }
}
