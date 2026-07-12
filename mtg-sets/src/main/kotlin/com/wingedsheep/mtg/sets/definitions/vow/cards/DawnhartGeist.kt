package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect

/**
 * Dawnhart Geist
 * {1}{W}
 * Creature — Spirit Warlock
 * 1/3
 * Whenever you cast an enchantment spell, you gain 2 life.
 */
val DawnhartGeist = card("Dawnhart Geist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Warlock"
    oracleText = "Whenever you cast an enchantment spell, you gain 2 life."
    power = 1
    toughness = 3
    triggeredAbility {
        trigger = Triggers.YouCastEnchantment
        effect = GainLifeEffect(2)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Artur Treffner"
        flavorText = "\"Our bodies may lie slain, but perhaps we can still complete the ritual. I'd rather not spend my afterlife in this wretched eternal night.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1d27617-2d3d-44a1-b93f-0694253b6774.jpg?1782703191"
    }
}
