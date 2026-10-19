package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glyph of Delusion — Legends #60
 * {U} · Instant
 *
 * Put X glyph counters on target creature that target Wall blocked this turn, where X is the power
 * of that blocked creature. The creature gains "This creature doesn't untap during your untap step
 * if it has a glyph counter on it" and "At the beginning of your upkeep, remove a glyph counter from
 * this creature."
 *
 * The Wall is chosen first so the creature's filter can read it: dependent target selection offers
 * only creatures whose turn-scoped block record says that Wall blocked them
 * ([GameObjectFilter.wasBlockedByThisTurn]), and only Walls that blocked something. The gained
 * static rides the creature's own static-ability channel ([Effects.BecomeArtifact] with every
 * transforming knob off) so it projects; the gained upkeep trigger is a permanent grant.
 */
val GlyphOfDelusion = card("Glyph of Delusion") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put X glyph counters on target creature that target Wall blocked this turn, where X is the " +
        "power of that blocked creature. The creature gains \"This creature doesn't untap during your untap " +
        "step if it has a glyph counter on it\" and \"At the beginning of your upkeep, remove a glyph counter " +
        "from this creature.\""

    spell {
        val wall = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.WALL)))
        val blocked = target(TargetFilter(GameObjectFilter.Creature.wasBlockedByThisTurn(wall)))
        effect = Effects.AddDynamicCounters(CounterType.GLYPH, DynamicAmounts.powerOf(blocked), blocked) then
            Effects.BecomeArtifact(
                target = blocked,
                cardTypes = null,
                subtypes = null,
                colors = null,
                loseAllAbilities = false,
                grantedStaticAbilities = listOf(
                    ConditionalStaticAbility(
                        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source()),
                        condition = Conditions.SourceHasCounter(CounterType.GLYPH),
                    )
                ),
                duration = Duration.Permanent,
            ) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.you.beginningOf(Step.UPKEEP),
                    effect = Effects.RemoveCounters(CounterType.GLYPH, 1, EffectTarget.Self),
                    descriptionOverride = "At the beginning of your upkeep, remove a glyph counter from this creature."
                ),
                target = blocked,
                duration = Duration.Permanent,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee39da13-4b8a-4796-a7c2-aaa11992d573.jpg?1783948075"
    }
}
