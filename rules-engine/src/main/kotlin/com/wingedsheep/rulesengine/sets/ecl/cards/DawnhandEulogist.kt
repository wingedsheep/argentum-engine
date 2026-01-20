package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DrainEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.MillEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Dawnhand Eulogist
 *
 * {3}{B} Creature — Elf Warlock 3/3
 * Menace
 * When this creature enters, mill three cards. Then if there is an Elf card in your graveyard,
 * each opponent loses 2 life and you gain 2 life.
 */
object DawnhandEulogist {
    val definition = CardDefinition.creature(
        name = "Dawnhand Eulogist",
        manaCost = ManaCost.parse("{3}{B}"),
        subtypes = setOf(Subtype.ELF, Subtype.WARLOCK),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.MENACE),
        oracleText = "Menace\nWhen this creature enters, mill three cards. Then if there is an Elf card " +
                "in your graveyard, each opponent loses 2 life and you gain 2 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "99",
            rarity = Rarity.COMMON,
            artist = "Evyn Fong",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa3a3a3a-3a3a-3a3a-3a3a-3a3a3a3a3a3a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Dawnhand Eulogist") {
        keywords(Keyword.MENACE)

        // ETB: Mill 3, then conditional drain
        // TODO: Conditional based on graveyard contents needs Condition infrastructure
        // For now, mill and drain happen unconditionally
        triggered(
            trigger = OnEnterBattlefield(),
            effect = MillEffect(count = 3) then DrainEffect(
                amount = 2,
                target = EffectTarget.EachOpponent
            )
        )
    }
}
