package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Scaled Nurturer
 * {1}{G}
 * Creature — Dragon Druid
 * 0/2
 *
 * {T}: Add {G}. When you spend this mana to cast a Dragon creature spell, you gain 2 life.
 *
 * The {G} is unrestricted; [ManaSpellRider.TriggersWhenSpent] queues the life-gain trigger above
 * the spell only when the mana pays for a Dragon creature spell, once per {G} spent.
 */
val ScaledNurturer = card("Scaled Nurturer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon Druid"
    power = 0
    toughness = 2
    oracleText = "{T}: Add {G}. When you spend this mana to cast a Dragon creature spell, you gain 2 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            riders = setOf(
                ManaSpellRider.TriggersWhenSpent(
                    spellFilter = GameObjectFilter.Creature.withSubtype("Dragon"),
                    effect = Effects.GainLife(2),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "David Gaillet"
        flavorText = "\"Life always starts small. Only time will tell what heights it may reach.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84ae5a76-b8b9-431d-8060-a6a96f5d1e0b.jpg?1783922704"
        ruling("2022-06-10", "The mana created by Scaled Nurturer can be spent on anything, not just Dragon creature spells.")
        ruling("2022-06-10", "The delayed triggered ability will trigger whether Scaled Nurturer is still on the battlefield or not.")
        ruling(
            "2022-06-10",
            "If more than one {G} produced by a Scaled Nurturer is spent to cast a single Dragon creature spell, " +
                "the delayed triggered ability associated with each mana spent will trigger. You will gain 2 life that many times."
        )
    }
}
