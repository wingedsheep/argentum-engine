package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoyaltyDamageFloor
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Deification — March of the Machine: The Aftermath #2
 * {1}{W} · Enchantment
 *
 * As this enchantment enters, choose a planeswalker type.
 * Planeswalkers you control of the chosen type have hexproof.
 * As long as you control a creature, if damage dealt to a planeswalker you control of the chosen
 * type would result in all loyalty counters on it being removed, instead all but one of those
 * counters are removed.
 *
 * The last ability is a [LoyaltyDamageFloor] of 1: the damage is still dealt in full (lifelink,
 * "deals damage" triggers), only the loyalty-counter removal stops at one counter left.
 */
val Deification = card("Deification") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a planeswalker type.\n" +
        "Planeswalkers you control of the chosen type have hexproof.\n" +
        "As long as you control a creature, if damage dealt to a planeswalker you control of the chosen type " +
        "would result in all loyalty counters on it being removed, instead all but one of those counters are removed."

    replacementEffect(EntersWithChoice(ChoiceType.PLANESWALKER_TYPE))

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Planeswalker.youControl(), chosenSubtypeKey = "chosenPlaneswalkerType")
        )
    }

    replacementEffect(
        LoyaltyDamageFloor(
            floor = 1,
            restrictions = listOf(Conditions.YouControl(GameObjectFilter.Creature)),
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Planeswalker.youControl().withChosenSubtype())
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52ce799e-cb9e-4d74-8b13-a5f4559e13a9.jpg?1783916530"
        ruling("2023-05-12", "A planeswalker with hexproof may still be attacked.")
        ruling(
            "2023-05-12",
            "Deification's last ability doesn't prevent damage. All damage is still dealt to the affected " +
                "planeswalkers, and any additional effects of that damage will still happen. For example, if a 3/3 " +
                "creature with lifelink attacks an affected planeswalker with two loyalty counters on it, 3 damage " +
                "will be dealt, causing the controller of the creature to gain 3 life and one loyalty counter to be " +
                "removed from the planeswalker."
        )
        ruling(
            "2023-05-12",
            "Deification's last ability will apply to a planeswalker dealt combat damage in the same combat " +
                "damage step that its controller's last creature is destroyed."
        )
    }
}
