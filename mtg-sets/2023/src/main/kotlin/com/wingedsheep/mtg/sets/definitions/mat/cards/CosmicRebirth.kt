package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cosmic Rebirth
 * {1}{G}{W}
 * Instant
 * Choose target permanent card in your graveyard. If it has mana value 3 or less, you may put it
 * onto the battlefield. If you don't put it onto the battlefield, put it into your hand.
 * You gain 3 life.
 *
 * The mana-value test is a resolution-time [Effects.If] over the targeted card; only when it
 * passes is the player asked ([Effects.May]). Both the "declined" branch and the "mana value 4 or
 * greater" branch put the card into its owner's hand, per the printed ruling. The life gain
 * happens either way.
 */
val CosmicRebirth = card("Cosmic Rebirth") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Choose target permanent card in your graveyard. If it has mana value 3 or less, " +
        "you may put it onto the battlefield. If you don't put it onto the battlefield, put it " +
        "into your hand.\nYou gain 3 life."

    spell {
        val chosen = target(TargetFilter.PermanentInYourGraveyard)
        val toHand = Effects.Move(chosen, Zone.HAND, fromZone = Zone.GRAVEYARD)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaValueOf(chosen),
                ComparisonOperator.LTE,
                3,
            ),
            then = Effects.May(
                Effects.Move(chosen, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
                otherwise = toHand,
                prompt = "Put the card onto the battlefield? (If you don't, put it into your hand.)",
            ),
            otherwise = toHand,
        ) then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Marta Nael"
        flavorText = "\"When all the stories find their ends, a new one shall begin.\"\n—The Endsaga"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fcb225c-d18f-4b84-b2fc-91ed84ec30aa.jpg?1783916516"
        ruling(
            "2023-05-12",
            "You put the card into your hand if you didn't put it onto the battlefield because you " +
                "chose not to or because its mana value was 4 or greater."
        )
    }
}
