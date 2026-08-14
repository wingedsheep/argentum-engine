package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Team Tactics — Marvel Super Heroes #155
 * {1}{R} · Instant
 *
 * Teamwork 1 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 1 or more.)
 * Target creature gains double strike until end of turn. If this spell was cast using teamwork,
 * that creature also gains trample until end of turn.
 *
 * The plain spell-rider shape of teamwork (CR 702.194b), gated on [Conditions.TeamworkWasPaid].
 * "Also gains" is additive rather than a replacement, so both grants land on the same target with
 * the same end-of-turn duration and the spell keeps a single target requirement.
 */
val TeamTactics = card("Team Tactics") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Teamwork 1 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 1 or more.)\n" +
        "Target creature gains double strike until end of turn. If this spell was cast using " +
        "teamwork, that creature also gains trample until end of turn."

    teamwork(1)

    spell {
        val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature).then(
            ConditionalEffect(
                condition = Conditions.TeamworkWasPaid,
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Jake Murray"
        flavorText = "\"Most of my trick arrows don't talk this much.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db1c7a71-3d01-4f2a-9b25-2a19bb0d1a56.jpg?1783902925"
    }
}
