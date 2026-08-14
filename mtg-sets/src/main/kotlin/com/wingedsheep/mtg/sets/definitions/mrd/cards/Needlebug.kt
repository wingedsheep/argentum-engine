package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Needlebug — Mirrodin #217
 * {4} · Artifact Creature — Insect · 2/2
 *
 * Flash
 * Protection from artifacts
 *
 * An artifact that hoses artifacts: flashed in as a surprise blocker it can't be blocked by, damaged
 * by, targeted by, or enchanted/equipped by anything artifact — including itself being blocked by
 * artifact creatures. Same [ProtectionScope.CardType] scope as Tel-Jilad Chosen, so all four
 * protection consequences fall out of the projection without per-card wiring.
 */
val Needlebug = card("Needlebug") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "Flash\nProtection from artifacts"

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Artifact")))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Paolo Parente"
        flavorText =
            "Near Tel-Jilad, the Tangle is almost silent, save for the trolls' chants and the skittering of needlebugs."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d54fbd8a-1e2a-450e-98f2-26bbc9e9ac79.jpg?1783944510"
    }
}
