package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * The Seriema
 * {1}{W}{W}
 * Legendary Artifact — Spacecraft
 * When The Seriema enters, search your library for a legendary creature card, reveal it,
 * put it into your hand, then shuffle.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)
 * 7+ | Flying
 * Other tapped legendary creatures you control have indestructible.
 * 5/5
 */
val TheSeriema = card("The Seriema") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 5
    toughness = 5
    oracleText = "When The Seriema enters, search your library for a legendary creature card, reveal it, put it into your hand, then shuffle.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)\n" +
        "7+ | Flying\n" +
        "Other tapped legendary creatures you control have indestructible."

    // When The Seriema enters, search your library for a legendary creature card, reveal it,
    // put it into your hand, then shuffle.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Creature.legendary(),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
        description = "When The Seriema enters, search your library for a legendary creature card, reveal it, put it into your hand, then shuffle."
    }

    // Station activated ability: tap another creature → add charge counters equal to its power.
    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Creature,
            excludeSelf = true
        )
        effect = Effects.AddDynamicCounters(
            counterType = Counters.CHARGE,
            amount = DynamicAmount.EntityProperty(
                entity = EntityReference.TappedAsCost(),
                numericProperty = EntityNumericProperty.Power
            ),
            target = EffectTarget.Self
        )
        timing = TimingRule.SorcerySpeed
    }

    val charge7 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(7)
    )

    // 7+ charge counters: becomes an artifact creature.
    staticAbility {
        condition = charge7
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // 7+ charge counters: Flying.
    staticAbility {
        condition = charge7
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    // Other tapped legendary creatures you control have indestructible.
    staticAbility {
        ability = GrantKeyword(
            Keyword.INDESTRUCTIBLE,
            GroupFilter(
                GameObjectFilter.Creature.legendary().youControl().tapped(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dec91ec3-42d3-4922-96f0-dbb50a576084.jpg?1755341202"
    }
}
