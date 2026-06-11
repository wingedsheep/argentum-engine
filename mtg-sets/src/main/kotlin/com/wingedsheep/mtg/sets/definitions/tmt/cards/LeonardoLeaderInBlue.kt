package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leonardo, Leader in Blue
 * {W}
 * Legendary Creature — Mutant Ninja Turtle
 * 2/1
 *
 * Sneak {3}{W}{W} (You may cast this spell for {3}{W}{W} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. He
 * enters tapped and attacking.)
 * When Leonardo enters, if his sneak cost was paid, creatures you control get
 * +2/+0 until end of turn.
 * {1}{W}: Leonardo gains first strike until end of turn.
 */
val LeonardoLeaderInBlue = card("Leonardo, Leader in Blue") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {3}{W}{W} (You may cast this spell for {3}{W}{W} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nWhen Leonardo enters, if his sneak cost was paid, creatures you control get +2/+0 until end of turn.\n{1}{W}: Leonardo gains first strike until end of turn."
    power = 2
    toughness = 1

    sneak("{3}{W}{W}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.SneakCostWasPaid
        effect = Patterns.Group.modifyStatsForAll(2, 0, Filters.Group.creaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self, Duration.EndOfTurn)
        description = "{1}{W}: Leonardo gains first strike until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6eaae35-d513-43d8-be2d-b97c15e25937.jpg?1771502519"
    }
}
