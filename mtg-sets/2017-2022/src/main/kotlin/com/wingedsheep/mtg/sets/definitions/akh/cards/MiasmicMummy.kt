package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Miasmic Mummy
 * {1}{B}
 * Creature — Zombie Jackal
 * 2/2
 * When this creature enters, each player discards a card.
 *
 * An enters trigger running [Effects.EachPlayerDiscards] — the same each-player discard as
 * Enemy of Enlightenment's upkeep trigger.
 */
val MiasmicMummy = card("Miasmic Mummy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Jackal"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, each player discards a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.EachPlayerDiscards(1)
        description = "When this creature enters, each player discards a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "E. M. Gist"
        flavorText = "Its breath doesn't assault the senses. It pollutes the mind."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47ef9e66-678c-4e8d-9ca7-3f2b72b9468c.jpg?1783936502"
        ruling(
            "2017-04-18",
            "When Miasmic Mummy's triggered ability resolves, first the player whose turn it is chooses a card " +
                "to discard, then each other player in turn order chooses a card to discard, then all of those " +
                "cards are discarded simultaneously. No one sees what the other players are discarding before " +
                "deciding which card to discard."
        )
    }
}
