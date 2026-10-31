package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barge In
 * {R}
 * Instant
 *
 * Target attacking creature gets +2/+2 until end of turn. Each attacking non-Human creature gains
 * trample until end of turn.
 *
 * The trample half is an untargeted group — every attacking non-Human creature, whoever controls
 * it — so it is [Effects.ForEachInGroup] with [EffectTarget.IterationEntity]. The spell's only
 * target is the attacker; if it is illegal on resolution the whole spell doesn't resolve, so no
 * creature gains trample (ruling 2019-10-04).
 */
val BargeIn = card("Barge In") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target attacking creature gets +2/+2 until end of turn. Each attacking non-Human " +
        "creature gains trample until end of turn."

    spell {
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(2, 2, attacker) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.attacking().notSubtype(Subtype.HUMAN)),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Mark Behm"
        flavorText = "The giant had heard of locks, but he had yet to discover a door he couldn't open."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bfe337c-4b8c-42ce-8c43-4c95051d09ac.jpg?1783932628"
        ruling(
            "2019-10-04",
            "If the target attacking creature is an illegal target by the time Barge In tries to resolve, " +
                "the spell won't resolve. Your attacking non-Human creatures won't gain trample."
        )
    }
}
