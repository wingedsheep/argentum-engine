package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Rites of Reaping — Return to Ravnica #191
 * {4}{B}{G} · Sorcery · Uncommon
 *
 * Target creature gets +3/+3 until end of turn. Another target creature gets -3/-3 until end of turn.
 *
 * Two independent target slots, the second wrapped in [TargetOther] so it cannot be the same
 * creature as the first ("another"), mirroring Drooling Groodion.
 */
val RitesOfReaping = card("Rites of Reaping") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +3/+3 until end of turn. Another target creature gets -3/-3 until end of turn."

    spell {
        val pumped = target(TargetFilter.Creature)
        val weakened = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.ModifyStats(3, 3, pumped, Duration.EndOfTurn) then
            Effects.ModifyStats(-3, -3, weakened, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "David Rapoza"
        flavorText = "Don't cross a Devkarin elf unless you long for an early grave."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/115d504f-3aec-4374-8cd8-732d56c448f2.jpg?1783940333"
    }
}
