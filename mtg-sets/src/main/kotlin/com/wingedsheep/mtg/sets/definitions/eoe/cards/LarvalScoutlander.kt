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
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Larval Scoutlander
 * {2}{G}
 * Artifact — Spacecraft
 * 3/3
 *
 * When this Spacecraft enters, you may sacrifice a land or Lander. If you do, search your library
 * for up to two basic land cards, put them onto the battlefield tapped, then shuffle.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)
 * 7+ | Flying
 */
val LarvalScoutlander = card("Larval Scoutlander") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Spacecraft"
    power = 3
    toughness = 3
    oracleText = "When this Spacecraft enters, you may sacrifice a land or Lander. If you do, search " +
        "your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)\n7+ | Flying"

    // When this Spacecraft enters, you may sacrifice a land or Lander. If you do, search your
    // library for up to two basic land cards, put them onto the battlefield tapped, then shuffle.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.Sacrifice(
                GameObjectFilter.Land.or(GameObjectFilter.Permanent.withSubtype("Lander")),
                count = 1,
                target = EffectTarget.Controller
            ).then(
                LibraryPatterns.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 2,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                )
            )
        )
        description = "When this Spacecraft enters, you may sacrifice a land or Lander. If you do, " +
            "search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."
    }

    // Station activated ability: tap another creature → add charge counters equal to its power
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

    // Conditional type change: artifact creature at 7+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(7)
        )
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // Conditional keyword: flying at 7+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(7)
        )
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6083f43-58dc-46fc-aeff-347b1080417b.jpg?1755341414"
    }
}
