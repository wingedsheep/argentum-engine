package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sarinth Greatwurm
 * {4}{R}{G}
 * Creature — Wurm
 * 7/6
 * Trample
 * Whenever a land enters, create a tapped Powerstone token.
 *
 * "A land" is any player's land (see ruling), so the trigger filter is the bare `Land`, as on
 * Ankh of Mishra — not landfall's `youControl()`.
 */
val SarinthGreatwurm = card("Sarinth Greatwurm") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 6
    oracleText = "Trample\nWhenever a land enters, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land).enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Jesper Ejsing"
        flavorText = "It felt the rumblings of war and uncoiled itself from the depths of the world."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaee6fcb-23de-4347-84b6-1e9849a537fc.jpg?1783920026"

        ruling(
            "2022-10-14",
            "Sarinth Greatwurm's second ability triggers whenever a land enters the battlefield under " +
                "any player's control, not just under your control."
        )
    }
}
