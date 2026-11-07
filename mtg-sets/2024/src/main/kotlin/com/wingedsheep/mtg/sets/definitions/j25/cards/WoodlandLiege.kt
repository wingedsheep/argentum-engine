package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Woodland Liege — Foundations Jumpstart #27
 * {2}{G} · Creature — Elf Druid Noble · 2/2
 *
 * Whenever a Beast you control enters, draw a card.
 *
 * A bare tribal noun is a permanent filter, so a Beast that isn't a creature still counts.
 */
val WoodlandLiege = card("Woodland Liege") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid Noble"
    power = 2
    toughness = 2
    oracleText = "Whenever a Beast you control enters, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.BEAST).youControl()).enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Marta Nael"
        flavorText = "\"Your human laws mean nothing here. Plead your case before the tribunal of the wilds.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4369919-780f-460e-bbac-db3bba05e01f.jpg?1783908862"
    }
}
