package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Gideon's Reproach
 * {1}{W}
 * Instant
 * Gideon's Reproach deals 4 damage to target attacking or blocking creature.
 */
val GideonsReproach = card("Gideon's Reproach") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Gideon's Reproach deals 4 damage to target attacking or blocking creature."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Izzy"
        flavorText = "\"On Amonkhet, Gideon lost both his sural and his faith in himself. But he can still throw a punch.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b771f44-ce32-41a2-b219-738924b7f42d.jpg?1562738270"
    }
}
