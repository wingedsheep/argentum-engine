package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Confront the Past — Strixhaven: School of Mages #67 (canonical printing)
 * {X}{B} · Sorcery — Lesson
 *
 * Choose one —
 * • Return target planeswalker card with mana value X or less from your graveyard to the
 *   battlefield.
 * • Remove twice X loyalty counters from target planeswalker an opponent controls.
 *
 * Both modes read the one X paid at cast time. In the first, X is a filter bound —
 * `manaValueAtMostX()` on a planeswalker card you own in your graveyard (the Profane Command
 * shape). In the second, X is an amount doubled through [DynamicAmount.Multiply]
 * (`xValue() * 2`); a planeswalker with fewer loyalty counters loses all it has. Lesson is only a
 * subtype.
 */
val ConfrontThePast = card("Confront the Past") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery — Lesson"
    oracleText = "Choose one —\n" +
        "• Return target planeswalker card with mana value X or less from your graveyard to the " +
        "battlefield.\n" +
        "• Remove twice X loyalty counters from target planeswalker an opponent controls."

    spell {
        modal(chooseCount = 1) {
            mode("Return target planeswalker card with mana value X or less from your graveyard to the battlefield") {
                val planeswalkerCard = target(
                    TargetFilter.Planeswalker.ownedByYou().inZone(Zone.GRAVEYARD).manaValueAtMostX()
                )
                effect = Effects.PutOntoBattlefieldFromGraveyard(planeswalkerCard)
            }
            mode("Remove twice X loyalty counters from target planeswalker an opponent controls") {
                val planeswalker = target(TargetFilter.Planeswalker.opponentControls())
                effect = Effects.RemoveCounters(CounterType.LOYALTY, DynamicAmounts.xValue() * 2, planeswalker)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Kieran Yanner"
        flavorText = "\"Why, Gideon? Of all people, why save me?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1951763-be97-400c-aa95-6b101f47bddf.jpg?1783927369"
    }
}
