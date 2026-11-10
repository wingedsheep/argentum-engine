package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Inexorable Blob (Shadows over Innistrad #212)
 * {2}{G}
 * Creature — Ooze
 * 3/3
 *
 * Delirium — Whenever this creature attacks, if there are four or more card types among cards in
 * your graveyard, create a 3/3 green Ooze creature token that's tapped and attacking.
 *
 * Delirium is an intervening "if" clause: checked when the attack trigger would fire and again on
 * resolution. The token is put onto the battlefield attacking but was never declared as an attacker,
 * and its controller picks which defender it attacks (CR 508.4) — not necessarily the Blob's.
 */
val InexorableBlob = card("Inexorable Blob") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    power = 3
    toughness = 3
    oracleText = "Delirium — Whenever this creature attacks, if there are four or more card types " +
        "among cards in your graveyard, create a 3/3 green Ooze creature token that's tapped and attacking."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.Delirium()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/6/2/623a7905-3a10-4d23-96fa-4960cac8f4e3.jpg?1783937681"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0210ff44-19d9-4d24-9d32-f1f9f219d9e0.jpg?1783937727"
        ruling(
            "2016-04-08",
            "You declare which player or planeswalker the token is attacking as you put it onto the " +
                "battlefield. It doesn't have to be the same player or planeswalker Inexorable Blob is attacking."
        )
        ruling(
            "2016-04-08",
            "Although the token is attacking, it was never declared as an attacking creature (for purposes " +
                "of abilities that trigger whenever a creature attacks, for example)."
        )
        ruling(
            "2016-04-08",
            "If an effect gives the token vigilance, the token will still enter the battlefield tapped."
        )
    }
}
