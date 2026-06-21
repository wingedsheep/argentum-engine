package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Entity Tracker
 * {2}{U}
 * Creature — Human Scout
 * 2/3
 *
 * Flash
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room,
 * draw a card.
 *
 * The Eerie ability word has no rules meaning — it's modeled as two triggers (enchantment-enters
 * and Room-fully-unlocked) sharing the [Effects.DrawCards] payoff, like the other DSK Eerie
 * creatures (e.g. Stalked Researcher).
 */
val EntityTracker = card("Entity Tracker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scout"
    power = 2
    toughness = 3
    oracleText = "Flash\n" +
        "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, draw a card."

    keywords(Keyword.FLASH, Keyword.EERIE)

    // Eerie trigger — part 1: whenever an enchantment you control enters
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.DrawCards(1)
        description = "Eerie — Whenever an enchantment you control enters, draw a card."
    }

    // Eerie trigger — part 2: whenever you fully unlock a Room
    triggeredAbility {
        trigger = Triggers.RoomFullyUnlocked
        effect = Effects.DrawCards(1)
        description = "Eerie — Whenever you fully unlock a Room, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Cristi Balanescu"
        flavorText = "\"The readings are off the charts! Whatever it is, it's close.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae54d697-6d06-4af1-a617-8a47a6ab9c01.jpg?1726286051"
    }
}
