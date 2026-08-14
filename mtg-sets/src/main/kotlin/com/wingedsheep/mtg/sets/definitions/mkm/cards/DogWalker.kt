package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dog Walker — Murders at Karlov Manor #197
 * {R}{W} · Creature — Human Citizen · 3/1
 *
 * Vigilance
 * Disguise {R/W}{R/W}
 * When this creature is turned face up, create two tapped 1/1 white Dog creature tokens.
 *
 * The turned-face-up payoff is the reason to disguise this rather than cast it for {R}{W}: the
 * special action (CR 702.168d) doesn't use the stack, so the Dogs arrive at instant speed — and
 * they arrive tapped, so they're ambush blockers only from the following turn.
 *
 * The token's art comes from the MKM `tokenArt` layer, so no `imageUri` is baked in here.
 */
val DogWalker = card("Dog Walker") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Citizen"
    oracleText = "Vigilance\n" +
        "Disguise {R/W}{R/W} (You may cast this card face down for {3} as a 2/2 creature with ward {2}. " +
        "Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, create two tapped 1/1 white Dog creature tokens."
    power = 3
    toughness = 1
    keywords(Keyword.VIGILANCE)
    disguise = "{R/W}{R/W}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Dog"),
            count = 2,
            tapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e0adb7-a030-4dcc-9284-cd91c7598a22.jpg"
    }
}
