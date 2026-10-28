package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Roil Eruption — Zendikar Rising #155
 * {1}{R} · Sorcery
 *
 * One target either way; kicking only raises the damage, so it branches on `WasKicked` like
 * Urza's Rage.
 */
val RoilEruption = card("Roil Eruption") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Kicker {5} (You may pay an additional {5} as you cast this spell.)\n" +
        "Roil Eruption deals 3 damage to any target. If this spell was kicked, it deals 5 damage instead."

    keywordAbility(KeywordAbility.kicker("{5}"))

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.DealDamage(5, t),
            otherwise = Effects.DealDamage(3, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Campbell White"
        flavorText = "\"Our feeble villages and tiny encampments stand only at the whim of the Roil.\"\n" +
            "—*The Invokers' Tales*"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86572747-8faa-4242-b059-07d11e6be1cd.jpg?1783929355"
    }
}
