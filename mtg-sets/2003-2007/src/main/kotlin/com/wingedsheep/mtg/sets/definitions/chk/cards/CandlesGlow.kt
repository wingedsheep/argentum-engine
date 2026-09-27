package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Rarity

/**
 * Candles' Glow
 * {1}{W}
 * Instant — Arcane
 * Prevent the next 3 damage that would be dealt to any target this turn. You gain life equal to
 * the damage prevented this way.
 * Splice onto Arcane {1}{W}
 *
 * The life comes from the shield as it prevents damage, not from the spell resolving — a shield
 * that never prevents anything gains nothing.
 */
val CandlesGlow = card("Candles' Glow") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant — Arcane"
    oracleText = "Prevent the next 3 damage that would be dealt to any target this turn. You gain life equal to " +
        "the damage prevented this way.\n" +
        "Splice onto Arcane {1}{W} (As you cast an Arcane spell, you may reveal this card from your " +
        "hand and pay its splice cost. If you do, add this card's effects to that spell.)"

    splice("{1}{W}")

    spell {
        val t = target(Targets.Any)
        effect = Effects.PreventDamage(target = t, amount = DynamicAmounts.fixed(3), gainLifeFromPrevented = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0758d20f-6f0f-462e-a7ec-2511c146983d.jpg?1783944341"
    }
}
