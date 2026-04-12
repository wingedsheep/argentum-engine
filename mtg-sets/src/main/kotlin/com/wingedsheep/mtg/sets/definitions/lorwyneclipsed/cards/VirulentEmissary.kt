package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Virulent Emissary
 * {G}
 * Creature — Elf Assassin
 * 1/1
 *
 * Deathtouch
 * Whenever another creature you control enters, you gain 1 life.
 */
val VirulentEmissary = card("Virulent Emissary") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Assassin"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\nWhenever another creature you control enters, you gain 1 life."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Tiffany Turrill"
        flavorText = "\"I could use a knife, but I'd miss that tremble under my fingers as the poison takes hold.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0702efed-915e-466a-96bb-ac09af06b21e.jpg?1767658502"
    }
}
