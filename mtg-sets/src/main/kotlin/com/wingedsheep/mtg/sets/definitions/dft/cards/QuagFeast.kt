package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Quag Feast
 * {1}{B}
 * Sorcery
 *
 * Choose target creature, planeswalker, or Vehicle. Mill two cards, then destroy the chosen
 * permanent if its mana value is less than or equal to the number of cards in your graveyard.
 *
 * Mill resolves first, then a [ConditionalEffect] re-reads the (now larger) graveyard: a
 * [Compare] of the chosen target's mana value against the count of cards in your graveyard,
 * destroying it only when the threshold holds.
 */
val QuagFeast = card("Quag Feast") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose target creature, planeswalker, or Vehicle. Mill two cards, then destroy " +
        "the chosen permanent if its mana value is less than or equal to the number of cards in " +
        "your graveyard."

    spell {
        target(
            "creature, planeswalker, or Vehicle",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.CreatureOrVehicle or GameObjectFilter.Planeswalker),
            ),
        )
        effect = Patterns.Library.mill(2).then(
            ConditionalEffect(
                condition = Compare(
                    left = DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.ManaValue,
                    ),
                    operator = ComparisonOperator.LTE,
                    right = DynamicAmount.Count(Player.You, Zone.GRAVEYARD),
                ),
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Loïc Canavaggia"
        flavorText = "Speedbrood racers are gregarious while not racing, but everyone must eat."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd8b6033-63e6-484e-8efb-a4eb9ca59fbf.jpg?1782687880"
    }
}
