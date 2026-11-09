package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fleeting Memories (SOI #62)
 * {2}{U}
 * Enchantment
 *
 * When this enchantment enters, investigate.
 * Whenever you sacrifice a Clue, target player mills three cards.
 *
 * "Sacrifice a Clue" is the per-permanent template (`Triggers.you.sacrifices`, CR 603.2c) keyed on
 * the Clue artifact subtype, so any Clue artifact sacrificed for any reason counts, and cracking two
 * Clues at once triggers twice. A Clue sacrificed to pay a cost triggers this as the spell or ability
 * goes on the stack, so the mill resolves first.
 */
val FleetingMemories = card("Fleeting Memories") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, target player mills three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate()
        description = "When this enchantment enters, investigate."
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Clue"))
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
        description = "Whenever you sacrifice a Clue, target player mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b9eaf90-2a9e-44a7-bdae-03c26f9d21e7.jpg?1783937799"

        ruling(
            "2016-04-08",
            "If you sacrifice a Clue to pay the cost of a spell or ability, Fleeting Memories's second " +
                "ability will resolve before that spell or ability."
        )
        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact token."
        )
    }
}
