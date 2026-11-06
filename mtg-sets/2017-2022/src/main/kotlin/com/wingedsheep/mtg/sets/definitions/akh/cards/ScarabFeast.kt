package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scarab Feast
 * {B}
 * Instant
 * Exile up to three target cards from a single graveyard.
 * Cycling {B}
 *
 * Shred Memory's shape: "from a single graveyard" is the cross-target `sameOwner` constraint, "up
 * to three" is `count = 3, optional = true`, and the payoff iterates every chosen target.
 */
val ScarabFeast = card("Scarab Feast") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile up to three target cards from a single graveyard.\n" +
        "Cycling {B} ({B}, Discard this card: Draw a card.)"

    spell {
        targets(TargetFilter.CardInGraveyard, count = 3, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Tony Foti"
        flavorText = "\"The face of mediocrity is wrapped in anointed linen. Aspire to a better end.\"\n—Epnokep, vizier of Bontu"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7540895c-cbbc-46ac-ba96-882628358865.jpg?1783936499"
    }
}
