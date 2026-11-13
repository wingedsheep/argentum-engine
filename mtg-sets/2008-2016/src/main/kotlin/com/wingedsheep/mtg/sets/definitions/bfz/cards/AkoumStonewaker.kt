package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Akoum Stonewaker — Battle for Zendikar #140
 * {1}{R} · Creature — Human Shaman · 2/1
 * Landfall — Whenever a land you control enters, you may pay {2}{R}. If you do, create a 3/1 red
 * Elemental creature token with trample and haste. Exile that token at the beginning of the next
 * end step.
 *
 * The optional payment is Wharf Infiltrator's mana `MayPay` gate; the token is Chandra,
 * Flamecaller's 3/1 Elemental (plus trample) exiled by `exileAtStep = Step.END`.
 */
val AkoumStonewaker = card("Akoum Stonewaker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, you may pay {2}{R}. If you do, create " +
        "a 3/1 red Elemental creature token with trample and haste. Exile that token at the beginning " +
        "of the next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.MayPay(
            ManaCost.parse("{2}{R}"),
            Effects.CreateToken(
                power = 3,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
                exileAtStep = Step.END,
                imageUri = "https://cards.scryfall.io/normal/front/1/0/10cb10f5-e22a-4ccd-a1b0-c6f245b15f64.jpg?1783938120",
            ),
        )
        description = "Landfall — Whenever a land you control enters, you may pay {2}{R}. If you do, " +
            "create a 3/1 red Elemental creature token with trample and haste. Exile that token at the " +
            "beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c92ab7b5-6a29-463e-b294-3cb0efaccddb.jpg?1783938195"
    }
}
