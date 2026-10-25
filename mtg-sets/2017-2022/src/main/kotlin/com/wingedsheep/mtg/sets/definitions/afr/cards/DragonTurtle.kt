package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Turtle
 * {1}{U}{U}
 * Creature — Dragon Turtle
 * 3/5
 * Flash
 * Drag Below — When this creature enters, tap it and up to one target creature an opponent
 * controls. They don't untap during their controllers' next untap steps.
 *
 * Each untap lock is keyed to the affected permanent's own controller. With no target chosen the
 * ability still resolves and locks Dragon Turtle alone; if the chosen target is illegal on
 * resolution the whole ability fizzles (CR 608.2b), leaving Dragon Turtle untapped.
 */
val DragonTurtle = card("Dragon Turtle") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon Turtle"
    power = 3
    toughness = 5
    oracleText = "Flash\nDrag Below — When this creature enters, tap it and up to one target creature an " +
        "opponent controls. They don't untap during their controllers' next untap steps."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Tap(EffectTarget.Self) then
            Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.Self,
                Duration.UntilAfterAffectedControllersNextUntap(),
            ) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap(),
            )
        description = "Drag Below — When this creature enters, tap it and up to one target creature an " +
            "opponent controls. They don't untap during their controllers' next untap steps."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d436ba80-386f-40ad-b0ca-a98cc50de964.jpg?1783926517"
        ruling(
            "2021-07-23",
            "If you target an opponent's creature with Dragon Turtle's Drag Below ability and that target is " +
                "illegal as the ability tries to resolve, the entire ability is removed from the stack and does " +
                "nothing. Dragon Turtle will remain untapped and will untap as normal during its controller's " +
                "next untap step."
        )
        ruling(
            "2021-07-23",
            "If, however, there are no legal targets for the Drag Below ability when Dragon Turtle enters the " +
                "battlefield (or you choose not to target anything), Dragon Turtle's ability will still resolve " +
                "and cause it to be tapped and it won't untap during its controllers next untap step."
        )
    }
}
