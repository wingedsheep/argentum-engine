package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Maw of Kozilek
 * {3}{R}
 * Creature — Eldrazi Drone
 * 2/5
 * Devoid (This card has no color.)
 * {C}: This creature gets +2/-2 until end of turn. ({C} represents colorless mana.)
 */
val MawofKozilek = card("Maw of Kozilek") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "{C}: This creature gets +2/-2 until end of turn. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{C}")
        effect = Effects.ModifyStats(2, -2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Daarken"
        flavorText = "\"The Eldrazi make worse than ruins—they make a world where not even ruins stand.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48669993-ad68-499e-8f2e-bc5c651b4b28.jpg"
    }
}
