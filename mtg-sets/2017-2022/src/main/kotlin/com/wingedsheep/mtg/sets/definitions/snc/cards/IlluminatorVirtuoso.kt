package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Illuminator Virtuoso
 * {1}{W}
 * Creature — Human Rogue
 * 1 / 1
 * Double strike
 * Whenever this creature becomes the target of a spell you control, it connives.
 */
val IlluminatorVirtuoso = card("Illuminator Virtuoso") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rogue"
    oracleText = "Double strike\n" +
        "Whenever this creature becomes the target of a spell you control, it connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 1
    toughness = 1

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byYou = true, spellsOnly = true)
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "John Stanko"
        flavorText = "The Obscura reserve their keenest blades for traitors in their midst."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52ba20d9-9423-4114-8734-1dac307483c8.jpg"
    }
}
