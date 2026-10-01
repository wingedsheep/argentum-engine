package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tyvar's Stand — Phyrexia: All Will Be One #190 (canonical printing)
 * {X}{G} · Instant
 *
 * Target creature you control gets +X/+X and gains hexproof and indestructible until end of turn.
 *
 * X is the value paid at cast time, read at resolution via [DynamicAmounts.xValue]; X = 0 is a
 * legal cast that still grants both keywords.
 */
val TyvarsStand = card("Tyvar's Stand") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +X/+X and gains hexproof and indestructible " +
        "until end of turn. (It can't be the target of spells or abilities your opponents " +
        "control. Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        val x = DynamicAmounts.xValue()
        effect = Effects.ModifyStats(x, x, creature) then
            Effects.GrantKeyword(Keyword.HEXPROOF, creature) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Kieran Yanner"
        flavorText = "\"When they write songs about this battle, I will make sure your name is " +
            "forgotten, devil.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7b362a5-382a-4016-b4f2-aa7b683354c4.jpg?1783918008"
    }
}
