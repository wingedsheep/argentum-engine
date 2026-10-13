package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blight Pile
 * {1}{B}
 * Creature — Phyrexian
 * 3/3
 * Defender
 * {2}{B}, {T}: Each opponent loses X life, where X is the number of creatures with defender you control.
 *
 * X is counted on resolution from projected state (Blight Pile itself counts while it's still there).
 */
val BlightPile = card("Blight Pile") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian"
    power = 3
    toughness = 3
    oracleText = "Defender\n" +
        "{2}{B}, {T}: Each opponent loses X life, where X is the number of creatures with defender you control."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)).count(),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        ruling("2022-09-09", "In a Two-Headed Giant game, Blight Pile causes the opposing team to lose X life twice.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Olivier Bernard"
        flavorText = "\"Keep your distance. It may be broken, but it's far from benign.\"\n—Stenn"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dcc2699-03e6-47d1-b976-1453fe9d27b7.jpg?1783921336"
    }
}
