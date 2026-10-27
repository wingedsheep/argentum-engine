package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Skyknight Vanguard
 * {R}{W}
 * Creature — Human Knight
 * 1/2
 *
 * Flying
 * Whenever this creature attacks, create a 1/1 white Soldier creature token that's tapped and
 * attacking.
 *
 * A bare "tapped and attacking": the token's controller picks its defender as it enters
 * (CR 508.4, per the 2019-07-12 ruling) — [EnteringAttackDefender.CONTROLLER_CHOOSES].
 */
val SkyknightVanguard = card("Skyknight Vanguard") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature attacks, create a 1/1 white Soldier creature token that's tapped and attacking."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/a/4/a491d9da-a58d-4423-b565-c5a99ef63132.jpg?1783932857",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Bayard Wu"
        flavorText = "\"Jump! NOW!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/549634d8-1c33-401b-841d-a79ef4374dd1.jpg?1783932947"
        ruling(
            "2019-07-12",
            "You choose which player or planeswalker the token is attacking. It doesn't have to be attacking the same player or planeswalker that Skyknight Vanguard is attacking.",
        )
        ruling(
            "2019-07-12",
            "Although the token is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when the token enters the battlefield attacking.",
        )
    }
}
