package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Scales of Shale
 * {2}{B}
 * Instant
 *
 * Affinity for Lizards (This spell costs {1} less to cast for each Lizard you control.)
 * Target creature gets +2/+0 and gains lifelink and indestructible until end of turn.
 */
val ScalesOfShale = card("Scales of Shale") {
    manaCost = "{2}{B}"
    typeLine = "Instant"
    oracleText = "Affinity for Lizards (This spell costs {1} less to cast for each Lizard you control.)\n" +
        "Target creature gets +2/+0 and gains lifelink and indestructible until end of turn."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.LIZARD))

    spell {
        val t = target("creature", TargetCreature())
        effect = Effects.ModifyStats(2, 0, t)
            .then(Effects.GrantKeyword(Keyword.LIFELINK, t))
            .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Yohann Schepacz"
        flavorText = "Like shedding, but backwards."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ae14276-dbbd-4257-80e9-accd6c19f5b2.jpg?1721426499"
    }
}
