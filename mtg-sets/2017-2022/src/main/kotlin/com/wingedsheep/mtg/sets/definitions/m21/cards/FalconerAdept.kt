package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Falconer Adept
 * {3}{W}
 * Creature — Human Soldier
 * 2/3
 *
 * Whenever this creature attacks, create a 1/1 white Bird creature token with flying that's
 * tapped and attacking.
 *
 * The bare "tapped and attacking" lets the token's controller pick what it attacks as it enters
 * (CR 508.4; 2020-06-23 ruling), so the token uses [EnteringAttackDefender.CONTROLLER_CHOOSES].
 */
val FalconerAdept = card("Falconer Adept") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature attacks, create a 1/1 white Bird creature token with flying " +
        "that's tapped and attacking."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/2/2/22e33dff-23d5-4f31-8dd6-270657e150d8.jpg?1783930594"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Billy Christian"
        flavorText = "\"My falcons are family, as much as any other member of my clan.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb4733e6-6fe2-4460-ac9f-82feb583d790.jpg?1783930741"
        ruling("2020-06-23", "You choose which player or planeswalker the Bird token is attacking. It doesn't have to be attacking the same player or planeswalker that Falconer Adept is attacking.")
        ruling("2020-06-23", "Although the Bird is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when it enters the battlefield attacking.")
    }
}
