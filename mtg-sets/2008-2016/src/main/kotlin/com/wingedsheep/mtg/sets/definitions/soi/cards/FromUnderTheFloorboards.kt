package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * From Under the Floorboards (Shadows over Innistrad #111)
 * {3}{B}{B}
 * Sorcery
 *
 * Create three tapped 2/2 black Zombie creature tokens and you gain 3 life. If this spell's
 * madness cost was paid, instead create X of those tokens and you gain X life.
 * Madness {X}{B}{B}
 *
 * The X lives only in the madness cost: the madness cast announces it, and the resolving spell
 * reads it as [DynamicAmounts.xValue]. [Conditions.MadnessCostWasPaid] (CR 702.35) picks the X
 * branch; a cast from hand makes three tokens and gains 3 life.
 */
val FromUnderTheFloorboards = card("From Under the Floorboards") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Create three tapped 2/2 black Zombie creature tokens and you gain 3 life. " +
        "If this spell's madness cost was paid, instead create X of those tokens and you gain X life.\n" +
        "Madness {X}{B}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    fun zombiesAndLife(amount: DynamicAmount) = Effects.CreateToken(
        count = amount,
        power = 2,
        toughness = 2,
        colors = setOf(Color.BLACK),
        creatureTypes = setOf("Zombie"),
        tapped = true,
    ) then Effects.GainLife(amount)

    spell {
        effect = Effects.If(
            Conditions.MadnessCostWasPaid,
            then = zombiesAndLife(DynamicAmounts.xValue()),
            otherwise = zombiesAndLife(DynamicAmounts.fixed(3)),
        )
    }

    madness("{X}{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e644706-223d-4e56-9614-b224e281be2f.jpg?1783937776"
    }
}
