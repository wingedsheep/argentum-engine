package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kamahl's Summons
 * {3}{G}
 * Sorcery
 * Each player may reveal any number of creature cards from their hand. Then each
 * player creates a 2/2 green Bear creature token for each card they revealed this way.
 */
val KamahlsSummons = card("Kamahl's Summons") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Each player may reveal any number of creature cards from their hand. Then each player creates a 2/2 green Bear creature token for each card they revealed this way."

    spell {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = Effects.PipelineSteps {
                val creatures = gather(
                    CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                    name = "creatures"
                )
                chooseAnyNumber(
                    from = creatures,
                    prompt = "You may reveal any number of creature cards from your hand",
                    name = "revealed"
                )
                run(
                    CreateTokenEffect(
                        count = DynamicAmount.VariableReference("revealed_count"),
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.GREEN),
                        creatureTypes = setOf("Bear"),
                        imageUri = "https://cards.scryfall.io/normal/front/7/7/772dac39-269b-4a35-aad3-320279af833f.jpg?1675455454"
                    )
                )
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "Anthony S. Waters"
        flavorText = "As Krosa unleashed the peace in Kamahl, he unleashed the fury in Krosa."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0edc37c6-b6a8-424f-95dd-928d03c28542.jpg?1562897867"
    }
}
