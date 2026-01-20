package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.OnSpellCast
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Kulrath Mystic
 *
 * {2}{U} Creature — Elemental Wizard 2/4
 * Whenever you cast a spell with mana value 4 or greater, this creature gets +2/+0
 * and gains vigilance until end of turn.
 */
object KulrathMystic {
    val definition = CardDefinition.creature(
        name = "Kulrath Mystic",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.ELEMENTAL, Subtype.WIZARD),
        power = 2,
        toughness = 4,
        oracleText = "Whenever you cast a spell with mana value 4 or greater, this creature gets +2/+0 " +
                "and gains vigilance until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "56",
            rarity = Rarity.COMMON,
            artist = "Jason A. Engle",
            imageUri = "https://cards.scryfall.io/normal/front/f/f/ff0f0f0f-0f0f-0f0f-0f0f-0f0f0f0f0f0f.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Kulrath Mystic") {
        // Whenever you cast a spell with MV 4+, get +2/+0 and vigilance until EOT
        triggered(
            trigger = OnSpellCast(controllerOnly = true, manaValueAtLeast = 4),
            effect = CompositeEffect(
                effects = listOf(
                    ModifyStatsEffect(
                        powerModifier = 2,
                        toughnessModifier = 0,
                        target = EffectTarget.Self,
                        untilEndOfTurn = true
                    ),
                    GrantKeywordUntilEndOfTurnEffect(
                        keyword = Keyword.VIGILANCE,
                        target = EffectTarget.Self
                    )
                )
            )
        )
    }
}
