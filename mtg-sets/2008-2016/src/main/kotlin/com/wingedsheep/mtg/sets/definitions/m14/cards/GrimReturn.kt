package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Return — Magic 2014 #100 (canonical printing)
 * {2}{B} · Instant
 *
 * Choose target creature card in a graveyard that was put there from the battlefield this turn.
 * Put that card onto the battlefield under your control.
 *
 * The target is any graveyard's creature card carrying the existing
 * `putIntoGraveyardFromBattlefieldThisTurn()` predicate (Salvager of Ruin, Supper for Spiders), so a
 * discarded, milled, or countered creature card is not a legal target (rulings 2013-07-01). The move
 * is Reanimate's: graveyard to battlefield with `controllerOverride = EffectTarget.Controller`.
 */
val GrimReturn = card("Grim Return") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose target creature card in a graveyard that was put there from the battlefield " +
        "this turn. Put that card onto the battlefield under your control."

    spell {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.putIntoGraveyardFromBattlefieldThisTurn(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Move(
            creature,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Seb McKinnon"
        flavorText = "The necromancer's touch strips away memory and loyalty, and only strength and malice remain."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15b69f74-3b54-4db4-abf3-b71db8cc9562.jpg?1783939923"
        ruling(
            "2013-07-01",
            "You can choose a creature card in any player's graveyard as long as that card was put there " +
                "from the battlefield during the turn. A creature card that was put into a graveyard from " +
                "anywhere else, such as a card that was discarded, can't be chosen as the target."
        )
        ruling(
            "2013-07-01",
            "Creature spells that are countered never entered the battlefield, so those cards can't be " +
                "chosen as the target of Grim Return."
        )
    }
}
