package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deem Worthy
 * {4}{R}
 * Instant
 * Deem Worthy deals 7 damage to target creature.
 * Cycling {3}{R}
 * When you cycle this card, you may have it deal 2 damage to target creature.
 */
val DeemWorthy = card("Deem Worthy") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Deem Worthy deals 7 damage to target creature.\n" +
        "Cycling {3}{R} ({3}{R}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, you may have it deal 2 damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(7, creature)
    }

    keywordAbility(KeywordAbility.cycling("{3}{R}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        val creature = target(TargetFilter.Creature)
        effect = Effects.May(Effects.DealDamage(2, creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99a2ed5f-62b8-4308-a656-f273f62f6ab8.jpg?1783936490"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
    }
}
