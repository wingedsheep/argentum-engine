package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Costs

/**
 * Duty Beyond Death — Tarkir: Dragonstorm #10
 * {1}{W} · Instant · Uncommon
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Creatures you control gain indestructible until end of turn. Put a +1/+1 counter on each
 * creature you control.
 *
 * Both clauses apply to "creatures you control" — they affect the same group of creatures
 * present as the spell resolves (the sacrificed creature is already gone, paid as a cast cost).
 */
val DutyBeyondDeath = card("Duty Beyond Death") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Creatures you control gain indestructible until end of turn. Put a +1/+1 counter on " +
        "each creature you control."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.Composite(
            listOf(
                // Creatures you control gain indestructible until end of turn.
                Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = GrantKeywordEffect(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
                ),
                // Put a +1/+1 counter on each creature you control.
                Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    effect = AddCountersEffect(
                        counterType = Counters.PLUS_ONE_PLUS_ONE,
                        count = 1,
                        target = EffectTarget.Self
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Kev Fang"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e92640d-768b-4357-905f-bea017d351cc.jpg?1743203993"
    }
}
