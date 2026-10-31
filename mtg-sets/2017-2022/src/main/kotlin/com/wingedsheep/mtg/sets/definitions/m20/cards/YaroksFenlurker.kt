package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yarok's Fenlurker — Core Set 2020 #123
 * {B}{B} · Creature — Horror · Uncommon
 * 1/1
 *
 * When this creature enters, each opponent exiles a card from their hand.
 * {2}{B}: This creature gets +1/+1 until end of turn.
 *
 * [Effects.EachOpponentExilesFromHand] lets each opponent pick the card from their own hand
 * (Mindleech Ghoul's shape); the firebreathing-style pump is a self-targeted [Effects.ModifyStats].
 */
val YaroksFenlurker = card("Yarok's Fenlurker") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, each opponent exiles a card from their hand.\n" +
        "{2}{B}: This creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.EachOpponentExilesFromHand(1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Daarken"
        flavorText = "\"Yarok's nightmares lurk and wait\nWhere dark despair is near at hand.\"\n" +
            "—Lament for Bala Ged, stanza 3"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16b122f8-fcd9-4b2a-92f6-f0ed7307ff58.jpg?1783932986"
    }
}
