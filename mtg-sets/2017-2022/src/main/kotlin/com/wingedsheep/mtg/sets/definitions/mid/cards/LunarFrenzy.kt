package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lunar Frenzy — Innistrad: Midnight Hunt #147.
 * {X}{R} · Instant
 *
 * Target creature you control gets +X/+0 and gains first strike and trample until end of turn.
 *
 * One target, three until-end-of-turn effects in printed order: [Effects.ModifyStats] for +X/+0
 * (X read from the spell's announced X), then [Effects.GrantKeyword] for first strike and trample.
 */
val LunarFrenzy = card("Lunar Frenzy") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +X/+0 and gains first strike and trample until end of turn."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Alix Branwyn"
        flavorText = "In the adrenaline-fueled chaos, she hadn't even felt the bite."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5fb18c4-1386-4ea0-97c1-4bdd1e7eb5a6.jpg?1783925597"
    }
}
