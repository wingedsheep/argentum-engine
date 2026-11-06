package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hapatra's Mark — Amonkhet #169 (canonical printing)
 * {G} · Instant
 *
 * Target creature you control gains hexproof until end of turn. Remove all -1/-1 counters from it.
 * (A creature with hexproof can't be the target of spells or abilities your opponents control.)
 *
 * Sheltering Word's floating-keyword hexproof grant, followed by removing every -1/-1 counter from
 * the same target.
 */
val HapatrasMark = card("Hapatra's Mark") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gains hexproof until end of turn. Remove all -1/-1 " +
        "counters from it. (A creature with hexproof can't be the target of spells or abilities your " +
        "opponents control.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature) then
            Effects.RemoveAllCountersOfType(CounterType.MINUS_ONE_MINUS_ONE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Lindsey Look"
        flavorText = "\"If you fear poisons, it is because you are ignorant of them.\"\n—Hapatra, vizier of poisons"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/199d6a8d-008d-4495-9cbc-b8d680fab13c.jpg?1783936475"
    }
}
