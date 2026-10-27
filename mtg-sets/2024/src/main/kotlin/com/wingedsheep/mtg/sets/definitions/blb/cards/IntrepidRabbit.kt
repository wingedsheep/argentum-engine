package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Intrepid Rabbit
 * {2}{W}
 * Creature — Rabbit Soldier
 * 3/2
 *
 * Offspring {1} (You may pay an additional {1} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 *
 * When this creature enters, target creature you control gets +1/+1 until end of turn.
 */
val IntrepidRabbit = card("Intrepid Rabbit") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Soldier"
    power = 3
    toughness = 2
    oracleText = "Offspring {1} (You may pay an additional {1} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature enters, target creature you control gets +1/+1 until end of turn."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.offspring("{1}"))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // ETB: target creature you control gets +1/+1 until end of turn
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Artur Treffner"
        flavorText = "\"I'll light the way, but we must walk it together.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d70b99d-c8bf-4a56-8957-cf587fe60b81.jpg?1721425856"
    }
}
