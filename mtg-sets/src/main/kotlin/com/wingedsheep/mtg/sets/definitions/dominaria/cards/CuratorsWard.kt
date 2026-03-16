package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Curator's Ward
 * {2}{U}
 * Enchantment — Aura
 * Enchant permanent
 * Enchanted permanent has hexproof.
 * When enchanted permanent leaves the battlefield, if it was historic, draw two cards.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val CuratorsWard = card("Curator's Ward") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\nEnchanted permanent has hexproof.\nWhen enchanted permanent leaves the battlefield, if it was historic, draw two cards. (Artifacts, legendaries, and Sagas are historic.)"

    auraTarget = Targets.Permanent

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF)
    }

    triggeredAbility {
        trigger = Triggers.EnchantedPermanentLeavesBattlefield
        triggerCondition = Conditions.TriggeringEntityWasHistoric
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10cc417f-0ea7-47a8-b7d0-aa8d20168faf.jpg?1562911270"
    }
}
