package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thawbringer
 * {2}{G}
 * Creature — Insect Scout
 * When this creature enters or dies, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 * 4/2
 */
val Thawbringer = card("Thawbringer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Insect Scout"
    power = 4
    toughness = 2
    oracleText = "When this creature enters or dies, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    // When this creature enters, surveil 1
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    // When this creature dies, surveil 1
    triggeredAbility {
        trigger = Triggers.Dies
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Olivier Bernard"
        flavorText = "\"We are as ice,\" their mentor once told them, \"Destined to shed our rigid form and nourish the grounds with our melting.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b7f934f-8eb4-408b-a55f-245ec5cc4a8a.jpg?1752947419"
    }
}
