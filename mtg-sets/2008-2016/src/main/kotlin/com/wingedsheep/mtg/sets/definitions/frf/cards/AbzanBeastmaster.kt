package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Abzan Beastmaster — Fate Reforged #119
 * {2}{G} · Creature — Dog Shaman 2/1
 *
 * At the beginning of your upkeep, draw a card if you control the creature with the greatest
 * toughness or tied for the greatest toughness.
 *
 * The "if" trails the effect, so this is not an intervening-if: the ability always triggers and
 * the check happens only as it resolves (ruling). "You control the creature with the greatest
 * toughness or tied for it" is: you control a creature, and the greatest toughness among creatures
 * you control is at least the greatest among all creatures on the battlefield. The existence check
 * keeps an empty board from counting as a 0-vs-0 tie. Toughness is read from projected state.
 */
val AbzanBeastmaster = card("Abzan Beastmaster") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Shaman"
    power = 2
    toughness = 1
    oracleText = "At the beginning of your upkeep, draw a card if you control the creature with the " +
        "greatest toughness or tied for the greatest toughness."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.All(
                Conditions.YouControl(GameObjectFilter.Creature),
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness(),
                    ComparisonOperator.GTE,
                    DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature).maxToughness()
                )
            ),
            Effects.DrawCards(1)
        )
        description = "At the beginning of your upkeep, draw a card if you control the creature with the " +
            "greatest toughness or tied for the greatest toughness."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Winona Nelson"
        flavorText = "His beasts move the great siege towers of the Abzan across the endless sands."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/219c4ab2-1f7c-469b-a72a-d435213f130c.jpg?1783938683"
        ruling("2014-11-24", "The ability checks the creatures on the battlefield as it resolves to determine if you will draw a card.")
        ruling("2014-11-24", "You'll draw a maximum of one card, even if you control multiple creatures tied for the greatest toughness.")
    }
}
