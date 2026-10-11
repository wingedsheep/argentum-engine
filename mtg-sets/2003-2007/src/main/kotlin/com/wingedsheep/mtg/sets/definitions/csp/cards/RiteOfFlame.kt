package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rite of Flame
 * {R}
 * Sorcery
 * Add {R}{R}, then add {R} for each card named Rite of Flame in each graveyard.
 *
 * The resolving copy is on the stack, not in a graveyard, so it never counts itself.
 */
val RiteOfFlame = card("Rite of Flame") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Add {R}{R}, then add {R} for each card named Rite of Flame in each graveyard."
    spell {
        effect = Effects.AddMana(
            Color.RED,
            2 + DynamicAmounts.count(Player.Each, Zone.GRAVEYARD, GameObjectFilter.Any.named("Rite of Flame"))
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Dany Orizio"
        flavorText = "Deep beneath the ice, beneath the soil and the rock, Dominaria's fire still burned hot."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c062caf7-f0eb-44db-9f74-e6711a13fada.jpg?1783943336"
    }
}
