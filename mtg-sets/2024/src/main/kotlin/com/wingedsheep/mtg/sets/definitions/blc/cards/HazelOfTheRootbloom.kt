package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hazel of the Rootbloom — Bloomburrow Commander #2
 * {2}{B}{G} · Legendary Creature — Squirrel Druid · 3/5
 *
 * {T}, Pay 2 life, Tap X untapped tokens you control: Add X mana in any combination of colors.
 * At the beginning of your end step, create a token that's a copy of target token you control.
 * If that token is a Squirrel, instead create two tokens that are copies of it.
 *
 * The mana ability reads X off the [Costs.TapXPermanents] payment. The end-step trigger is one
 * token-copy effect whose count is "two if the target is a Squirrel, otherwise one", read as the
 * ability resolves (CR 608.2b — the target's current characteristics).
 */
val HazelOfTheRootbloom = card("Hazel of the Rootbloom") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Squirrel Druid"
    power = 3
    toughness = 5
    oracleText = "{T}, Pay 2 life, Tap X untapped tokens you control: Add X mana in any combination of colors.\n" +
        "At the beginning of your end step, create a token that's a copy of target token you control. " +
        "If that token is a Squirrel, instead create two tokens that are copies of it."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.PayLife(2),
            Costs.TapXPermanents(GameObjectFilter.Token)
        )
        effect = Effects.AddManaInAnyCombination(DynamicAmounts.xValue())
        manaAbility = true
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val token = target(TargetFilter(GameObjectFilter.Token.youControl()))
        effect = Effects.CreateTokenCopyOfTarget(
            target = token,
            count = DynamicAmounts.conditional(
                Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype("Squirrel"), targetIndex = 0),
                ifTrue = 2,
                ifFalse = 1
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Anna Pavleeva"
        flavorText = "\"May all your caches be full.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1269ca79-907e-4932-9e77-a9a5d2929060.jpg?1783910738"

        ruling("2024-07-26", "You can tap any untapped tokens you control, including creature tokens you control that you haven't controlled continuously since the beginning of your most recent turn, to pay the cost of Hazel of the Rootbloom's activated ability.")
        ruling("2024-07-26", "The token (or tokens) you create copies the original characteristics of the token as stated by the effect that created that token (unless that token is copying something else; see below). It doesn't copy whether that token is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2024-07-26", "If the copied token is copying something else (for example, if the copied token is one previously created by this ability), then the token enters as whatever that token copied.")
        ruling("2024-07-26", "If the copied token has {X} in its mana cost, X is 0.")
    }
}
