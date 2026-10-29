package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Sakashima's Student — Planechase 2012 #24 (canonical printing)
 * {2}{U}{U} · Creature — Human Ninja · 0/0
 *
 * Ninjutsu {1}{U}
 * You may have this creature enter as a copy of any creature on the battlefield, except it's a
 * Ninja in addition to its other creature types.
 *
 * Mirrorhall Mimic's Clone shape — an optional [EntersAsCopy] with `additionalSubtypes = ["Ninja"]`
 * as the copy exception. It applies however the Student enters: cast, or put onto the battlefield
 * tapped and attacking by ninjutsu (the effect-driven entry asks the copy choice, then replays the
 * move). The attacker returned to pay ninjutsu is already in hand, so it can't be copied; declining
 * leaves a 0/0 that dies to state-based actions.
 */
val SakashimasStudent = card("Sakashima's Student") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 0
    toughness = 0
    oracleText = "Ninjutsu {1}{U} ({1}{U}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "You may have this creature enter as a copy of any creature on the battlefield, except it's a " +
        "Ninja in addition to its other creature types."

    ninjutsu("{1}{U}")

    replacementEffect(
        EntersAsCopy(
            optional = true,
            additionalSubtypes = listOf("Ninja"),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c3d4d71-c750-4146-80fb-7cfd2427c62f.jpg?1783940629"
        ruling(
            "2012-06-01",
            "Because you return the unblocked attacking creature to its owner's hand while activating the " +
                "ninjutsu ability, you can't have Sakashima's Student enter as a copy of that creature."
        )
        ruling(
            "2012-06-01",
            "Sakashima's Student copies exactly what was printed on the original creature (unless that " +
                "creature is copying something else or is a token) and it is a Ninja in addition to its " +
                "other types. It doesn't copy whether that creature is tapped or untapped, whether it has " +
                "any counters on it or any Auras and Equipment attached to it, or any non-copy effects " +
                "that have changed its power, toughness, types, color, or so on."
        )
        ruling(
            "2012-06-01",
            "You can choose not to copy anything. In that case, Sakashima's Student enters as a 0/0 " +
                "creature, and is probably put into the graveyard immediately."
        )
    }
}
