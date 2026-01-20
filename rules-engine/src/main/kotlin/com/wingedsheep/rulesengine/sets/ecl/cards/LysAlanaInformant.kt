package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SurveilEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Lys Alana Informant
 *
 * {1}{G} Creature — Elf Scout 3/1
 * When this creature enters or dies, surveil 1.
 */
object LysAlanaInformant {
    val definition = CardDefinition.creature(
        name = "Lys Alana Informant",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.SCOUT),
        power = 3,
        toughness = 1,
        oracleText = "When this creature enters or dies, surveil 1.",
        metadata = ScryfallMetadata(
            collectorNumber = "181",
            rarity = Rarity.COMMON,
            artist = "Sidharth Chaturvedi",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aabb1234-5678-9012-cdef-aabb12345678.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Lys Alana Informant") {
        // ETB: Surveil 1
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SurveilEffect(count = 1)
        )

        // On death: Surveil 1
        triggered(
            trigger = OnDeath(),
            effect = SurveilEffect(count = 1)
        )
    }
}
