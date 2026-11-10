package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relentless Hunter
 * {1}{R}{G}
 * Creature — Human Warrior
 * 3/3
 * {1}{R}{G}: This creature gets +1/+1 and gains trample until end of turn.
 */
val RelentlessHunter = card("Relentless Hunter") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "{1}{R}{G}: This creature gets +1/+1 and gains trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{R}{G}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Matt Stewart"
        flavorText = "\"I swore to take one head for each member of my family they killed. My task is nowhere near its end.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91a52252-46b6-42e5-8cb5-c4f823cd0bf5.jpg"
    }
}
