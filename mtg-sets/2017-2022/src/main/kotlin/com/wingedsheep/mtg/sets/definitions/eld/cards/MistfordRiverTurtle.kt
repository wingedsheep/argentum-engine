package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Mistford River Turtle
 * {3}{U}
 * Creature — Turtle
 * 1/5
 * Whenever this creature attacks, another target attacking non-Human creature can't be blocked
 * this turn.
 *
 * Same shape as Clammy Prowler, with the attacking-creature target narrowed to non-Humans.
 */
val MistfordRiverTurtle = card("Mistford River Turtle") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Turtle"
    oracleText = "Whenever this creature attacks, another target attacking non-Human creature can't be blocked this turn."
    power = 1
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(
            TargetOther(
                baseRequirement = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Creature.attacking().notSubtype(Subtype.HUMAN))
                )
            )
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Milivoj Ćeran"
        flavorText = "The fae raised the turtle from a tiny hatchling. They taught it whom to ferry—and whom to drown."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c78c386-c64b-4fab-a718-f18b46360e20.jpg?1783932653"
    }
}
