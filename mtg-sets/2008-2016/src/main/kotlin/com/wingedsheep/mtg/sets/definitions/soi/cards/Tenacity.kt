package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenacity
 * {3}{W}
 * Instant
 * Creatures you control get +1/+1 and gain lifelink until end of turn. Untap those creatures.
 * (Damage dealt by those creatures also causes you to gain that much life.)
 *
 * One pass over the group, so "those creatures" are exactly the ones that got the pump.
 */
val Tenacity = card("Tenacity") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 and gain lifelink until end of turn. Untap those creatures. " +
        "(Damage dealt by those creatures also causes you to gain that much life.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.IterationEntity) then
                Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Zoltan Boros"
        flavorText = "Thalia established the Order of Saint Traft in an effort to bring together cathars who " +
            "would stand against the corruption of the Lunarch Council."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41fbe149-3e1f-495e-be1b-efc3a6dbcb08.jpg?1783937807"
    }
}
