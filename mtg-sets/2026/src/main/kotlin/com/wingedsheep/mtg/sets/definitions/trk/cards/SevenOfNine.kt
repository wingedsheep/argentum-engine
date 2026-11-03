package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Seven of Nine
 * {2}{U}{B}
 * Legendary Artifact Creature — Human Scientist
 * 2/4
 *
 * Whenever you face a dilemma, draw a card. (You face a dilemma as you choose one or more modes for
 * a spell or ability.)
 */
val SevenOfNine = card("Seven of Nine") {
    manaCost = "{2}{U}{B}"
    typeLine = "Legendary Artifact Creature — Human Scientist"
    colorIdentity = "UB"
    oracleText = "Whenever you face a dilemma, draw a card. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Bastien L. Deharme"
        flavorText = "\"I am no longer Borg, but the prospect of becoming human is . . . unsettling. " +
            "I don't know where I belong.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6204cfdb-6fb4-4030-96b6-9306c8266bcf.jpg?1793538768"
    }
}
