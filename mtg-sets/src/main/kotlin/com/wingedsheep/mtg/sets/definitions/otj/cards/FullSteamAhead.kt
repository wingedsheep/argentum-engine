package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Full Steam Ahead
 * {3}{G}{G}
 * Sorcery
 *
 * Until end of turn, each creature you control gets +2/+2 and gains trample and
 * "This creature can't be blocked by more than one creature."
 *
 * Like Overrun (mass +N/+N + trample), but with a third granted clause — a temporary
 * static blocking restriction. Each creature you control is iterated
 * ([Effects.ForEachInGroup]) and given +2/+2 ([Effects.ModifyStats]), trample
 * ([Effects.GrantKeyword]), and the static ability "can't be blocked by more than one
 * creature" ([Effects.GrantStaticAbility] of [CantBeBlockedByMoreThan]) until end of
 * turn. Combat blocker validation consults the granted restriction alongside any printed
 * one (CR 509.1b).
 */
val FullSteamAhead = card("Full Steam Ahead") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, each creature you control gets +2/+2 and gains trample and " +
        "\"This creature can't be blocked by more than one creature.\""

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.ModifyStats(2, 2, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
                Effects.GrantStaticAbility(CantBeBlockedByMoreThan(maxBlockers = 1), EffectTarget.Self),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Inkognit"
        flavorText = "The runaway train tore through the manicured streets of Prosperity, its new " +
            "conductors reveling in the sheer destruction."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/084748d8-7169-4e86-a69c-631c6d7d3a1e.jpg?1712355924"
    }
}
