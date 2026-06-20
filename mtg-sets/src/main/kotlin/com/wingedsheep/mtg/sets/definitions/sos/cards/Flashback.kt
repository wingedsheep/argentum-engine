package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Flashback — Secrets of Strixhaven #115
 * {R} · Instant
 *
 * Target instant or sorcery card in your graveyard gains flashback until end of turn. The
 * flashback cost is equal to its mana cost. (You may cast that card from your graveyard for its
 * flashback cost. Then exile it.)
 *
 * [Effects.GrantFlashback] with a null cost grants flashback whose cost equals the targeted
 * card's own mana cost, until end of turn (Archmage's Newt's runtime grant).
 */
val Flashback = card("Flashback") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target instant or sorcery card in your graveyard gains flashback until end of " +
        "turn. The flashback cost is equal to its mana cost. (You may cast that card from your " +
        "graveyard for its flashback cost. Then exile it.)"

    spell {
        target = TargetObject(filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
        effect = Effects.GrantFlashback(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Flavio Greco Paglia"
        flavorText = "Quintorius found that every new discovery motivated him to keep exploring."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b832fda-d7c4-4566-884c-2a8b6da15488.jpg?1775937742"
    }
}
