package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thassa's Devourer
 * {4}{U}
 * Enchantment Creature — Elemental
 * 2/6
 *
 * Constellation — Whenever this creature or another enchantment you control enters, target player mills two cards.
 */
val ThassasDevourer = card("Thassa's Devourer") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Elemental"
    power = 2
    toughness = 6
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, target player mills two cards."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target player mills two cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "David Palumbo"
        flavorText = "When mortals claim there are places Thassa cannot reach, the sea god laughs."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4ebd8c0-bb03-434d-b669-d9b35d07b2c6.jpg?1783939442"
    }
}
