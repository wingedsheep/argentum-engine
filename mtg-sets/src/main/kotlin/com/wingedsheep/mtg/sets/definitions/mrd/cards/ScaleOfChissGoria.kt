package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Scale of Chiss-Goria — Mirrodin #236
 * {3} · Artifact
 *
 * Flash
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * {T}: Target creature gets +0/+1 until end of turn.
 *
 * The defensive half of the Chiss-Goria pair — identical in shape to
 * [ToothOfChissGoria] (+1/+0) and to the rest of the flash/affinity "relic" cycle: stock
 * [KeywordAbility.Affinity] over [CardType.ARTIFACT] plus a bare {T} ability. Summoning sickness
 * never applies (it's a non-creature artifact), so with enough artifacts out it can be flashed in
 * and tapped in the same combat as a free +0/+1 blocking trick.
 */
val ScaleOfChissGoria = card("Scale of Chiss-Goria") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "{T}: Target creature gets +0/+1 until end of turn."

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(0, 1, t)
        description = "{T}: Target creature gets +0/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/133b367c-b1a1-46f7-a539-a33ee655affb.jpg?1783944506"
    }
}
