package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Arcades, the Strategist
 * {1}{G}{W}{U}
 * Legendary Creature — Elder Dragon
 * 3/5
 * Flying, vigilance
 * Whenever a creature you control with defender enters, draw a card.
 * Each creature you control with defender assigns combat damage equal to its toughness rather
 * than its power and can attack as though it didn't have defender.
 *
 * Both halves of the last ability are battlefield-scoped statics gated on the creature having
 * defender in projected state, so a creature that loses defender stops benefiting.
 */
private val defenderYouControl = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER).youControl()

val ArcadesTheStrategist = card("Arcades, the Strategist") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 3
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "Whenever a creature you control with defender enters, draw a card.\n" +
        "Each creature you control with defender assigns combat damage equal to its toughness rather than its power " +
        "and can attack as though it didn't have defender."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(defenderYouControl).enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter(defenderYouControl),
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    staticAbility {
        ability = CanAttackDespiteDefender(filter = GroupFilter(defenderYouControl))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e90c638-d4b2-4243-bbc4-1cc10516c40f.jpg?1783934524"
        ruling(
            "2021-03-19",
            "Arcades's last ability doesn't actually change any creature's power. It changes only the amount of " +
                "combat damage it assigns. All other rules and effects that check power or toughness use the real values.",
        )
        ruling(
            "2021-03-19",
            "If Arcades leaves the battlefield after a creature with defender has attacked, that creature remains " +
                "an attacking creature, although it will assign damage equal to its power.",
        )
    }
}
