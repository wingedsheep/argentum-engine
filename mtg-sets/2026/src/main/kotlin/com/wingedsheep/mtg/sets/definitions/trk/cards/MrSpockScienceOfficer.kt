package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mr. Spock, Science Officer
 * {2}{U}{U}
 * Legendary Creature — Vulcan Human Scientist
 * 2/4
 *
 * If you would draw a card, draw two cards instead.
 *
 * Teferi's Ageless Insight without the draw-step exemption: `ReplaceDrawWith(draw 2)` on every card
 * you draw, the first one in your draw step included.
 */
val MrSpockScienceOfficer = card("Mr. Spock, Science Officer") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Vulcan Human Scientist"
    oracleText = "If you would draw a card, draw two cards instead."
    power = 2
    toughness = 4

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.DrawCards(2),
            appliesTo = EventPattern.DrawEvent(player = Player.You),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "74"
        artist = "Colin Boyer"
        flavorText = "\"Insufficient facts always invites danger.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d19fc127-669e-4264-935a-e5991e6c0964.jpg?1792508192"
    }
}
