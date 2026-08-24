package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Revered Elder
 * {2}{W}
 * Creature — Human Cleric
 * 1 / 2
 *
 * Samite Healer's shield pointed inward: "dealt to this creature" is
 * [EffectTarget.Self], so the ability needs no target requirement at all.
 */
val ReveredElder = card("Revered Elder") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "{1}: Prevent the next 1 damage that would be dealt to this creature this turn."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventNextDamage(1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Donato Giancola"
        flavorText = "The Cho-Arrim worship life and revere those who've experienced it longest."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0793175-e56b-4ff8-9e22-3a96a698068c.jpg"
    }
}
