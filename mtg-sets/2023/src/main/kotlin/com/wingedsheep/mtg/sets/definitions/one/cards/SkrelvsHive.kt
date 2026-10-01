package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skrelv's Hive
 * {1}{W}
 * Enchantment
 * At the beginning of your upkeep, you lose 1 life and create a 1/1 colorless Phyrexian Mite
 * artifact creature token with toxic 1 and "This token can't block."
 * Corrupted — As long as an opponent has three or more poison counters, creatures you control
 * with toxic have lifelink.
 *
 * "With toxic" is `withKeyword(TOXIC)`, matching any toxic N (printed or granted) off the
 * projected keyword — the same check as [FlensingRaptor].
 */
val SkrelvsHive = card("Skrelv's Hive") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you lose 1 life and create a 1/1 colorless Phyrexian Mite " +
        "artifact creature token with toxic 1 and \"This token can't block.\"\n" +
        "Corrupted — As long as an opponent has three or more poison counters, creatures you control " +
        "with toxic have lifelink."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreatePhyrexianMite()
        description = "At the beginning of your upkeep, you lose 1 life and create a 1/1 colorless " +
            "Phyrexian Mite artifact creature token with toxic 1 and \"This token can't block.\""
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(
                Keyword.LIFELINK,
                GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC))
            ),
            condition = Conditions.Corrupted
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffbd77ec-fc81-41d5-934f-c3dd844cb053.jpg?1783918072"
    }
}
