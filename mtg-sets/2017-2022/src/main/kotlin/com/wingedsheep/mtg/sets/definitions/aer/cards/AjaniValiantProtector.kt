package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Valiant Protector — Aether Revolt #185 (Planeswalker Deck)
 * {4}{G}{W} · Legendary Planeswalker — Ajani · Loyalty 4
 *
 * +2: Put two +1/+1 counters on up to one target creature.
 * +1: Reveal cards from the top of your library until you reveal a creature card. Put that card
 * into your hand and the rest on the bottom of your library in a random order.
 * −11: Put X +1/+1 counters on target creature, where X is your life total. That creature gains
 * trample until end of turn.
 *
 * The +2 is Ajani, Caller of the Pride's "up to one target" shape: an optional target iterated with
 * ForEachTarget, so the ability can be activated with no target. The +1 is Spinner of Souls' reveal
 * — with no creature card in the library the whole library is revealed and goes to the bottom in a
 * random order, which is the ruling's "reveal your library and then randomize it". The −11 reads
 * the life total as it resolves (second ruling).
 */
val AjaniValiantProtector = card("Ajani, Valiant Protector") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+2: Put two +1/+1 counters on up to one target creature.\n" +
        "+1: Reveal cards from the top of your library until you reveal a creature card. Put that card " +
        "into your hand and the rest on the bottom of your library in a random order.\n" +
        "−11: Put X +1/+1 counters on target creature, where X is your life total. That creature gains " +
        "trample until end of turn."

    loyaltyAbility(+2) {
        target(TargetFilter.Creature, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.ContextTarget(0))
        )
    }

    loyaltyAbility(+1) {
        effect = Patterns.Library.revealUntilMatchToHand(filter = GameObjectFilter.Creature)
    }

    loyaltyAbility(-11) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.yourLifeTotal(),
            creature
        ) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "185"
        inBooster = false
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/791fdd9a-0ab6-4db9-84f9-859d2d862518.jpg?1783936716"
        ruling(
            "2017-02-09",
            "If you don't have a creature card in your library while resolving Ajani's second ability, " +
                "you'll reveal your library and then randomize it."
        )
        ruling(
            "2017-02-09",
            "The value of X for Ajani's third ability is determined only as it resolves. The number of " +
                "+1/+1 counters on the creature won't change later if your life total changes."
        )
    }
}
