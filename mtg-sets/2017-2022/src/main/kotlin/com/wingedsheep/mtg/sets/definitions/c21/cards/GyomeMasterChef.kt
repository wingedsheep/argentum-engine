package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gyome, Master Chef — Commander 2021 #5 (canonical printing)
 * {2}{B}{G} · Legendary Creature — Troll Warlock · 5/3
 *
 * Trample
 * At the beginning of your end step, create a number of Food tokens equal to the number of
 * nontoken creatures you had enter the battlefield under your control this turn.
 * {1}, Sacrifice a Food: Target creature gains indestructible until end of turn. Tap it.
 *
 * The end-step count is the nontoken-creature slice of the per-player entry log, so it counts
 * creatures that have since left, stopped being creatures or changed control, and Gyome itself on
 * the turn it entered (2021-04-16 rulings).
 */
val GyomeMasterChef = card("Gyome, Master Chef") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Troll Warlock"
    oracleText = "Trample\n" +
        "At the beginning of your end step, create a number of Food tokens equal to the number of " +
        "nontoken creatures you had enter the battlefield under your control this turn.\n" +
        "{1}, Sacrifice a Food: Target creature gains indestructible until end of turn. Tap it."
    power = 5
    toughness = 3
    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateFood(DynamicAmounts.nonTokenCreaturesEnteredThisTurn())
        description = "At the beginning of your end step, create a number of Food tokens equal to the number " +
            "of nontoken creatures you had enter the battlefield under your control this turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")))
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t) then Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dd7dd1a-6dd1-43c3-8298-7db703d384a1.jpg?1783927614"
        ruling("2021-04-16", "The second ability will count Gyome itself on the turn it enters the battlefield.")
        ruling("2021-04-16", "You can activate the last ability even if the creature is already tapped.")
        ruling("2021-04-16", "Gyome, Master Chef's second ability counts all nontoken creatures that entered the battlefield under your control this turn, even if some or all of those creatures are no longer on the battlefield, no longer creatures, or no longer under your control.")
    }
}
