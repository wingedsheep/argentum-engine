package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * War of the Last Alliance
 * {3}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Search your library for a legendary creature card, reveal it, put it into your hand, then shuffle.
 * III — Creatures you control gain double strike until end of turn. The Ring tempts you.
 */
val WarOfTheLastAlliance = card("War of the Last Alliance") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Search your library for a legendary creature card, reveal it, put it into your hand, then shuffle.\n" +
        "III — Creatures you control gain double strike until end of turn. The Ring tempts you."

    sagaChapter(1) {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Creature.legendary(),
            count = 1,
            reveal = true
        )
    }

    sagaChapter(2) {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Creature.legendary(),
            count = 1,
            reveal = true
        )
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = GrantKeywordEffect(Keyword.DOUBLE_STRIKE.name, EffectTarget.Self, Duration.EndOfTurn)
        ).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60ddf2cd-5b33-4c8a-a610-8e6a15404dde.jpg?1686967971"
    }
}
