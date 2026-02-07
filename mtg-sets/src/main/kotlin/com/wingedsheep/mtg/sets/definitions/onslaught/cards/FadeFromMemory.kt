package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Fade from Memory
 * {B}
 * Instant
 * Exile target card from a graveyard.
 * Cycling {B}
 */
val FadeFromMemory = card("Fade from Memory") {
    manaCost = "{B}"
    typeLine = "Instant"

    spell {
        target = Targets.CardInGraveyard
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE)
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "David Martin"
        flavorText = "\"Our scholars can defeat your warriors simply by forgetting they ever existed.\"\n—Ambassador Laquatus"
        imageUri = "https://cards.scryfall.io/large/front/5/6/56b34afa-0183-49aa-aa5f-03e070020136.jpg?1562915291"
    }
}
