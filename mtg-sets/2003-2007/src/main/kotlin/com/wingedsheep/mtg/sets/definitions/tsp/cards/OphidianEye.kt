package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ophidian Eye
 * {2}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Whenever enchanted creature deals damage to an opponent, you may draw a card.
 *
 * Curiosity with flash.
 */
val OphidianEye = card("Ophidian Eye") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Enchant creature\n" +
        "Whenever enchanted creature deals damage to an opponent, you may draw a card."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(Recipient.Opponent)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Zoltan Boros & Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26836ff5-b3c3-4b10-af1e-df3658781cb2.jpg?1783943242"
    }
}
