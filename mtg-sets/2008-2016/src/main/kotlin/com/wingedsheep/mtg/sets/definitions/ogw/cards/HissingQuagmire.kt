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
 * Hissing Quagmire
 * Land
 * This land enters tapped.
 * {T}: Add {B} or {G}.
 * {1}{B}{G}: Until end of turn, this land becomes a 2/2 black and green Elemental creature
 * with deathtouch. It's still a land.
 */
val HissingQuagmire = card("Hissing Quagmire") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B} or {G}.\n" +
        "{1}{B}{G}: Until end of turn, this land becomes a 2/2 black and green Elemental creature " +
        "with deathtouch. It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.BLACK.name, Color.GREEN.name),
            keywords = setOf(Keyword.DEATHTOUCH),
            duration = Duration.EndOfTurn,
        )
        description = "{1}{B}{G}: Until end of turn, this land becomes a 2/2 black and green Elemental creature with deathtouch. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ef11b54-205a-48e9-92d0-5d416be869ef.jpg?1783937892"

        ruling("2016-01-22", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature nor when it entered the battlefield.")
        ruling("2016-01-22", "This land is colorless until the last ability gives it colors.")
        ruling("2016-01-22", "An ability that turns a land into a creature also sets that creature's power and toughness. If the land was already a creature (for example, if it was the target of a spell with awaken), this will overwrite the previous effect that set its power and toughness. Effects that modify its power or toughness will continue to apply no matter when they started to take effect. The same is true for counters that change its power or toughness (such as +1/+1 counters) and effects that switch its power and toughness.")
    }
}
