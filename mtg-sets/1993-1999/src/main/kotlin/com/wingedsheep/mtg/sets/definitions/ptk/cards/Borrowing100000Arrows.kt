package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Borrowing 100,000 Arrows
 * {2}{U}
 * Sorcery
 * Draw a card for each tapped creature target opponent controls.
 *
 * The count is a [DynamicAmount.AggregateBattlefield] over [Player.TargetOpponent] and
 * `GameObjectFilter.Creature.tapped()` — the amount reads the targeted opponent's board on
 * resolution, so the draw is never a snapshot taken at cast. The [TargetOpponent] requirement is
 * what binds that player; the [Effects.DrawCards] itself is the ordinary controller-facing draw.
 */
val Borrowing100000Arrows = card("Borrowing 100,000 Arrows") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each tapped creature target opponent controls."

    spell {
        target("target", TargetOpponent())
        effect = Effects.DrawCards(
            DynamicAmount.AggregateBattlefield(
                Player.TargetOpponent,
                GameObjectFilter.Creature.tapped()
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Song Shikai"
        flavorText = "Kongming and Lu Su tricked Wei troops into shooting over 100,000 arrows at them to later use against the Wei at Red Cliffs."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96b5362a-bbca-4dc3-a320-3664609fe169.jpg"
    }
}
