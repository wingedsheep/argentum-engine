package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Memory's Journey
 * {1}{U}
 * Instant
 * Target player shuffles up to three target cards from their graveyard into their library.
 * Flashback {G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 */
val MemorysJourney = card("Memory's Journey") {
    manaCost = "{1}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Target player shuffles up to three target cards from their graveyard into their library.\nFlashback {G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"
    spell {
        val player = target(Targets.Player)
        val cards = targets(
            TargetFilter(GameObjectFilter.Any.ownedByTargetPlayer(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true,
        )
        effect = Effects.ForEachTargetOf(cards, Effects.Move(EffectTarget.IterationEntity, Zone.LIBRARY)) then
            Effects.ShuffleLibrary(player)
    }
    keywordAbility(KeywordAbility.flashback("{G}"))
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/265aaa73-1a1e-4282-a860-f7c422f21db3.jpg"
    }
}
