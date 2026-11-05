package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Desiccated Naga
 * {2}{B}
 * Creature — Zombie Snake
 * 3/2
 * {3}{B}: Target opponent loses 2 life and you gain 2 life. Activate only if you control a
 * Liliana planeswalker.
 *
 * The [com.wingedsheep.mtg.sets.definitions.m19.cards.ArisenGorgon] Liliana check, as an
 * activation restriction (the [com.wingedsheep.mtg.sets.definitions.iko.cards.BondersEnclave] shape).
 */
val DesiccatedNaga = card("Desiccated Naga") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Snake"
    oracleText = "{3}{B}: Target opponent loses 2 life and you gain 2 life. Activate only if you control a Liliana planeswalker."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Liliana"))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "276"
        artist = "Svetlin Velinov"
        flavorText = "\"The nature of this plane makes it almost too easy for me to command the dead.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21f31e96-bb66-4b28-b14a-426d1d5839a6.jpg"
    }
}
