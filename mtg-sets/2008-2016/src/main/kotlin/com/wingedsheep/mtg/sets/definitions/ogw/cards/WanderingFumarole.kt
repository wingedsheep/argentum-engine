package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wandering Fumarole
 * Land
 * This land enters tapped.
 * {T}: Add {U} or {R}.
 * {2}{U}{R}: Until end of turn, this land becomes a 1/4 blue and red Elemental creature with
 * "{0}: Switch this creature's power and toughness until end of turn." It's still a land.
 *
 * [Effects.BecomeCreature] then [Effects.GrantActivatedAbility] until end of turn (Den of the
 * Bugbear's shape, with an activated rather than a triggered ability). The switch lives in its own
 * P/T sublayer after the animation's set-P/T, so it holds even if the land is re-animated later.
 */
val WanderingFumarole = card("Wandering Fumarole") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {U} or {R}.\n" +
        "{2}{U}{R}: Until end of turn, this land becomes a 1/4 blue and red Elemental creature with " +
        "\"{0}: Switch this creature's power and toughness until end of turn.\" It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{R}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 1,
            toughness = 4,
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.BLUE.name, Color.RED.name),
            duration = Duration.EndOfTurn,
        ) then Effects.GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{0}")
                effect = Effects.SwitchPowerToughness(EffectTarget.Self)
                description = "{0}: Switch this creature's power and toughness until end of turn."
            },
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "{2}{U}{R}: Until end of turn, this land becomes a 1/4 blue and red Elemental creature with " +
            "\"{0}: Switch this creature's power and toughness until end of turn.\" It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd0ee251-6c79-499b-8366-c281c989b061.jpg?1783937890"

        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature may become lethal if you switch its power and toughness during that turn.")
        ruling("2021-03-19", "Switching a creature's power and toughness twice (or any even number of times) effectively returns the creature to the power and toughness it had before any switches.")
        ruling("2016-01-22", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature nor when it entered the battlefield.")
        ruling("2016-01-22", "An ability that turns a land into a creature also sets that creature's power and toughness. If the land was already a creature (for example, if it was the target of a spell with awaken), this will overwrite the previous effect that set its power and toughness. Effects that modify its power or toughness will continue to apply no matter when they started to take effect. The same is true for counters that change its power or toughness (such as +1/+1 counters) and effects that switch its power and toughness. For example, if Wandering Fumarole has been made a 0/0 creature with three +1/+1 counters on it, activating its last ability will turn it into a 4/7 creature that's still a land.")
        ruling("2016-01-22", "This land is colorless until the last ability gives it colors.")
    }
}
