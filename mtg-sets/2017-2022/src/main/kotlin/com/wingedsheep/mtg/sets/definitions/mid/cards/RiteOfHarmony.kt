package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Rite of Harmony
 * {G}{W}
 * Instant
 * Whenever a creature or enchantment you control enters this turn, draw a card.
 * Flashback {2}{G}{W}
 *
 * A turn-bounded, filter-scoped delayed triggered ability (the First Day of Class shape):
 * `fireOnce = false` so it draws for every qualifying permanent that enters this turn, and
 * `expiry = EndOfTurn` is the printed "this turn". An enchantment creature matches the filter
 * once, so it draws one card, not two.
 */
val RiteOfHarmony = card("Rite of Harmony") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Whenever a creature or enchantment you control enters this turn, draw a card.\n" +
        "Flashback {2}{G}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.CreatureOrEnchantment.youControl()).enters(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.DrawCards(1)
        )
    }

    keywordAbility(KeywordAbility.flashback("{2}{G}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Rovina Cai"
        flavorText = "Nature bends to the witches' call."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb1a16f9-13d4-4188-8e1e-0f2394349c7a.jpg?1783925556"
    }
}
