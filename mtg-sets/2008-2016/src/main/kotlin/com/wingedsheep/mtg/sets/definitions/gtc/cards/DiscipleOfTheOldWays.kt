package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disciple of the Old Ways
 * {1}{G}
 * Creature — Human Warrior
 * 2/2
 * {R}: This creature gains first strike until end of turn.
 */
val DiscipleOfTheOldWays = card("Disciple of the Old Ways") {
    manaCost = "{1}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Warrior"
    oracleText = "{R}: This creature gains first strike until end of turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Anthony Palumbo"
        flavorText = "\"Selesnya thinks nature is a pretty plaything. True nature would rip their faces off and wear their skins as trophies.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c62b3ee-db2b-45c3-87d5-5d917ea4baeb.jpg"
    }
}
