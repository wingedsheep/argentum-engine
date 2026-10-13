package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Timely Interference — Dominaria United #70
 * {U} · Instant
 *
 * The kicked rider is [Effects.MarkMustBlockThisTurn] — a blocking *requirement* (a tapped
 * creature or one with no legal block simply doesn't block). The draw happens whether or not
 * the spell was kicked.
 */
val TimelyInterference = card("Timely Interference") {
    manaCost = "{U}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Kicker {1}{R} (You may pay an additional {1}{R} as you cast this spell.)\n" +
        "Target creature gets -1/-0 until end of turn. If this spell was kicked, that creature " +
        "blocks this turn if able.\n" +
        "Draw a card."

    keywordAbility(KeywordAbility.kicker("{1}{R}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, 0, creature) then
            Effects.If(Conditions.WasKicked, Effects.MarkMustBlockThisTurn(creature)) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Joshua Raphael"
        flavorText = "Students fled with their research as Tolaria fell to the Phyrexians."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/017a3c6b-9a1e-403a-9c20-2360090d39ee.jpg?1783921342"
    }
}
