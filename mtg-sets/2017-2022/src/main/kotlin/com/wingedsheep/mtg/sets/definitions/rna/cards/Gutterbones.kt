package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gutterbones
 * {B}
 * Creature — Skeleton Warrior
 * 2/1
 * This creature enters tapped.
 * {1}{B}: Return this card from your graveyard to your hand. Activate only during your turn and
 * only if an opponent lost life this turn.
 */
val Gutterbones = card("Gutterbones") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    power = 2
    toughness = 1
    oracleText = "This creature enters tapped.\n" +
        "{1}{B}: Return this card from your graveyard to your hand. Activate only during your turn and only if an opponent lost life this turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.OnlyIfCondition(Conditions.OpponentLostLifeThisTurn),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"Down here, things don't stay dead.\"\n—Lavinia"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a1c710b-bd67-4174-ab02-6ae98a7575ac.jpg?1783933694"
    }
}
