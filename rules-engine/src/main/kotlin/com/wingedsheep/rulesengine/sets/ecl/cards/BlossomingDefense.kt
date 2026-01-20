package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.TargetCreature
import com.wingedsheep.rulesengine.targeting.CreatureTargetFilter

/**
 * Blossoming Defense
 *
 * {G} Instant
 * Target creature you control gets +2/+2 and gains hexproof until end of turn.
 */
object BlossomingDefense {
    val definition = CardDefinition.instant(
        name = "Blossoming Defense",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Target creature you control gets +2/+2 and gains hexproof until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "167",
            rarity = Rarity.UNCOMMON,
            artist = "Eelis Kyttanen",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aabbccdd-1234-5678-abcd-aabbccdd1234.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Blossoming Defense") {
        targets(TargetCreature(filter = CreatureTargetFilter.YouControl))

        spell(
            ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 2,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            ) then GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.HEXPROOF,
                target = EffectTarget.TargetCreature
            )
        )
    }
}
