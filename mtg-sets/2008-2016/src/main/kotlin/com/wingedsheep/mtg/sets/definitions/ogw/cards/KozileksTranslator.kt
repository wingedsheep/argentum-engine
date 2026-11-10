package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Kozilek's Translator
 * {4}{B}
 * Creature — Eldrazi Drone
 * 3/5
 * Devoid (This card has no color.)
 * Pay 1 life: Add {C}. Activate only once each turn. ({C} represents colorless mana.)
 */
val KozileksTranslator = card("Kozilek's Translator") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "Pay 1 life: Add {C}. Activate only once each turn. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.PayLife(1)
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Izzy"
        flavorText = "From nothingness, sustenance."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d80665ae-fcb2-48fd-95be-8768293bcef7.jpg"
    }
}
