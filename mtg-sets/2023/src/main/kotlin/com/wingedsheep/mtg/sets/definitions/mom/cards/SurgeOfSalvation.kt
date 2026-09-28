package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Surge of Salvation
 * {W}
 * Instant
 *
 * You and permanents you control gain hexproof until end of turn. Prevent all damage that black
 * and/or red sources would deal to creatures you control this turn.
 *
 * The hexproof grant is locked in at resolution (only permanents you control then). The prevention
 * shield is a group shield whose recipient filter (`toGroup`) and source filter are re-read each time
 * damage would be dealt, so it also covers creatures you begin to control later this turn — as the
 * ruling requires.
 */
val SurgeOfSalvation = card("Surge of Salvation") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You and permanents you control gain hexproof until end of turn. Prevent all damage that " +
        "black and/or red sources would deal to creatures you control this turn."

    spell {
        effect = Effects.GrantHexproof(EffectTarget.Controller) then
            Patterns.Group.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.permanentsYouControl) then
            Effects.PreventDamage(
                toGroup = GameObjectFilter.Creature.youControl(),
                sources = PreventionSourceFilter.Matching(
                    GameObjectFilter.Any.withAnyColor(Color.BLACK, Color.RED)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Dominik Mayer"
        flavorText = "No Meletian had ever laid eyes on an angel, but when protective Halo flooded the " +
            "Multiverse, they knew a divine blessing was at hand."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41d25ee5-0348-4206-bb6a-ccb0a599ac87.jpg?1783917047"

        ruling(
            "2023-04-14",
            "The set of permanents that gain hexproof is determined as Surge of Salvation resolves. " +
                "Permanents you begin to control later in the turn won't gain hexproof. However, the damage " +
                "prevention effect will apply to all creatures you control throughout the turn, even if you " +
                "didn't control them (or they weren't creatures) as Surge of Salvation resolved."
        )
    }
}
