package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overwhelmed Apprentice
 * {U}
 * Creature — Human Wizard
 * 1/2
 *
 * When this creature enters, each opponent mills two cards. Then you scry 2.
 *
 * "Each opponent" is the player argument to the mill recipe — one pipeline fans out over every
 * opponent's library — and the scry follows once, after all the milling, as the ruling requires.
 */
val OverwhelmedApprentice = card("Overwhelmed Apprentice") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, each opponent mills two cards. Then you scry 2. " +
        "(Look at the top two cards of your library, then put any number of them on the bottom " +
        "and the rest on top in any order.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8659092e-4fe8-42be-ab03-efc99ed37436.jpg?1783932651"
        ruling("2019-10-04", "You scry 2 once after each opponent has moved the top cards of their library. You don't scry 2 for each opponent.")
    }
}
