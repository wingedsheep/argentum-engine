package com.wingedsheep.mtg.sets.definitions.kld.cards

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
 * Metalwork Colossus — Kaladesh #222 (canonical printing)
 * {11} · Artifact Creature — Construct · 10/10
 *
 * This spell costs {X} less to cast, where X is the total mana value of noncreature artifacts you
 * control.
 * Sacrifice two artifacts: Return this card from your graveyard to your hand.
 *
 * Earthquake Dragon's shape: [SpellCostTarget.SelfCast] + [CostModification.ReduceGenericBy] over
 * [CostReductionSource.TotalPropertyAmongPermanentsYouControl] summing mana value of noncreature
 * artifacts (read from projected state, so an animated Vehicle or crewed artifact stops counting).
 * The cost is all generic, so 11 or more total mana value makes it free. The graveyard ability is
 * the `activateFromZone = GRAVEYARD` return-to-hand shape with a two-artifact sacrifice cost.
 */
val MetalworkColossus = card("Metalwork Colossus") {
    manaCost = "{11}"
    typeLine = "Artifact Creature — Construct"
    power = 10
    toughness = 10
    oracleText = "This spell costs {X} less to cast, where X is the total mana value of noncreature " +
        "artifacts you control.\n" +
        "Sacrifice two artifacts: Return this card from your graveyard to your hand."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.TotalPropertyAmongPermanentsYouControl(
                    property = EntityNumericProperty.ManaValue,
                    filter = GameObjectFilter.Artifact.notCreature()
                )
            ),
        )
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Artifact)
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        activateFromZone = Zone.GRAVEYARD
        description = "Return this card from your graveyard to your hand"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/474480b5-c60b-4c7f-9d3e-751bca43d074.jpg?1783937152"
        ruling("2016-09-20", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-09-20", "The total cost to cast Metalwork Colossus is locked in before you pay that cost. For example, if you control three noncreature artifacts each with mana value 2, including one you can sacrifice to add {C}, the total cost of Metalwork Colossus is {5}. Then you can sacrifice the artifact when you activate mana abilities just before paying the cost.")
        ruling("2016-09-20", "If the total mana value of noncreature artifacts you control is 11 or greater, Metalwork Colossus costs {0} to cast.")
        ruling("2016-09-20", "Metalwork Colossus's last ability can be activated only if it is in your graveyard.")
    }
}
