package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Mardu Devotee
 * {W}
 * Creature — Human Scout
 * 1/2
 *
 * When this creature enters, scry 2.
 * {1}: Add {R}, {W}, or {B}. Activate only once each turn.
 */
val MarduDevotee = card("Mardu Devotee") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, scry 2. (Look at the top two cards of your library, " +
        "then put any number of them on the bottom and the rest on top in any order.)\n" +
        "{1}: Add {R}, {W}, or {B}. Activate only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(2)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.RED, Color.WHITE, Color.BLACK))
        )
        description = "{1}: Add {R}, {W}, or {B}. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Lorenzo Mastroianni"
        flavorText = "Eskeg and his companion stood watch, the sights of the steppe a constant inspiration."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da45e9b0-a4f6-413b-9e62-666c511eb5b0.jpg?1743204014"
    }
}
