package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Riverguard's Reflexes
 *
 * {1}{W} Instant
 * Target creature gets +2/+2 and gains first strike until end of turn. Untap it.
 */
object RiverguardsReflexes {
    val definition = CardDefinition.instant(
        name = "Riverguard's Reflexes",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "Target creature gets +2/+2 and gains first strike until end of turn. Untap it.",
        metadata = ScryfallMetadata(
            collectorNumber = "33",
            rarity = Rarity.COMMON,
            artist = "Jarel Threat",
            imageUri = "https://cards.scryfall.io/normal/front/c/5/c5723f3f-5c9a-4c1e-9cac-5e3de8b5d5bb.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Riverguard's Reflexes") {
        // Target creature gets +2/+2, first strike, and untap
        spell(
            CompositeEffect(
                effects = listOf(
                    ModifyStatsEffect(
                        powerModifier = 2,
                        toughnessModifier = 2,
                        target = EffectTarget.TargetCreature
                    ),
                    GrantKeywordUntilEndOfTurnEffect(
                        keyword = Keyword.FIRST_STRIKE,
                        target = EffectTarget.TargetCreature
                    ),
                    TapUntapEffect(
                        target = EffectTarget.TargetCreature,
                        tap = false  // Untap
                    )
                )
            )
        )
    }
}
