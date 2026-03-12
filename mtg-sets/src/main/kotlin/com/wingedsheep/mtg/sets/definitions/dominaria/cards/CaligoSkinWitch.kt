package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Caligo Skin-Witch
 * {1}{B}
 * Creature — Human Wizard
 * 1/3
 * Kicker {3}{B}
 * When this creature enters, if it was kicked, each opponent discards two cards.
 */
val CaligoSkinWitch = card("Caligo Skin-Witch") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Kicker {3}{B}\nWhen this creature enters, if it was kicked, each opponent discards two cards."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{3}{B}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.EachOpponentDiscards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Daarken"
        flavorText = "\"Take their faces. The lips of these unbelievers may yet honor the Demonlord.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e23ee25-2005-4e81-a807-f52c0dbfb192.jpg?1562733375"
    }
}
