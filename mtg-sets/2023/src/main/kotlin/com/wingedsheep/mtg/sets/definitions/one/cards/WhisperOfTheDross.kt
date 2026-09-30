package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Whisper of the Dross
 * {B}
 * Instant
 * Target creature gets -1/-1 until end of turn. Proliferate.
 *
 * Both happen during resolution, before state-based actions are checked, so proliferating a
 * +1/+1 counter onto the shrunken creature can still save it.
 */
val WhisperOfTheDross = card("Whisper of the Dross") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-1 until end of turn. Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, t) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Eli Minaya"
        flavorText = "\"Live or die, you serve the Steel Thanes now.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58d287e8-5297-415a-97d5-02002470c52b.jpg?1783918038"
    }
}
