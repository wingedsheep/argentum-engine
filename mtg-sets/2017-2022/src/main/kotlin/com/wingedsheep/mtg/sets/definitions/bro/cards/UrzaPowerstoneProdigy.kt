package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Urza, Powerstone Prodigy — The Brothers' War #69
 * {2}{U} · Legendary Creature — Human Artificer · 1/3
 *
 * Vigilance
 * {1}, {T}: Draw a card, then discard a card.
 * Whenever you discard one or more artifact cards, create a tapped Powerstone token. This ability
 * triggers only once each turn.
 *
 * Mirror of Mishra, Excavation Prodigy: "one or more artifact cards" is a batch (CR 603.2c), so
 * `discards(Artifact, batch = true)` fires once per discard event holding at least one artifact card,
 * and "triggers only once each turn" is the `oncePerTurn` firing cap. It fires on any discard, not
 * just Urza's own loot.
 */
val UrzaPowerstoneProdigy = card("Urza, Powerstone Prodigy") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 3
    oracleText = "Vigilance\n" +
        "{1}, {T}: Draw a card, then discard a card.\n" +
        "Whenever you discard one or more artifact cards, create a tapped Powerstone token. This " +
        "ability triggers only once each turn. (The token is an artifact with \"{T}: Add {C}. This " +
        "mana can't be spent to cast a nonartifact spell.\")"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Hand.loot()
        description = "{1}, {T}: Draw a card, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Artifact, batch = true)
        oncePerTurn = true
        effect = Effects.CreatePowerstone(tapped = true)
        description = "Whenever you discard one or more artifact cards, create a tapped Powerstone " +
            "token. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/445f897e-a059-4cd5-bfd2-302dc7a4c8e2.jpg?1783920102"
    }
}
