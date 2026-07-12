package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Piercing Light
 * {W}
 * Instant
 * Piercing Light deals 2 damage to target attacking or blocking creature. Scry 1.
 */
val PiercingLight = card("Piercing Light") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Piercing Light deals 2 damage to target attacking or blocking creature. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = Effects.Composite(
            DealDamageEffect(2, t),
            Patterns.Library.scry(1)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Donato Giancola"
        flavorText = "\"Creatures of the night dissolve like shadows in the light of faith.\"\n—Thalia, Guardian of Thraben"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54cc4e2b-1497-4788-afb4-9e42b7683b5a.jpg?1782703174"
    }
}
