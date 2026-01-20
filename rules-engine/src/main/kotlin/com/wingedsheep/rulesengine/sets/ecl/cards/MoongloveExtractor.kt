package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Moonglove Extractor
 *
 * {2}{B} Creature — Elf Warlock 2/1
 * Whenever this creature attacks, you draw a card and lose 1 life.
 */
object MoongloveExtractor {
    val definition = CardDefinition.creature(
        name = "Moonglove Extractor",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype.ELF, Subtype.WARLOCK),
        power = 2,
        toughness = 1,
        oracleText = "Whenever this creature attacks, you draw a card and lose 1 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "109",
            rarity = Rarity.COMMON,
            artist = "Milivoj Ćeran",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/dd7d7d7d-7d7d-7d7d-7d7d-7d7d7d7d7d7d.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Moonglove Extractor") {
        // Attack trigger: draw a card and lose 1 life
        triggered(
            trigger = OnAttack(),
            effect = CompositeEffect(
                effects = listOf(
                    DrawCardsEffect(count = 1, target = EffectTarget.Controller),
                    LoseLifeEffect(amount = 1, target = EffectTarget.Controller)
                )
            )
        )
    }
}
