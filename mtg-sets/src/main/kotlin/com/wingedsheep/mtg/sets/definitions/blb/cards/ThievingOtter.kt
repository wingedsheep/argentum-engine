package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Thieving Otter
 * {2}{U}
 * Creature — Otter
 * 2/2
 *
 * Whenever this creature deals damage to an opponent, draw a card.
 */
val ThievingOtter = card("Thieving Otter") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Otter"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals damage to an opponent, draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
            event = DealsDamageEvent(
                damageType = DamageType.Any,
                recipient = RecipientFilter.Opponent
            ),
            binding = TriggerBinding.SELF
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "390"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52a258be-39e3-4689-b2d0-7c353ce7d574.jpg?1721428090"
        inBooster = false
    }
}
