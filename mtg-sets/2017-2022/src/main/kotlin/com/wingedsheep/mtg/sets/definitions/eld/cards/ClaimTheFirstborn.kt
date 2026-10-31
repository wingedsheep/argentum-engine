package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Claim the Firstborn
 * {R}
 * Sorcery
 * Gain control of target creature with mana value 3 or less until end of turn. Untap that creature.
 * It gains haste until end of turn.
 */
val ClaimTheFirstborn = card("Claim the Firstborn") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature with mana value 3 or less until end of turn. Untap that creature. It gains haste until end of turn."

    spell {
        val t = target(TargetFilter.Creature.manaValueAtMost(3))
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Iain McCaig"
        flavorText = "\"The deal was struck and the favor had been done. And so, under the cover of night, the debt was paid.\"\n—Tales of the Fae"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feaf1e6c-c7d9-4ac7-9aeb-c4b5d61548ec.jpg?1783932626"
    }
}
