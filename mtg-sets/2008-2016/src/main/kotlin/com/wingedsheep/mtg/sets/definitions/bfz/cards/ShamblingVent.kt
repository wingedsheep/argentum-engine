package com.wingedsheep.mtg.sets.definitions.bfz.cards

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
 * Shambling Vent
 * Land
 * This land enters tapped.
 * {T}: Add {W} or {B}.
 * {1}{W}{B}: Until end of turn, this land becomes a 2/3 white and black Elemental creature with lifelink. It's still a land.
 */
val ShamblingVent = card("Shambling Vent") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {W} or {B}.\n" +
        "{1}{W}{B}: Until end of turn, this land becomes a 2/3 white and black Elemental creature " +
        "with lifelink. It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}{B}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 3,
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.WHITE.name, Color.BLACK.name),
            keywords = setOf(Keyword.LIFELINK),
            duration = Duration.EndOfTurn,
        )
        description = "{1}{W}{B}: Until end of turn, this land becomes a 2/3 white and black Elemental creature with lifelink. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30a73816-1f33-4798-a9d6-5f0410d19625.jpg?1783938172"

        ruling("2015-08-25", "Multiple instances of lifelink on the same creature are redundant. Activating the last ability more than once won't cause you to gain additional life if Shambling Vent deals damage.")
        ruling("2015-08-25", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature nor when it entered the battlefield.")
        ruling("2015-08-25", "This land is colorless until the last ability gives it colors.")
        ruling("2015-08-25", "When a land becomes a creature, that doesn't count as having a creature enter the battlefield. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters the battlefield won't trigger.")
        ruling("2015-08-25", "An ability that turns a land into a creature also sets that creature's power and toughness. If the land was already a creature (for example, if it was the target of a spell with awaken), this will overwrite the previous effect that set its power and toughness. Effects that modify its power or toughness will continue to apply no matter when they started to take effect. The same is true for counters that change its power or toughness (such as +1/+1 counters) and effects that switch its power and toughness. For example, if Shambling Vent has been made a 0/0 creature with three +1/+1 counters on it, activating its last ability will turn it into a 5/6 creature that's still a land.")
    }
}
