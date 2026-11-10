package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek's Shrieker
 * {2}{B}
 * Creature — Eldrazi Drone
 * 3/2
 * Devoid (This card has no color.)
 * {C}: This creature gets +1/+0 and gains menace until end of turn. (It can't be blocked except by
 * two or more creatures. {C} represents colorless mana.)
 */
val KozileksShrieker = card("Kozilek's Shrieker") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{C}: This creature gets +1/+0 and gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures. {C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{C}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Jason Felix"
        flavorText = "Listen to the sound of defeat."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a384cd5b-2c6c-4969-bb62-a017e2fc9794.jpg"
    }
}
