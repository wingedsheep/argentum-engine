package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Toxin Analysis — Murders at Karlov Manor #107
 * {B} · Instant
 *
 * Target creature gains deathtouch and lifelink until end of turn. Investigate.
 *
 * The Clue is not conditional on the target surviving: investigate happens on resolution
 * regardless. If the single target is illegal by then the whole spell is countered on
 * resolution (CR 608.2b) and no Clue is created.
 */
val ToxinAnalysis = card("Toxin Analysis") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gains deathtouch and lifelink until end of turn. Investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.DEATHTOUCH, t),
            Effects.GrantKeyword(Keyword.LIFELINK, t),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Irina Nordsol"
        flavorText = "\"Poison this deadly is only used by the Ochran. Either the Golgari are behind " +
            "this, or someone wants us to think they are.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eda1aff-c1f4-4171-a800-605396cc8168.jpg?1783912889"
    }
}
