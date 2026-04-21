package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Riverguard's Reflexes
 * {1}{W}
 * Instant
 *
 * Target creature gets +2/+2 and gains first strike until end of turn. Untap it.
 */
val RiverguardsReflexes = card("Riverguard's Reflexes") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains first strike until end of turn. Untap it."

    spell {
        val creature = target("creature", TargetCreature())
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(2, 2, creature),
                Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
                Effects.Untap(EffectTarget.ContextTarget(0))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Lucas Graciano"
        flavorText = "\"If you hire a riverguard for protection, go in with your best offer. They won't consider anything less.\"\n—Cenna, ferry pilot"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88a79f3e-ca74-467c-b0a8-22802ac5b465.jpg?1767956969"
    }
}
