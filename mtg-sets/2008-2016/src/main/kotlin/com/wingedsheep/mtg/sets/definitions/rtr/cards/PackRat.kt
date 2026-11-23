package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pack Rat — Return to Ravnica #73
 * {1}{B} · Creature — Rat · Rare · * / *
 *
 * Pack Rat's power and toughness are each equal to the number of Rats you control.
 * {2}{B}, Discard a card: Create a token that's a copy of this creature.
 *
 * The copy carries the same characteristic-defining ability, so every Pack Rat (card or token)
 * counts every Rat you control, itself included.
 */
val PackRat = card("Pack Rat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 0
    toughness = 0
    oracleText = "Pack Rat's power and toughness are each equal to the number of Rats you control.\n" +
        "{2}{B}, Discard a card: Create a token that's a copy of this creature."

    dynamicStats(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.RAT)).count())

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.DiscardCard)
        effect = Effects.CreateTokenCopyOfSelf()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/170693f5-13db-4191-99b1-e527ffb5b88e.jpg?1783940361"
    }
}
