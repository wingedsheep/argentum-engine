package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sheoldred, Whispering One
 * {5}{B}{B}
 * Legendary Creature — Phyrexian Praetor
 * 6/6
 *
 * Swampwalk
 * At the beginning of your upkeep, return target creature card from your graveyard to the battlefield.
 * At the beginning of each opponent's upkeep, that player sacrifices a creature of their choice.
 *
 * Canonical printing: New Phyrexia, the card's earliest real printing. "That player" is the
 * opponent whose upkeep it is — [Player.TriggeringPlayer] on a step trigger.
 */
val SheoldredWhisperingOne = card("Sheoldred, Whispering One") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 6
    toughness = 6
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)\n" +
        "At the beginning of your upkeep, return target creature card from your graveyard to the battlefield.\n" +
        "At the beginning of each opponent's upkeep, that player sacrifices a creature of their choice."

    keywords(Keyword.SWAMPWALK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "73"
        artist = "Jana Schirmer & Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bb8347b-8663-40b8-bdfb-411236d2efc8.jpg?1783941310"
        ruling(
            "2017-11-17",
            "The opponent whose upkeep it is chooses a creature to sacrifice when Sheoldred's second triggered " +
                "ability resolves."
        )
    }
}
