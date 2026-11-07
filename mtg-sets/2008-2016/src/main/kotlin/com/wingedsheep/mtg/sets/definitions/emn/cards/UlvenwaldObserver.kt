package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ulvenwald Observer (Eldritch Moon #176)
 * {4}{G}{G}
 * Creature — Treefolk
 * 6/6
 *
 * Whenever a creature you control with toughness 4 or greater dies, draw a card.
 *
 * Sultai Flayer shape. The toughness check is a leaves-the-battlefield trigger condition, so it
 * reads the creature's last-known toughness (pumps included) off the zone-change event.
 */
val UlvenwaldObserver = card("Ulvenwald Observer") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 6
    toughness = 6
    oracleText = "Whenever a creature you control with toughness 4 or greater dies, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().toughnessAtLeast(4)).dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Jaime Jones"
        flavorText = "\"I tell you, the trees were closing in! It's by the grace of the Heron alone that I " +
            "made it out of the Ulvenwald!\"\n—Ricton, traveling merchant"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a9d23d5-2f94-48e6-824f-f1de8f742989.jpg"
    }
}
