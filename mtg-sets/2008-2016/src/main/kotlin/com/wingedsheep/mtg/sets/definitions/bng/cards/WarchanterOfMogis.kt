package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Warchanter of Mogis
 * {3}{B}{B}
 * Creature — Minotaur Shaman
 * 3/3
 *
 * Inspired — Whenever this creature becomes untapped, target creature you control gains intimidate
 * until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val WarchanterOfMogis = card("Warchanter of Mogis") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Minotaur Shaman"
    power = 3
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, target creature you control gains " +
        "intimidate until end of turn. (A creature with intimidate can't be blocked except by artifact " +
        "creatures and/or creatures that share a color with it.)"

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.INTIMIDATE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/757948b3-8cb2-4709-9bbf-031c2bf6d4c5.jpg?1783939551"
    }
}
