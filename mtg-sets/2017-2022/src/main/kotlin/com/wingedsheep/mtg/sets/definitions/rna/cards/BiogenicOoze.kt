package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val OOZE_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/0/2/0260e0ae-bf09-430a-a449-c16219d84b63.jpg?1783933602"

/**
 * Biogenic Ooze — Ravnica Allegiance #122
 * {3}{G}{G} · Creature — Ooze · 2 / 2
 *
 * The end-step counter counts every Ooze you control, itself and the tokens included.
 */
val BiogenicOoze = card("Biogenic Ooze") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a 2/2 green Ooze creature token.\n" +
        "At the beginning of your end step, put a +1/+1 counter on each Ooze you control.\n" +
        "{1}{G}{G}{G}: Create a 2/2 green Ooze creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            imageUri = OOZE_TOKEN_IMAGE,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.youControl().withSubtype("Ooze")),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}{G}{G}")
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            imageUri = OOZE_TOKEN_IMAGE,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "122"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f11029a-b24d-4248-834f-b852b56857f6.jpg?1783933671"
    }
}
