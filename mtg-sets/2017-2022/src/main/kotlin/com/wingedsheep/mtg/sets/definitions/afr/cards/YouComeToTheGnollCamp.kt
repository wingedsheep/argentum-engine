package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * You Come to the Gnoll Camp
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • Intimidate Them — Up to two target creatures can't block this turn.
 * • Fend Them Off — Target creature gets +3/+1 until end of turn.
 *
 * A choose-one `modal(chooseCount = 1)`: Abandon the Post's optional two-count can't-block
 * (applied per chosen target), and a plain +3/+1 pump until end of turn.
 */
val YouComeToTheGnollCamp = card("You Come to the Gnoll Camp") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Intimidate Them — Up to two target creatures can't block this turn.\n" +
        "• Fend Them Off — Target creature gets +3/+1 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Intimidate Them — Up to two target creatures can't block this turn.") {
                targets(TargetFilter.Creature, count = 2, optional = true)
                effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
            }
            mode("Fend Them Off — Target creature gets +3/+1 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(3, 1, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Billy Christian"
        flavorText = "\"Easy now, everyone. I'll take care of this.\"\n—Nadaar"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e893613-423e-409b-9334-292bd252405b.jpg?1783926469"
        ruling(
            "2021-07-23",
            "Casting this spell and choosing the Intimidate Them mode after blockers have been declared won't change or undo any blocks."
        )
    }
}
