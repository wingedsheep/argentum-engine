package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Knightfisher
 * {3}{U}{U}
 * Creature — Bird Knight
 * 4/5
 *
 * Flying
 * Whenever another nontoken Bird you control enters, create a 1/1 blue Fish
 * creature token.
 */
val Knightfisher = card("Knightfisher") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Bird Knight"
    power = 4
    toughness = 5
    oracleText = "Flying\nWhenever another nontoken Bird you control enters, create a 1/1 blue Fish creature token."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsCreature,
                        CardPredicate.HasSubtype(Subtype("Bird")),
                        CardPredicate.IsNontoken
                    ),
                    controllerPredicate = com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou
                ),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = CreateTokenEffect(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Fish"),
            imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Jakob Eirich"
        flavorText = "His keen eyes can see the glint of scales far below the river's surface."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7fb7f4f-2153-4527-8f11-adbf508d3533.jpg?1721426127"
    }
}
