package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Appeal to Eirdu
 *
 * {3}{W} Instant
 * Convoke
 * One or two target creatures each get +2/+1 until end of turn.
 *
 * Flavor: "Adherents of the sun implore others to bask in Eirdu's light
 * and expel darkness from the plane."
 */
object AppealToEirdu {
    val definition = CardDefinition.instant(
        name = "Appeal to Eirdu",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap " +
                "while casting this spell pays for {1} or one mana of that creature's color.)\n" +
                "One or two target creatures each get +2/+1 until end of turn.",
        keywords = setOf(Keyword.CONVOKE),
        metadata = ScryfallMetadata(
            collectorNumber = "5",
            rarity = Rarity.COMMON,
            artist = "Milivoj Ćeran",
            flavorText = "Adherents of the sun implore others to bask in Eirdu's light and expel darkness from the plane.",
            imageUri = "https://cards.scryfall.io/normal/front/6/8/68ce9752-21f9-48e9-bf48-7f76f1cecbc5.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Appeal to Eirdu") {
        // Convoke is a keyword that modifies casting cost - handled during casting
        keywords(Keyword.CONVOKE)

        // One or two target creatures each get +2/+1 until end of turn
        // Note: Multi-target spells require specialized targeting (1-2 targets)
        spell(
            ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 1,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
        )
    }
}
