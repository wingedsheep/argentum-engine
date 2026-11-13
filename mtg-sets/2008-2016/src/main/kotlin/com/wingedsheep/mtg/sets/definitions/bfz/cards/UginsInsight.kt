package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ugin's Insight
 * {3}{U}{U}
 * Sorcery
 * Scry X, where X is the greatest mana value among permanents you control, then draw three cards.
 *
 * X is the MAX mana-value aggregate over permanents you control, read as the spell resolves (per
 * the ruling). With no permanents, or only mana-value-0 ones (lands, tokens), X is 0 — no scry at
 * all, so "whenever you scry" abilities don't trigger — and the spell just draws three.
 */
val UginsInsight = card("Ugin's Insight") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Scry X, where X is the greatest mana value among permanents you control, then draw three cards."

    spell {
        effect = Effects.Scry(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).maxManaValue()
        ) then Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Tyler Jacobson"
        flavorText = "\"You speak to me of the threat of Ulamog. But do not forget: they came as three.\"\n—Ugin, to Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f37d5938-9c8f-4203-8f3f-d6e89ced28ef.jpg?1783938206"
        ruling("2015-08-25", "Use the highest mana value among permanents you control as Ugin's Insight resolves to determine how many cards you scry.")
        ruling("2015-08-25", "If X is 0, you won't scry at all. Any abilities that trigger whenever you scry won't trigger.")
    }
}
