package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stallion of Ashmouth (Shadows over Innistrad #136)
 * {3}{B}
 * Creature — Nightmare Horse
 * 3/3
 *
 * Delirium — {1}{B}: This creature gets +1/+1 until end of turn. Activate only if there are four or
 * more card types among cards in your graveyard.
 */
val StallionOfAshmouth = card("Stallion of Ashmouth") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Horse"
    power = 3
    toughness = 3
    oracleText = "Delirium — {1}{B}: This creature gets +1/+1 until end of turn. Activate only if there " +
        "are four or more card types among cards in your graveyard."

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Delirium()))
        description = "{1}{B}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Chris Rahn"
        flavorText = "Echoing hoofbeats can no longer be assumed to be help from the Gavony Riders."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b465a5d-3ce4-4c96-a3f3-4ad9716946c4.jpg"
    }
}
