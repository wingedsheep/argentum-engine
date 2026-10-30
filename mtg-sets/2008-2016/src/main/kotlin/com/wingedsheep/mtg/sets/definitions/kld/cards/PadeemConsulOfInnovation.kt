package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Padeem, Consul of Innovation — Kaladesh #59
 * {3}{U} · Legendary Creature — Vedalken Artificer 1/4
 *
 * Artifacts you control have hexproof.
 * At the beginning of your upkeep, if you control the artifact with the greatest mana value or
 * tied for the greatest mana value, draw a card.
 *
 * "You control the artifact with the greatest mana value or tied for it" is: you control an
 * artifact, and the greatest mana value among artifacts you control is at least the greatest among
 * all artifacts on the battlefield. The existence check keeps an empty artifact board from tying a
 * 0-mana-value opposing artifact. It is an intervening-if (CR 603.4), checked as the upkeep begins
 * and again on resolution (rulings).
 */
val PadeemConsulOfInnovation = card("Padeem, Consul of Innovation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Vedalken Artificer"
    power = 1
    toughness = 4
    oracleText = "Artifacts you control have hexproof. (They can't be the targets of spells or " +
        "abilities your opponents control.)\n" +
        "At the beginning of your upkeep, if you control the artifact with the greatest mana value " +
        "or tied for the greatest mana value, draw a card."

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter(GameObjectFilter.Artifact.youControl()))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.All(
            Conditions.YouControl(GameObjectFilter.Artifact),
            Conditions.CompareAmounts(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).maxManaValue(),
                ComparisonOperator.GTE,
                DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Artifact).maxManaValue()
            )
        )
        effect = Effects.DrawCards(1)
        description = "At the beginning of your upkeep, if you control the artifact with the greatest " +
            "mana value or tied for the greatest mana value, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Matt Stewart"
        flavorText = "\"Impress me.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e31b30a7-13e8-408e-a758-60e6e9290808.jpg?1783937217"
        ruling("2016-09-20", "The artifact you control has to have the highest mana value only among artifacts on the battlefield, not among all permanents on the battlefield.")
        ruling("2016-09-20", "If an artifact has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-09-20", "If you don't control an artifact with the highest mana value as your upkeep begins, Padeem's second ability won't trigger. You can't cast an instant to destroy an opponent's artifact during your turn before your upkeep begins.")
        ruling("2016-09-20", "If you no longer control an artifact with the highest mana value as Padeem's triggered ability resolves, you don't draw a card.")
    }
}
