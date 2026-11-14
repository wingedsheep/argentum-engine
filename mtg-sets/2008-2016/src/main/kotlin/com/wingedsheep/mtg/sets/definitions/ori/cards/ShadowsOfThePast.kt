package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadows of the Past
 * {1}{B}
 * Enchantment
 * Whenever a creature dies, scry 1.
 * {4}{B}: Each opponent loses 2 life and you gain 2 life. Activate only if there are four or more
 * creature cards in your graveyard.
 */
val ShadowsOfThePast = card("Shadows of the Past") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature dies, scry 1. (Look at the top card of your library. You may put " +
        "that card on the bottom.)\n" +
        "{4}{B}: Each opponent loses 2 life and you gain 2 life. Activate only if there are four or more " +
        "creature cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.CreatureCardsInGraveyardAtLeast(4))
        )
        description = "{4}{B}: Each opponent loses 2 life and you gain 2 life. Activate only if there are " +
            "four or more creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02962013-fc5a-46be-9021-8fd40ab3027c.jpg?1783938337"
        ruling("2015-06-22", "Once you legally activate the last ability, it doesn't matter how many creature cards are in your graveyard as it resolves.")
    }
}
