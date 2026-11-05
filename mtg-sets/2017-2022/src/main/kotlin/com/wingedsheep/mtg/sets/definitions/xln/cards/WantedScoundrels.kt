package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wanted Scoundrels
 * {1}{B}
 * Creature — Human Pirate
 * 4/3
 *
 * When this creature dies, target opponent creates two Treasure tokens.
 */
val WantedScoundrels = card("Wanted Scoundrels") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    oracleText = "When this creature dies, target opponent creates two Treasure tokens. (They're artifacts " +
        "with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateTreasure(2, controller = opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Volkan Baǵa"
        flavorText = "\"Trust me. The reward's not worth it.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6704e4c9-3b5e-4ba5-a5bc-6d684e2983e9.jpg?1783935750"
    }
}
