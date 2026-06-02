package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
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
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Specimen Freighter
 * {5}{U}
 * Artifact — Spacecraft
 * When this Spacecraft enters, return up to two target non-Spacecraft creatures to their owners' hands.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 9+.)
 * 9+ | Flying
 * Whenever this Spacecraft attacks, defending player mills four cards.
 * 4/7
 */
val SpecimenFreighter = card("Specimen Freighter") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 7
    oracleText = "When this Spacecraft enters, return up to two target non-Spacecraft creatures to their owners' hands.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 9+.)\n9+ | Flying\nWhenever this Spacecraft attacks, defending player mills four cards."

    // ETB: return up to two target non-Spacecraft creatures to their owners' hands
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to two target non-Spacecraft creatures",
            TargetCreature(
                count = 2,
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Spacecraft")))
            )
        )
        effect = ForEachTargetEffect(
            listOf(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
        )
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

    // Station threshold: 9+ charge counters
    val charge9 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(9)
    )

    // 9+ charge counters: becomes artifact creature
    staticAbility {
        condition = charge9
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // 9+ charge counters: gains Flying
    staticAbility {
        condition = charge9
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    // Whenever this Spacecraft attacks, defending player mills four cards
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = LibraryPatterns.mill(4, EffectTarget.PlayerRef(Player.Opponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b862a9f8-2361-4220-99bd-ae2530905195.jpg?1755341389"
    }
}
