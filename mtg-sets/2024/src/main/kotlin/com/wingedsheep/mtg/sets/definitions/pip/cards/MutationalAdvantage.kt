package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutational Advantage — Fallout #111
 * {1}{G}{U} · Instant
 *
 * Permanents you control with counters on them gain hexproof and indestructible until end of
 * turn. Prevent all damage that would be dealt to those permanents this turn. Proliferate.
 *
 * The affected set is locked when the spell resolves (CR 611.2c and the 2024-03-08 ruling):
 * `ForEachInGroup` snapshots the permanents you control with any counter before iterating, and
 * each one gets its own keyword grants and its own prevention shield. A permanent that gains a
 * counter later — including from this spell's own proliferate, which comes after — isn't
 * affected; one that loses its counters stays affected.
 */
val MutationalAdvantage = card("Mutational Advantage") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Permanents you control with counters on them gain hexproof and indestructible " +
        "until end of turn. Prevent all damage that would be dealt to those permanents this turn. " +
        "Proliferate. (Choose any number of permanents and/or players, then give each another " +
        "counter of each kind already there.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.youControl().withAnyCounter()),
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity) then
                Effects.PreventDamage(target = EffectTarget.IterationEntity)
        ) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Maxim Kostin"
        flavorText = "\"Ow, my pipe!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1ff92f1-9dc7-48e6-9e7c-9ef4a88518cb.jpg?1783912387"
        ruling(
            "2024-03-08",
            "The set of permanents affected by Mutational Advantage is determined at the time " +
                "Mutational Advantage resolves. Permanents that gain counters later in the turn won't " +
                "become affected by this effect, and permanents that lose all of their counters later " +
                "in the turn won't stop being affected."
        )
    }
}
