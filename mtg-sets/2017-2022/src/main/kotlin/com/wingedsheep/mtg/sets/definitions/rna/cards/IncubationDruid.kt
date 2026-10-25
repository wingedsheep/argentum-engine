package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Incubation Druid
 * {1}{G}
 * Creature — Elf Druid
 * 0/2
 * {T}: Add one mana of any type that a land you control could produce. If this creature has a
 * +1/+1 counter on it, add three mana of that type instead.
 * {3}{G}{G}: Adapt 3.
 *
 * "Any type" is any color *or* colorless (CR 106.1b), read per CR 106.7 — authored as Naga
 * Vitalist's two halves: the colors from `ManaColorSet.LandsCouldProduce(YOU)`, and `{C}` as a
 * second mana ability usable only while a land you control could produce colorless mana. Both
 * halves add one mana, or three of the chosen type while this creature has a +1/+1 counter.
 */
val IncubationDruid = card("Incubation Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 0
    toughness = 2
    oracleText = "{T}: Add one mana of any type that a land you control could produce. If this creature " +
        "has a +1/+1 counter on it, add three mana of that type instead.\n" +
        "{3}{G}{G}: Adapt 3. (If this creature has no +1/+1 counters on it, put three +1/+1 counters on it.)"

    val amount = DynamicAmounts.conditional(
        Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
        ifTrue = 3,
        ifFalse = 1,
    )

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.LandsCouldProduce(LandControllerScope.YOU),
            amount = amount,
        )
        description = "{T}: Add one mana of any color that a land you control could produce " +
            "(three if this creature has a +1/+1 counter on it)."
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(amount)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Land.couldProduceColorlessMana())
            ),
        )
        description = "{T}: Add {C} ({C}{C}{C} if this creature has a +1/+1 counter on it). " +
            "Activate only if a land you control could produce colorless mana."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{G}")
        effect = Effects.Adapt(3)
        description = "{3}{G}{G}: Adapt 3."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/075bbe5d-d0f3-4be3-a3a6-072d5d3d614c.jpg?1783933668"
        ruling("2019-01-25", "The types of mana are white, blue, black, red, green, and colorless.")
        ruling("2019-01-25", "Incubation Druid doesn't care about any restrictions or riders your lands put on the mana they produce. It just produces one mana of the appropriate type, with no restrictions or riders.")
        ruling("2019-01-25", "Incubation Druid checks the effects of all mana-producing abilities of lands you control, but it doesn't check their costs or legality.")
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
    }
}
