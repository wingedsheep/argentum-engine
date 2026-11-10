package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Needle Spires
 * Land
 * This land enters tapped.
 * {T}: Add {R} or {W}.
 * {2}{R}{W}: Until end of turn, this land becomes a 2/1 red and white Elemental creature
 * with double strike. It's still a land.
 */
val NeedleSpires = card("Needle Spires") {
    typeLine = "Land"
    colorIdentity = "RW"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {R} or {W}.\n" +
        "{2}{R}{W}: Until end of turn, this land becomes a 2/1 red and white Elemental creature " +
        "with double strike. It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}{W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 1,
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.RED.name, Color.WHITE.name),
            keywords = setOf(Keyword.DOUBLE_STRIKE),
            duration = Duration.EndOfTurn,
        )
        description = "{2}{R}{W}: Until end of turn, this land becomes a 2/1 red and white Elemental creature with double strike. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d73700c-fa62-42be-aaca-5f8a50af20db.jpg?1783937892"

        ruling("2016-01-22", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature nor when it entered the battlefield.")
        ruling("2016-01-22", "This land is colorless until the last ability gives it colors.")
        ruling("2016-01-22", "When a land becomes a creature, that doesn't count as having a creature enter the battlefield. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters the battlefield won't trigger.")
        ruling("2016-01-22", "An ability that turns a land into a creature also sets that creature's power and toughness. If the land was already a creature (for example, if it was the target of a spell with awaken), this will overwrite the previous effect that set its power and toughness. Effects that modify its power or toughness will continue to apply no matter when they started to take effect. The same is true for counters that change its power or toughness (such as +1/+1 counters) and effects that switch its power and toughness. For example, if Needle Spires has been made a 0/0 creature with three +1/+1 counters on it, activating its last ability will turn it into a 5/4 creature that's still a land.")
    }
}
