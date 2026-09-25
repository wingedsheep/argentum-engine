package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hisoka's Defiance
 * {1}{U}
 * Instant
 * Counter target Spirit or Arcane spell.
 *
 * "Spirit or Arcane" is one subtype disjunction on the spell — the same `withAnySubtype` the
 * Kamigawa "whenever you cast a Spirit or Arcane spell" triggers use (`KamiOfTheHunt.kt`).
 */
val HisokasDefiance = card("Hisoka's Defiance") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target Spirit or Arcane spell."

    spell {
        target(TargetFilter.SpellOnStack.withAnySubtype("Spirit", "Arcane"))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Greg Hildebrandt"
        flavorText = "\"With every passing day, the kami shape our world to suit their will. I, for one, would not see them succeed.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09fd4d01-1204-46a3-b237-45c37985acac.jpg?1783944326"
    }
}
