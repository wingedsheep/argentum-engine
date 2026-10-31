package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Faerie Miscreant
 * {U}
 * Creature — Faerie Rogue
 * 1/1
 * Flying
 * When this creature enters, if you control another creature named Faerie Miscreant, draw a card.
 *
 * "another" is [Conditions.YouControlOtherAtLeast], which leaves the entering Miscreant itself out
 * of the count; the intervening "if" is checked on trigger and again on resolution.
 */
val FaerieMiscreant = card("Faerie Miscreant") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, if you control another creature named Faerie Miscreant, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlOtherAtLeast(
            1,
            GameObjectFilter.Creature.named("Faerie Miscreant"),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Steven Belledin"
        flavorText = "One faerie distracts the judge while a second steals the law."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c487f1a-a51d-449c-827d-c7b9381acb34.jpg?1783938352"
    }
}
