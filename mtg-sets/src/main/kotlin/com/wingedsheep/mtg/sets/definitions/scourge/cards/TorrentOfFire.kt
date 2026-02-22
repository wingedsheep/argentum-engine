package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Torrent of Fire
 * {3}{R}{R}
 * Sorcery
 * Torrent of Fire deals damage to any target equal to the greatest mana value
 * among permanents you control.
 */
val TorrentOfFire = card("Torrent of Fire") {
    manaCost = "{3}{R}{R}"
    typeLine = "Sorcery"
    oracleText = "Torrent of Fire deals damage to any target equal to the greatest mana value among permanents you control."

    spell {
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.battlefield(Player.You).maxManaValue(),
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Thomas M. Baxa"
        flavorText = "Dragon fire melts any instrument designed to measure it."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feeee859-f64a-4cd8-be0b-ad60cff8812e.jpg?1744334031"
    }
}
