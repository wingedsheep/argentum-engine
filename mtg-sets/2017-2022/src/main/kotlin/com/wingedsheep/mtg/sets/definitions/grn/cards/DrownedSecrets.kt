package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Drowned Secrets
 * {1}{U}
 * Enchantment
 * Whenever you cast a blue spell, target player mills two cards.
 */
val DrownedSecrets = card("Drowned Secrets") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a blue spell, target player mills two cards."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.BLUE))
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Alexander Forssberg"
        flavorText = "Beneath Ravnica's streets runs a layer of tunnels, then caverns and waterways, then terrors and nightmares."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfc98428-7a0f-445f-9d48-eeb76c3e10d6.jpg?1783934187"
    }
}
