package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Eumidian Terrabotanist
 * {1}{G}
 * Creature — Insect Druid
 * Landfall — Whenever a land you control enters, you gain 1 life.
 */
val EumidianTerrabotanist = card("Eumidian Terrabotanist") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Insect Druid"
    power = 2
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, you gain 1 life."

    // Landfall trigger: whenever a land you control enters, you gain 1 life
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.GainLife(1, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Loïc Canavaggia"
        flavorText = "To plant the seeds of the other is to cultivate the self.\n—Eumidian aphorism"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64fb2981-86ed-478a-89cd-c6bb078a5bc7.jpg?1753683225"
    }
}
