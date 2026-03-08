package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect

/**
 * Ashcloud Phoenix
 * {2}{R}{R}
 * Creature — Phoenix
 * 4/1
 * Flying
 * When Ashcloud Phoenix dies, return it to the battlefield face down under your control.
 * Morph {4}{R}{R}
 * When Ashcloud Phoenix is turned face up, it deals 2 damage to each player.
 */
val AshcloudPhoenix = card("Ashcloud Phoenix") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Phoenix"
    power = 4
    toughness = 1
    oracleText = "Flying\nWhen Ashcloud Phoenix dies, return it to the battlefield face down under your control.\nMorph {4}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Ashcloud Phoenix is turned face up, it deals 2 damage to each player."

    keywords(Keyword.FLYING)

    morph = "{4}{R}{R}"

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.PutOntoBattlefieldFaceDown()
    }

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = DealDamageToPlayersEffect(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "99"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a53752c-5689-4939-9a8b-bf59d88d7c6b.jpg?1562786146"
    }
}
