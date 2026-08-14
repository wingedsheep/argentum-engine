package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Ultimate Green Goblin — Marvel's Spider-Man #157
 * {1}{B/R}{B/R} · Legendary Creature — Goblin Villain · 5/4
 *
 * At the beginning of your upkeep, discard a card, then create a Treasure token.
 * Mayhem {2}{B/R}
 */
val UltimateGreenGoblin = card("Ultimate Green Goblin") {
    manaCost = "{1}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Villain"
    power = 5
    toughness = 4
    oracleText = "At the beginning of your upkeep, discard a card, then create a Treasure token.\n" +
        "Mayhem {2}{B/R} (You may cast this card from your graveyard for {2}{B/R} if you discarded it " +
        "this turn. Timing rules still apply.)"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            Effects.Discard(1),
            Effects.CreateTreasure(1),
        )
    }
    mayhem("{2}{B/R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Jesper Ejsing"
        flavorText = "Osborn's lust for power was nothing short of monstrous."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e82d3f71-8404-40e7-b7fa-35713d1b384e.jpg?1783905308"
    }
}
