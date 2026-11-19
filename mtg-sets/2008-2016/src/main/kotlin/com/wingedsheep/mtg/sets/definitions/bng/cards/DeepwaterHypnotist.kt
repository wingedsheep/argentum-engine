package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deepwater Hypnotist
 * {1}{U}
 * Creature — Merfolk Wizard
 * 2/1
 *
 * Inspired — Whenever this creature becomes untapped, target creature an opponent controls gets
 * -3/-0 until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val DeepwaterHypnotist = card("Deepwater Hypnotist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "Inspired — Whenever this creature becomes untapped, target creature an opponent " +
        "controls gets -3/-0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-3, 0, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Christopher Moeller"
        flavorText = "\"Watch the waves too long, and you may never look away.\"\n—Meletian proverb"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab7c6a7-8214-4644-8f2e-ec74d2aff27e.jpg?1783939571"
    }
}
