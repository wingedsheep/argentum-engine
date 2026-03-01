package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mardu Hateblade
 * {W}
 * Creature — Human Warrior
 * 1/1
 * {B}: This creature gains deathtouch until end of turn.
 */
val MarduHateblade = card("Mardu Hateblade") {
    manaCost = "{W}"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "{B}: This creature gains deathtouch until end of turn."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Ryan Alexander Lee"
        flavorText = "\"There may be little honor in my tactics, but there is no honor in losing.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1d1cdfa-834c-4028-8036-c4bfb7da071b.jpg?1562795845"
    }
}
