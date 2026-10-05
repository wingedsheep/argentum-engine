package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Righteous Valkyrie
 * {2}{W}
 * Creature — Angel Cleric
 * 2/4
 *
 * Flying
 * Whenever another Angel or Cleric you control enters, you gain life equal to that creature's
 * toughness.
 * As long as you have at least 7 life more than your starting life total, creatures you control
 * get +2/+2.
 *
 * - "Angel or Cleric" is the bare tribal noun — any permanent with either subtype.
 * - The life gained reads the entering permanent's toughness as the trigger resolves, falling back
 *   to last-known information if it has left the battlefield (2021-02-05 ruling) — that is
 *   [DynamicAmounts.triggeringToughness].
 * - The anthem is gated on [Conditions.LifeAboveStartingBy], which compares against the seat's real
 *   starting life total (20 / 30 / 40), never a hardcoded 20. It includes the Valkyrie itself.
 */
val RighteousValkyrie = card("Righteous Valkyrie") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever another Angel or Cleric you control enters, you gain life equal to that creature's toughness.\n" +
        "As long as you have at least 7 life more than your starting life total, creatures you control get +2/+2."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Permanent.withAnySubtype("Angel", "Cleric").youControl()
        ).enters()
        effect = Effects.GainLife(DynamicAmounts.triggeringToughness())
    }

    staticAbility {
        condition = Conditions.LifeAboveStartingBy(7)
        ability = ModifyStats(2, 2, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02fb5f9f-8750-4eb5-a03a-6dacc60e0b90.jpg?1783928279"
        ruling(
            "2021-02-05",
            "The amount of life you gain is equal to the Angel or Cleric's toughness as the triggered " +
                "ability resolves. If the Angel or Cleric is no longer on the battlefield at that time, " +
                "use its toughness from when it was last on the battlefield."
        )
        ruling(
            "2021-02-05",
            "Because damage remains marked on creatures until the damage is removed as the turn ends, " +
                "nonlethal damage dealt to creatures you control may become lethal if your life total drops " +
                "below 7 life more than your starting life total or if Righteous Valkyrie leaves the " +
                "battlefield that turn."
        )
    }
}
