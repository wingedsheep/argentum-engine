package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Countless Gears Renegade
 * {1}{W}
 * Creature — Dwarf Artificer
 * 2/2
 *
 * Revolt — When this creature enters, if a permanent left the battlefield under your control
 * this turn, create a 1/1 colorless Servo artifact creature token.
 *
 * Revolt is a CR 603.4 intervening-if, checked when the creature enters and again on
 * resolution, so it rides on `interveningIf` with the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker.
 *
 * Aether Revolt is the earliest printing; the only later one is Kaladesh Remastered (KLR),
 * which isn't scaffolded.
 */
val CountlessGearsRenegade = card("Countless Gears Renegade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    oracleText = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, create a 1/1 colorless Servo artifact creature token."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true
        )
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, create a 1/1 colorless Servo artifact creature token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Dan Murayama Scott"
        flavorText = "\"Go forth and cause trouble, my little one!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2d6f334-0040-42b1-9c72-362e4dcaa65e.jpg?1783936781"
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. Tokens that leave the battlefield will satisfy a revolt ability; paying {E} won't.")
    }
}
