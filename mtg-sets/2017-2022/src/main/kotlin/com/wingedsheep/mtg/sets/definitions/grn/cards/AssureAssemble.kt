package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Assure // Assemble (GRN 221) — split-layout spell (CR 709).
 *
 * Assure {G/W}{G/W} — Instant
 *   Put a +1/+1 counter on target creature. That creature gains indestructible until end of turn.
 *
 * Assemble {4}{G}{W} — Instant
 *   Create three 2/2 green and white Elf Knight creature tokens with vigilance.
 */
val AssureAssemble = card("Assure // Assemble") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Assure") {
        manaCost = "{G/W}{G/W}"
        typeLine = "Instant"
        oracleText = "Put a +1/+1 counter on target creature. That creature gains indestructible until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
        }
    }

    face("Assemble") {
        manaCost = "{4}{G}{W}"
        typeLine = "Instant"
        oracleText = "Create three 2/2 green and white Elf Knight creature tokens with vigilance."

        spell {
            effect = Effects.CreateToken(
                count = 3,
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Elf", "Knight"),
                keywords = setOf(Keyword.VIGILANCE)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad454e7a-06c9-4694-ae68-7b1431e00077.jpg?1783934113"
    }
}
