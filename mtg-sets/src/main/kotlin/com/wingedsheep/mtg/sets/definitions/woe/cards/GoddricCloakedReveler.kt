package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goddric, Cloaked Reveler
 * {1}{R}{R}
 * Legendary Creature — Human Noble
 * 3/3
 *
 * Haste
 * Celebration — As long as two or more nonland permanents entered under your control this turn,
 * Goddric is a Dragon with base power and toughness 4/4, flying, and
 * "{R}: Dragons you control get +1/+0 until end of turn." He loses all other creature types.
 *
 * All four continuous changes share one Celebration gate. The subtype replacement is Layer 4,
 * flying and the granted activated ability are Layer 6, and the base P/T change is Layer 7b. The
 * granted ability snapshots the Dragons present when it resolves through ForEachInGroup.
 */
val GoddricCloakedReveler = card("Goddric, Cloaked Reveler") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Noble"
    oracleText = "Haste\nCelebration — As long as two or more nonland permanents entered the " +
        "battlefield under your control this turn, Goddric is a Dragon with base power and " +
        "toughness 4/4, flying, and \"{R}: Dragons you control get +1/+0 until end of turn.\" " +
        "(It loses all other creature types.)"
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ConditionalStaticAbility(
            condition = Conditions.Celebration,
            ability = CompositeStaticAbility(
                listOf(
                    TransformPermanent(setSubtypes = setOf("Dragon"), filter = Filters.Self),
                    SetBasePowerToughnessStatic(4, 4, Filters.Self),
                    GrantKeyword(Keyword.FLYING, Filters.Self),
                    GrantActivatedAbility(
                        ability = ActivatedAbility(
                            id = AbilityId.generate(),
                            cost = Costs.Mana("{R}"),
                            effect = Effects.ForEachInGroup(
                                GroupFilter(GameObjectFilter.Creature.withSubtype("Dragon").youControl()),
                                Effects.ModifyStats(1, 0, EffectTarget.Self),
                            ),
                            descriptionOverride = "{R}: Dragons you control get +1/+0 until end of turn.",
                        ),
                        filter = Filters.Self,
                    ),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe93ef82-51de-40ad-9b52-8f3fd11c144f.jpg?1783915094"

        ruling(
            "2023-09-01",
            "If an effect causes Goddric to lose all abilities during a turn in which it has " +
                "already become a Dragon, he's still a Dragon creature with base power and " +
                "toughness 4/4."
        )
        ruling(
            "2023-09-01",
            "Goddric's activated ability affects only Dragons you control at the time it resolves. " +
                "Any Dragons that come under your control later in the turn won't be affected."
        )
        ruling(
            "2023-09-01",
            "Celebration abilities only care if two or more nonland permanents entered the " +
                "battlefield under your control in a turn. They won't get more powerful if more " +
                "than two permanents entered the battlefield under your control in a turn."
        )
        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield " +
                "or under your control. Celebration abilities are checking for past events, not " +
                "the current game state."
        )
    }
}
