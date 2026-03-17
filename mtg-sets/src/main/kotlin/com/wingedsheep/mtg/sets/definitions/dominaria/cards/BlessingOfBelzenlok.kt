package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Blessing of Belzenlok
 * {B}
 * Instant
 * Target creature gets +2/+1 until end of turn. If it's legendary, it also gains lifelink
 * until end of turn.
 */
val BlessingOfBelzenlok = card("Blessing of Belzenlok") {
    manaCost = "{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+1 until end of turn. If it's legendary, it also gains lifelink until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(2, 1, t)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.legendary()),
                    effect = Effects.GrantKeyword(com.wingedsheep.sdk.core.Keyword.LIFELINK, t)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Joe Slucher"
        flavorText = "\"My heart is not mine, it is Belzenlok's. All hearts are his, and all blood.\" —\"Rite of Belzenlok\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d224940c-d87c-4317-9ca3-b704ef894a7b.jpg?1562743446"
    }
}
