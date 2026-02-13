package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Righteous Cause
 * {3}{W}{W}
 * Enchantment
 * Whenever a creature attacks, you gain 1 life.
 */
val RighteousCause = card("Righteous Cause") {
    manaCost = "{3}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature attacks, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.AnyAttacks
        effect = GainLifeEffect(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Scott M. Fischer"
        flavorText = "\"Until the world unites in vengeful fury and Phage is destroyed, I will not stay my hand.\" —Akroma, angelic avenger"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b83c6245-4b37-430d-af10-2581804fff08.jpg"
    }
}
