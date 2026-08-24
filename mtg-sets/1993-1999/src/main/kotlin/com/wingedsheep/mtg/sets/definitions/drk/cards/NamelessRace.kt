package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRunEffect
import com.wingedsheep.sdk.scripting.effects.PayAnyAmountOfLifeAsEntersEffect
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Nameless Race
 * {3}{B}
 * Creature — (no subtype)
 * &#42;/&#42;
 * Trample
 * As this creature enters, pay any amount of life. The amount you pay can't be more than the total
 * number of white nontoken permanents your opponents control plus the total number of white cards
 * in their graveyards.
 * Nameless Race's power and toughness are each equal to the life paid as it entered.
 *
 * Two halves that have to agree across a boundary the engine usually doesn't cross: a choice made
 * during resolution, read back by a characteristic-defining ability during *layer projection*, for
 * as long as the permanent lives.
 *
 * The pipeline's own variables (`VariableReference`) die with the resolution that set them, and a
 * counter would be the wrong shape — counters are visible, removable game state, while this is a
 * fixed fact about how the permanent entered. So the amount is stamped on the permanent as its own
 * component and read back through
 * [EntityNumericProperty.ValueChosenAsEntered], which the projector evaluates like any other
 * per-entity property. A Race that dies and is reanimated re-chooses, because the stamp goes with
 * the old object (CR 400.7).
 *
 * The printed ceiling is a sum of two counts, which `DynamicAmount.Add` already expresses. The
 * engine bounds the choice by the controller's life total as well — not printed, but a player can't
 * pay life they don't have.
 */
val NamelessRace = card("Nameless Race") {
    manaCost = "{3}{B}"
    typeLine = "Creature"
    oracleText = "Trample\nAs this creature enters, pay any amount of life. The amount you pay " +
        "can't be more than the total number of white nontoken permanents your opponents control " +
        "plus the total number of white cards in their graveyards.\nNameless Race's power and " +
        "toughness are each equal to the life paid as it entered."

    keywords(Keyword.TRAMPLE)

    dynamicStats(
        DynamicAmount.EntityProperty(
            EntityReference.Source,
            EntityNumericProperty.ValueChosenAsEntered
        )
    )

    replacementEffect(
        OnEnterRunEffect(
            PayAnyAmountOfLifeAsEntersEffect(
                maxAmount = DynamicAmount.Add(
                    DynamicAmount.Count(
                        player = Player.EachOpponent,
                        zone = Zone.BATTLEFIELD,
                        filter = GameObjectFilter.Permanent.withColor(Color.WHITE).nontoken(),
                    ),
                    DynamicAmount.Count(
                        player = Player.EachOpponent,
                        zone = Zone.GRAVEYARD,
                        filter = GameObjectFilter.Any.withColor(Color.WHITE),
                    ),
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/348a467a-4661-4fdb-af1d-9171a1a930d9.jpg?1783947938"
    }
}
