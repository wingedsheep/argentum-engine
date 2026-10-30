package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elspeth, Undaunted Hero
 * {2}{W}{W}{W}
 * Legendary Planeswalker — Elspeth
 * Loyalty 5
 * +2: Put a +1/+1 counter on each of up to two target creatures.
 * −2: Search your library and/or graveyard for a card named Sunlit Hoplite and put it onto the
 * battlefield. If you search your library this way, shuffle.
 * −8: Until end of turn, creatures you control gain flying and get +X/+X, where X is your
 * devotion to white.
 *
 * −2 is Elspeth's Devotee's [Patterns.Library.searchLibraryAndOrGraveyard] onto the battlefield
 * (mandatory search, so no "Don't search" option). −8 visits the creatures you control once
 * ([Effects.ForEachInGroup] snapshots the group at resolution — later creatures aren't affected,
 * ruling) and each gets a `ModifyStats` whose devotion amount is evaluated as the ability
 * resolves and then fixed (ruling), plus flying.
 */
val ElspethUndauntedHero = card("Elspeth, Undaunted Hero") {
    manaCost = "{2}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 5
    oracleText = "+2: Put a +1/+1 counter on each of up to two target creatures.\n" +
        "−2: Search your library and/or graveyard for a card named Sunlit Hoplite and put it onto the " +
        "battlefield. If you search your library this way, shuffle.\n" +
        "−8: Until end of turn, creatures you control gain flying and get +X/+X, where X is your " +
        "devotion to white."

    loyaltyAbility(+2) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "Put a +1/+1 counter on each of up to two target creatures."
    }

    loyaltyAbility(-2) {
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Sunlit Hoplite"),
            destination = SearchDestination.BATTLEFIELD,
        )
        description = "Search your library and/or graveyard for a card named Sunlit Hoplite and put it " +
            "onto the battlefield. If you search your library this way, shuffle."
    }

    loyaltyAbility(-8) {
        val devotion = DynamicAmounts.devotionTo(Color.WHITE)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity) then
                Effects.ModifyStats(devotion, devotion, EffectTarget.IterationEntity),
        )
        description = "Until end of turn, creatures you control gain flying and get +X/+X, where X is " +
            "your devotion to white."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "270"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf9867e5-9d69-400c-a84f-3b085a4fd510.jpg?1783931499"
        ruling(
            "2020-01-24",
            "The value of X is determined only as Elspeth's last ability resolves. Once that happens, the " +
                "value of X won't change later in the turn even if your devotion to white changes."
        )
        ruling(
            "2020-01-24",
            "Elspeth's last ability affects only creatures you control at the time it resolves. Creatures you " +
                "begin to control later in the turn won't get +X/+X or gain flying."
        )
    }
}
