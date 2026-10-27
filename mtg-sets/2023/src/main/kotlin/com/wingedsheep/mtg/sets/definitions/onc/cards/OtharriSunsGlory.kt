package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Otharri, Suns' Glory
 * {3}{R}{W}
 * Legendary Creature — Phoenix
 * 3/3
 *
 * Flying, lifelink, haste
 * Whenever Otharri attacks, you get an experience counter. Then create a 2/2 red Rebel creature
 * token that's tapped and attacking for each experience counter you have.
 * {2}{R}{W}, Tap an untapped Rebel you control: Return this card from your graveyard to the
 * battlefield tapped.
 *
 * The experience counter is Meren of Clan Nel Toth's `AddCounters(EXPERIENCE)` on the controller;
 * the token count reads `playerCounterCount(EXPERIENCE)` after it, so the new counter is included.
 * A bare "tapped and attacking" is CR 508.4 as printed — `CONTROLLER_CHOOSES` lets each Rebel
 * attack any defending player or planeswalker (Hero of Bladehold, Caesar). The graveyard return is
 * Haunted Dead's tapped `PutOntoBattlefieldFromGraveyard` with Gangrenous Goliath's
 * tap-an-untapped-tribal-permanent cost; tapping a Rebel isn't a {T} cost, so a summoning-sick
 * Rebel can pay it.
 */
val OtharriSunsGlory = card("Otharri, Suns' Glory") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Phoenix"
    power = 3
    toughness = 3
    oracleText = "Flying, lifelink, haste\n" +
        "Whenever Otharri attacks, you get an experience counter. Then create a 2/2 red Rebel creature " +
        "token that's tapped and attacking for each experience counter you have.\n" +
        "{2}{R}{W}, Tap an untapped Rebel you control: Return this card from your graveyard to the " +
        "battlefield tapped."

    keywords(Keyword.FLYING, Keyword.LIFELINK, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddCounters(CounterType.EXPERIENCE, 1, EffectTarget.Controller) then
            Effects.CreateToken(
                count = DynamicAmounts.playerCounterCount(CounterType.EXPERIENCE),
                power = 2,
                toughness = 2,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Rebel"),
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                imageUri = "https://cards.scryfall.io/normal/front/a/4/a41eb9df-d8b4-4697-a759-886faf16754d.jpg?1783918169"
            )
        description = "Whenever Otharri attacks, you get an experience counter. Then create a 2/2 red Rebel " +
            "creature token that's tapped and attacking for each experience counter you have."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}{W}"),
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype("Rebel").untapped().youControl())
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{R}{W}, Tap an untapped Rebel you control: Return this card from your graveyard " +
            "to the battlefield tapped."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80c72839-0fa6-4b5f-83b7-6553ebf09bef.jpg?1783918163"
        ruling(
            "2023-02-04",
            "Tapping a Rebel you control is part of the cost to activate the last ability of Otharri, Suns' Glory " +
                "while it is in the graveyard. Once a player has begun to activate the ability, other players may " +
                "not respond by removing that Rebel from the battlefield to prevent it from being tapped."
        )
    }
}
