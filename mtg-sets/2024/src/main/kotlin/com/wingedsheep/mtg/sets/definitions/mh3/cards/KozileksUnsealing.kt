package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kozilek's Unsealing — Modern Horizons 3 #65 (uncommon)
 * {2}{U} · Enchantment
 *
 * Devoid
 * Whenever you cast a creature spell with mana value 4, 5, or 6, create two 0/1 colorless
 * Eldrazi Spawn creature tokens with "Sacrifice this token: Add {C}."
 * Whenever you cast a creature spell with mana value 7 or greater, draw three cards.
 */
val KozileksUnsealing = card("Kozilek's Unsealing") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever you cast a creature spell with mana value 4, 5, or 6, create two 0/1 colorless " +
        "Eldrazi Spawn creature tokens with \"Sacrifice this token: Add {C}.\"\n" +
        "Whenever you cast a creature spell with mana value 7 or greater, draw three cards."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.manaValueAtLeast(4).manaValueAtMost(6))
        effect = Effects.CreateEldraziSpawn(2)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.manaValueAtLeast(7))
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b44ffe-db9e-4db5-9806-098411c9ece0.jpg?1783911288"
    }
}
