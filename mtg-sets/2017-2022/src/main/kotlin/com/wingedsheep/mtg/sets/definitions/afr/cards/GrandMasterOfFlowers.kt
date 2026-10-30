package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.Duration

/**
 * Grand Master of Flowers — Adventures in the Forgotten Realms #17
 * {2}{W}{W} · Legendary Planeswalker — Bahamut · Loyalty 3
 *
 * As long as Grand Master of Flowers has seven or more loyalty counters on him, he's a 7/7 Dragon
 * God creature with flying and indestructible.
 * +1: Target creature without first strike, double strike, or vigilance can't attack or block
 * until your next turn.
 * +1: Search your library and/or graveyard for a card named Monk of the Open Hand, reveal it, and
 * put it into your hand. If you search your library this way, shuffle.
 *
 * The animation is type-*setting* (ruling 2021-07-23): while animated he is not a planeswalker, so
 * he can't be attacked and damage marks on him instead of removing loyalty — hence the static also
 * removes PLANESWALKER (as Kaito, Bane of Nightmares does). It's one multi-layer static gated on the
 * loyalty count; his loyalty abilities stay activatable because that gate keys off the ability, not
 * his current type. Paying a +1 cost from six loyalty makes him a creature before anyone can respond.
 */
val GrandMasterOfFlowers = card("Grand Master of Flowers") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Bahamut"
    startingLoyalty = 3
    oracleText = "As long as Grand Master of Flowers has seven or more loyalty counters on him, he's a 7/7 " +
        "Dragon God creature with flying and indestructible.\n" +
        "+1: Target creature without first strike, double strike, or vigilance can't attack or block " +
        "until your next turn.\n" +
        "+1: Search your library and/or graveyard for a card named Monk of the Open Hand, reveal it, " +
        "and put it into your hand. If you search your library this way, shuffle."

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.LOYALTY, 7)
        ability = CompositeStaticAbility(
            listOf(
                GrantCardType("CREATURE", GroupFilter.source()),
                RemoveCardType("PLANESWALKER", GroupFilter.source()),
                GrantSubtype("Dragon", GroupFilter.source()),
                GrantSubtype("God", GroupFilter.source()),
                SetBasePowerToughnessStatic(7, 7, GroupFilter.source()),
                GrantKeyword(Keyword.FLYING, GroupFilter.source()),
                GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            )
        )
    }

    loyaltyAbility(+1) {
        val creature = target(
            TargetFilter.Creature
                .withoutKeyword(Keyword.FIRST_STRIKE)
                .withoutKeyword(Keyword.DOUBLE_STRIKE)
                .withoutKeyword(Keyword.VIGILANCE)
        )
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn)
    }

    loyaltyAbility(+1) {
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Monk of the Open Hand"),
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ca4ffff-404e-4e8e-8fd6-4e64d4828fc7.jpg?1783926531"
        ruling(
            "2021-07-23",
            "While Grand Master of Flowers is a creature, he is not a planeswalker. He cannot be attacked " +
                "and damage dealt to him while he is a creature will not reduce the number of loyalty counters " +
                "he has. However, you may still activate his loyalty abilities as normal."
        )
        ruling(
            "2021-07-23",
            "You can activate the last ability of Grand Master of Flowers even if there isn't a card named " +
                "Monk of the Open Hand in your library or graveyard."
        )
        ruling(
            "2021-07-23",
            "The first ability of Grand Master of Flowers doesn't use the stack and can't be responded to. " +
                "Similarly, players can't respond to paying the cost of adding loyalty counters for an ability. " +
                "Therefore, if Grand Master of Flowers has six loyalty and you activate one of his abilities, he " +
                "will be a 7/7 Dragon God with flying and indestructible before players can respond to the " +
                "loyalty ability."
        )
        ruling(
            "2021-07-23",
            "If Grand Master of Flowers somehow gains enough loyalty to become a creature while it is being " +
                "attacked, it is removed from combat. If blockers haven't been declared yet, it may then block. " +
                "It may even block creatures that were previously attacking it. Creatures that were previously " +
                "attacking it that go unblocked won't deal combat damage."
        )
    }
}
