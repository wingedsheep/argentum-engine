package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abandon Reason
 * {2}{R}
 * Instant
 * Up to two target creatures each get +1/+0 and gain first strike until end of turn.
 * Madness {1}{R}
 */
val AbandonReason = card("Abandon Reason") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Up to two target creatures each get +1/+0 and gain first strike until end of turn. (They deal combat damage before creatures without first strike.)\n" +
        "Madness {1}{R} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.ContextTarget(0)),
        )
    }

    madness("{1}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c0b3b3-bb62-42c5-9869-386af0540a9b.jpg?1783937467"
    }
}
