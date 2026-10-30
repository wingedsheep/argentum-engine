package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sram, Senior Edificer
 * {1}{W}
 * Legendary Creature — Dwarf Advisor
 * 2/2
 *
 * Whenever you cast an Aura, Equipment, or Vehicle spell, draw a card.
 */
val SramSeniorEdificer = card("Sram, Senior Edificer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Advisor"
    oracleText = "Whenever you cast an Aura, Equipment, or Vehicle spell, draw a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT) or
                GameObjectFilter.Any.withSubtype(Subtype.VEHICLE)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Chris Rahn"
        flavorText = "\"As this conflict grows, it gets harder to prevent the city from breaking down. How much longer can we keep this machinery balanced?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b323e2c-59dd-4d70-9a48-b10f807bb818.jpg?1783936777"
        ruling("2025-06-06", "Sram's ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
