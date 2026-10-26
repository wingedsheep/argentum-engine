package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Earthquake Dragon — Commander Legends: Battle for Baldur's Gate #228
 * {14}{G} · Creature — Elemental Dragon · 10/10
 *
 * This spell costs {X} less to cast, where X is the total mana value of Dragons you control.
 * Flying, trample
 * {2}{G}, Sacrifice a land: Return this card from your graveyard to your hand.
 *
 * The Ghalta / The Lord of the Eagles rail: [SpellCostTarget.SelfCast] +
 * [CostModification.ReduceGenericBy] sourced from
 * [CostReductionSource.TotalPropertyAmongPermanentsYouControl] over Dragon permanents by mana
 * value. The reduction only eats generic mana, so the floor is {G}. The graveyard ability is
 * World Breaker's shape (`activateFromZone = GRAVEYARD`).
 */
val EarthquakeDragon = card("Earthquake Dragon") {
    manaCost = "{14}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Dragon"
    power = 10
    toughness = 10
    oracleText = "This spell costs {X} less to cast, where X is the total mana value of Dragons you control.\n" +
        "Flying, trample\n" +
        "{2}{G}, Sacrifice a land: Return this card from your graveyard to your hand."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.TotalPropertyAmongPermanentsYouControl(
                    property = EntityNumericProperty.ManaValue,
                    filter = GameObjectFilter.Permanent.withSubtype("Dragon")
                )
            ),
        )
    }

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        activateFromZone = Zone.GRAVEYARD
        description = "Return this card from your graveyard to your hand"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfedfd30-8075-429e-a0fa-4919920c8632.jpg?1783922715"
        ruling("2022-06-10", "If a permanent has {X} in its mana cost, X is 0 when calculating its mana value.")
        ruling("2022-06-10", "The total cost to cast Earthquake Dragon is locked in before you pay that cost. For example, if you control three Dragons, each with mana value 4, including one you can sacrifice to add {C}, the total cost of Earthquake Dragon is {2}{G}. Then you can sacrifice the Dragon as you activate mana abilities just before paying the cost.")
        ruling("2022-06-10", "If the total mana value of Dragons you control is 14 or greater, Earthquake Dragon costs {G} to cast.")
        ruling("2022-06-10", "Earthquake Dragon's last ability can be activated only if it is in your graveyard.")
    }
}
