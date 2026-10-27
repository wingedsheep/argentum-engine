package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thundering Rebuke — Zendikar Rising #170
 * {1}{R} · Sorcery
 *
 * Thundering Rebuke deals 4 damage to target creature or planeswalker.
 */
val ThunderingRebuke = card("Thundering Rebuke") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Thundering Rebuke deals 4 damage to target creature or planeswalker."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Campbell White"
        flavorText = "\"Fire is too imprecise for my tastes. I'd rather rely on a single, well-placed bolt " +
            "of lightning.\"\n—Lyrus, Sea Gate wizard"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc671707-4708-44d8-8ec8-d5332599adf9.jpg"
    }
}
