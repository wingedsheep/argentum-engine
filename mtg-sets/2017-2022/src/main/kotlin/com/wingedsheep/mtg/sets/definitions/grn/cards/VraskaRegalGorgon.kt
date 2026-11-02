package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, Regal Gorgon — Guilds of Ravnica #269 (canonical printing; planeswalker deck)
 * {5}{B}{G} · Legendary Planeswalker — Vraska · Loyalty 5
 *
 * +2: Put a +1/+1 counter on up to one target creature. That creature gains menace until end of turn.
 * −3: Destroy target creature.
 * −10: For each creature card in your graveyard, put a +1/+1 counter on each creature you control.
 *
 * The +2 is an optional ("up to one") target, so it can be activated with no creature around just
 * for the loyalty. The −10 puts N counters on each creature, N being the creature cards in your
 * graveyard as the ability resolves — [Effects.AddDynamicCounters] inside [Effects.ForEachInGroup];
 * putting counters doesn't move cards, so N is the same for every creature in the loop.
 */
val VraskaRegalGorgon = card("Vraska, Regal Gorgon") {
    manaCost = "{5}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 5
    oracleText = "+2: Put a +1/+1 counter on up to one target creature. That creature gains menace " +
        "until end of turn.\n" +
        "−3: Destroy target creature.\n" +
        "−10: For each creature card in your graveyard, put a +1/+1 counter on each creature you control."

    // +2: Put a +1/+1 counter on up to one target creature. That creature gains menace until end of turn.
    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.MENACE, creature)
    }

    // −3: Destroy target creature.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    // −10: For each creature card in your graveyard, put a +1/+1 counter on each creature you control.
    loyaltyAbility(-10) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.creatureCardsInYourGraveyard(),
                EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "269"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4299dbe6-c94d-4ecc-8bfd-741d2649cba1.jpg?1783934095"
    }
}
