package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard

/**
 * Icetill Explorer
 * {2}{G}{G}
 * Creature — Insect Scout
 * You may play an additional land on each of your turns.
 * You may play lands from your graveyard.
 * Landfall — Whenever a land you control enters, mill a card.
 * 2/4
 */
val IcetillExplorer = card("Icetill Explorer") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Scout"
    power = 2
    toughness = 4
    oracleText = "You may play an additional land on each of your turns.\nYou may play lands from your graveyard.\nLandfall — Whenever a land you control enters, mill a card."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = EffectPatterns.mill(1)
        description = "Landfall — Whenever a land you control enters, mill a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Warren Mahy"
        flavorText = "\"Come! Join me in the sun!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9482aab-6ddf-48e1-84fa-b13d5ff81e69.jpg?1752947338"
    }
}
