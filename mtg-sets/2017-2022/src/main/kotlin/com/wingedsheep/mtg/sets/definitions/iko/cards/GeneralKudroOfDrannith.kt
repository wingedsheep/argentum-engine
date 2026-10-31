package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * General Kudro of Drannith
 * {1}{W}{B}
 * Legendary Creature — Human Soldier
 * 3/3
 * Other Humans you control get +1/+1.
 * Whenever General Kudro or another Human you control enters, exile target card from an
 * opponent's graveyard.
 * {2}, Sacrifice two Humans: Destroy target creature with power 4 or greater.
 *
 * Kudro is itself a Human, so a single "a Human you control enters" trigger covers both halves
 * of the middle ability. It may be sacrificed to pay its own last ability's cost.
 */
val GeneralKudroOfDrannith = card("General Kudro of Drannith") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "Other Humans you control get +1/+1.\n" +
        "Whenever General Kudro or another Human you control enters, exile target card from an opponent's graveyard.\n" +
        "{2}, Sacrifice two Humans: Destroy target creature with power 4 or greater."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl()).enters()
        val t = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(t)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature.withSubtype(Subtype.HUMAN))
        )
        val t = target(TargetFilter.Creature.powerAtLeast(4))
        effect = Effects.Destroy(t)
        description = "{2}, Sacrifice two Humans: Destroy target creature with power 4 or greater."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c3227ae-0c72-478a-a6dd-661aaf718038.jpg?1783931024"
        ruling(
            "2020-04-17",
            "Only creatures are Humans. Planeswalkers such as Lukka and Narset who represent human characters won't cause General Kudro's middle ability to trigger."
        )
        ruling("2020-04-17", "General Kudro can be sacrificed to pay the cost of its own last ability. How noble.")
    }
}
