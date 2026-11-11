package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zendikar Resurgent
 * {5}{G}{G}
 * Enchantment
 * Whenever you tap a land for mana, add one mana of any type that land produced.
 * Whenever you cast a creature spell, draw a card.
 *
 * The mana line is Mirari's Wake's: [AdditionalManaOnSourceTap] with `color = null` mirrors the
 * produced type, narrowed to lands you control.
 */
val ZendikarResurgent = card("Zendikar Resurgent") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever you tap a land for mana, add one mana of any type that land produced. " +
        "(The types of mana are white, blue, black, red, green, and colorless.)\n" +
        "Whenever you cast a creature spell, draw a card."

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.youControl(),
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67f02bc3-b565-4a38-8313-eb47ff31db8d.jpg?1783937898"
        ruling("2016-01-22", "If the land's mana ability produces more than one type of mana, choose one of those types to determine what mana Zendikar Resurgent produces.")
        ruling("2016-01-22", "The last ability will resolve before the creature spell that caused it to trigger.")
    }
}
