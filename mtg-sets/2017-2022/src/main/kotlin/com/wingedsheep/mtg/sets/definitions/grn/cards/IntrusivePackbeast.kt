package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Intrusive Packbeast
 * {4}{W}
 * Creature — Beast
 * 3/3
 * Vigilance
 * When this creature enters, tap up to two target creatures your opponents control.
 */
val IntrusivePackbeast = card("Intrusive Packbeast") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Beast"
    oracleText = "Vigilance\n" +
        "When this creature enters, tap up to two target creatures your opponents control."
    power = 3
    toughness = 3

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Jason Felix"
        flavorText = "Good at carrying things. Really good at knocking them down."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49266f3c-4b43-4175-8bac-16789ba6f4b9.jpg?1783934198"
    }
}
