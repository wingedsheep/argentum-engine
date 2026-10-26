package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kindred Discovery — Commander 2017 #11 (canonical printing)
 * {3}{U}{U} · Enchantment
 *
 * As this enchantment enters, choose a creature type.
 * Whenever a creature you control of the chosen type enters or attacks, draw a card.
 *
 * "Enters or attacks" is one triggered ability, so it is a single [Triggers.or] over the two
 * ANY-bound atoms rather than two abilities. Both read the creature type chosen on this
 * enchantment through `withChosenSubtype()`; with no type chosen, nothing matches (ruling).
 */
val KindredDiscovery = card("Kindred Discovery") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Whenever a creature you control of the chosen type enters or attacks, draw a card."

    replacementEffect(EntersWithChoice(choiceType = ChoiceType.CREATURE_TYPE))

    triggeredAbility {
        val chosenType = GameObjectFilter.Creature.youControl().withChosenSubtype()
        trigger = Triggers.or(
            Triggers.a(chosenType).enters(),
            Triggers.a(chosenType).attacks(),
        )
        effect = Effects.DrawCards(1)
        description = "Whenever a creature you control of the chosen type enters or attacks, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Lake Hurwitz"
        flavorText = "Few things are truly \"lost\" at sea."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed07b7be-bd3d-4dbc-bc92-b5478d3604c6.jpg?1783935949"
        ruling("2022-06-10", "The choice of creature type is made as Kindred Discovery enters the battlefield. Players can't take any actions between the time the choice is made and the time it enters the battlefield. Notably, this means that if it is entering the battlefield at the same time as any creatures of the chosen type, its last ability will trigger.")
        ruling("2017-08-25", "If you somehow control a Kindred Discovery with no chosen creature type, its last ability can't trigger, even if a creature with no creature types enters the battlefield or attacks.")
    }
}
