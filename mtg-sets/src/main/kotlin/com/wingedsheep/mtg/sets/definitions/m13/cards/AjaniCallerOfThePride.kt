package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ajani, Caller of the Pride - {1}{W}{W}
 * Legendary Planeswalker — Ajani
 * Starting Loyalty: 4
 *
 * +1: Put a +1/+1 counter on up to one target creature.
 *
 * −3: Target creature gains flying and double strike until end of turn.
 *
 * −8: Create X 2/2 white Cat creature tokens, where X is your life total.
 *
 * The +1 is a genuinely optional target ("up to one"), so it is modelled as an
 * `optional` [TargetCreature] fanned out through [ForEachTargetEffect]: with no target
 * chosen the body simply never runs, and Ajani still gets the loyalty counter (per the
 * 2012-07-01 ruling). The −8 count is read at resolution via [DynamicAmount.YourLifeTotal],
 * matching the ruling that X is your life total *when the ability resolves*.
 */
val AjaniCallerOfThePride = card("Ajani, Caller of the Pride") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+1: Put a +1/+1 counter on up to one target creature.\n" +
        "−3: Target creature gains flying and double strike until end of turn.\n" +
        "−8: Create X 2/2 white Cat creature tokens, where X is your life total."

    // +1: Put a +1/+1 counter on up to one target creature.
    loyaltyAbility(+1) {
        target("up to one target creature", TargetCreature(count = 1, optional = true))
        effect = ForEachTargetEffect(
            listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
        )
    }

    // −3: Target creature gains flying and double strike until end of turn.
    loyaltyAbility(-3) {
        val creature = target("target creature", TargetCreature())
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.FLYING, creature),
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
        )
    }

    // −8: Create X 2/2 white Cat creature tokens, where X is your life total.
    loyaltyAbility(-8) {
        effect = Effects.CreateToken(
            count = DynamicAmount.YourLifeTotal,
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/f/9/f97868f6-a9ce-4ce9-bc3f-b535f3202602.jpg?1783940445"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e7f410a-7934-48ae-a90b-ffd096aed43d.jpg?1783940523"
        ruling(
            "2012-07-01",
            "You can activate the first ability without a target. You'll still put one loyalty " +
                "counter on Ajani when you activate the ability.",
        )
        ruling(
            "2012-07-01",
            "The number of Cat tokens you put onto the battlefield is equal to your life total " +
                "when the third ability resolves.",
        )
    }
}
