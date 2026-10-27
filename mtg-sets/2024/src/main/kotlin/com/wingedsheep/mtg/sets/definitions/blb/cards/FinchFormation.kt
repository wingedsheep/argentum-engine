package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Finch Formation
 * {2}{U}
 * Creature — Bird Scout
 * 2/2
 *
 * Offspring {3}
 * Flying
 * When this creature enters, target creature you control gains flying until end of turn.
 */
val FinchFormation = card("Finch Formation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Scout"
    power = 2
    toughness = 2
    oracleText = "Offspring {3} (You may pay an additional {3} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nFlying\nWhen this creature enters, target creature you control gains flying until end of turn."

    keywords(Keyword.FLYING)

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.offspring("{3}"))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // ETB: target creature you control gains flying until end of turn
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Rhonda Libbey"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c671eab-d1ef-4d79-94eb-8b85f0d18699.jpg?1721426087"
    }
}
