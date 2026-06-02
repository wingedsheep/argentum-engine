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
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Fell Gravship
 * {2}{B}
 * Artifact — Spacecraft
 * When this Spacecraft enters, mill three cards, then return a creature or Spacecraft card from your graveyard to your hand.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 8+ | Flying, lifelink
 * 3/2
 */
val FellGravship = card("Fell Gravship") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Spacecraft"
    power = 3
    toughness = 2
    oracleText = "When this Spacecraft enters, mill three cards, then return a creature or Spacecraft card from your graveyard to your hand.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n8+ | Flying, lifelink"

    // ETB: Mill 3 cards, then return a creature or Spacecraft card from graveyard to hand
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                // Mill 3 cards
                LibraryPatterns.mill(3),
                // Return a creature or Spacecraft card from graveyard to hand
                Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                                player = com.wingedsheep.sdk.scripting.references.Player.You,
                                filter = GameObjectFilter(
                                    cardPredicates = listOf(
                                        CardPredicate.Or(listOf(
                                            CardPredicate.IsCreature,
                                            CardPredicate.HasSubtype(Subtype("Spacecraft"))
                                        ))
                                    )
                                )
                            ),
                            storeAs = "creatureOrSpacecraftCards"
                        ),
                        SelectFromCollectionEffect(
                            from = "creatureOrSpacecraftCards",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            storeSelected = "chosen",
                            prompt = "Return a creature or Spacecraft card from your graveyard to your hand"
                        ),
                        MoveCollectionEffect(
                            from = "chosen",
                            destination = CardDestination.ToZone(com.wingedsheep.sdk.core.Zone.HAND)
                        )
                    )
                )
            )
        )
        description = "When this Spacecraft enters, mill three cards, then return a creature or Spacecraft card from your graveyard to your hand."
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

    // 8+ charge counters: Flying and lifelink
    val charge8 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(8)
    )

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.LIFELINK.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e94b130d-3547-43c5-a319-5ebc571c2e2d.jpg?1755341397"
    }
}
