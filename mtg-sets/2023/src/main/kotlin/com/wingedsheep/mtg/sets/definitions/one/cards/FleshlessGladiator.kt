package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fleshless Gladiator — Phyrexia: All Will Be One #94
 * {1}{B} · Creature — Phyrexian Skeleton · 2/2
 *
 * Corrupted — {2}{B}: Return this card from your graveyard to the battlefield tapped. You lose 1 life.
 * Activate only if an opponent has three or more poison counters.
 *
 * A graveyard-zone activated ability ([Ghoulsteed]'s shape) gated by [Conditions.Corrupted] as an
 * activation restriction. The return keeps the graveyard guard; the life loss is the controller's.
 */
val FleshlessGladiator = card("Fleshless Gladiator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Skeleton"
    power = 2
    toughness = 2
    oracleText = "Corrupted — {2}{B}: Return this card from your graveyard to the battlefield tapped. " +
        "You lose 1 life. Activate only if an opponent has three or more poison counters."

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You))
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Corrupted))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Konstantin Porubov"
        flavorText = "What she lacks in skin she makes up in swagger."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b2a32c9-f0ae-4ae4-a5c5-72bea05018fb.jpg?1783918046"
    }
}
