package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You See a Guard Approach
 * {U}
 * Instant
 *
 * Choose one —
 * • Distract the Guard — Tap target creature.
 * • Hide — Target creature you control gains hexproof until end of turn.
 *
 * A plain choose-one `modal(chooseCount = 1)` with one target per mode: a tap over any creature,
 * and the Sheltering Word floating [Keyword.HEXPROOF] grant (until end of turn) on a creature
 * you control.
 */
val YouSeeAGuardApproach = card("You See a Guard Approach") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Distract the Guard — Tap target creature.\n" +
        "• Hide — Target creature you control gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    spell {
        modal(chooseCount = 1) {
            mode("Distract the Guard — Tap target creature.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            }
            mode("Hide — Target creature you control gains hexproof until end of turn.") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Mila Pesic"
        flavorText = "\"Looks like we've got company.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30c5fd8e-753a-45e1-bcdd-fb6e072ffbb0.jpg?1783926504"
    }
}
