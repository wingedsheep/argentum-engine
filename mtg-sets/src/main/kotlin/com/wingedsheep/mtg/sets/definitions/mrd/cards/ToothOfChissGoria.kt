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
 * Tooth of Chiss-Goria — Mirrodin #264
 * {3} · Artifact
 *
 * Flash
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * {T}: Target creature gets +1/+0 until end of turn.
 *
 * The Galvanic Key shape (flash artifact with a tap ability) plus the stock
 * [KeywordAbility.Affinity] over [CardType.ARTIFACT]. Flash and affinity together are the whole
 * point of the cycle: with three other artifacts out this is a free combat trick cast at instant
 * speed, and it can be activated the turn it enters because the ability's only cost is {T} on a
 * non-creature permanent — summoning sickness never applies to an artifact that isn't a creature.
 */
val ToothOfChissGoria = card("Tooth of Chiss-Goria") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "{T}: Target creature gets +1/+0 until end of turn."

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(1, 0, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "264"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db5a91db-1b86-4471-badc-884142c355ca.jpg?1783944497"
    }
}
