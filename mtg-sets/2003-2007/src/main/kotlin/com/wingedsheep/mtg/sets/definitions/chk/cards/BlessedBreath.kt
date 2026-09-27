package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blessed Breath
 * {W}
 * Instant — Arcane
 * Target creature you control gains protection from the color of your choice until end of turn.
 * Splice onto Arcane {W}
 *
 * The colour is chosen on resolution (`ChooseColorThen`), not at cast time.
 */
val BlessedBreath = card("Blessed Breath") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant — Arcane"
    oracleText = "Target creature you control gains protection from the color of your choice until end of turn.\n" +
        "Splice onto Arcane {W} (As you cast an Arcane spell, you may reveal this card from your " +
        "hand and pay its splice cost. If you do, add this card's effects to that spell.)"

    splice("{W}")

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Tsutomu Kawade"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e2d8650-b8c3-4e89-9aa2-424a98715c38.jpg?1783944343"
    }
}
