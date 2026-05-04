package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bushy Bodyguard
 * {1}{G}
 * Creature — Squirrel Warrior
 * 2/1
 *
 * Offspring {2}
 * When this creature enters, you may forage. If you do, put two +1/+1 counters on it.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 */
val BushyBodyguard = card("Bushy Bodyguard") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Squirrel Warrior"
    power = 2
    toughness = 1
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature enters, you may forage. If you do, put two +1/+1 counters on it. (To forage, exile three cards from your graveyard or sacrifice a Food.)"

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // When this creature enters, you may forage. If you do, put two +1/+1 counters on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = EffectPatterns.forage(
                afterEffect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
            ),
            descriptionOverride = "You may forage",
            hint = "Exile three cards from your graveyard or sacrifice a Food"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0de60cf7-fa82-4b6f-9f88-6590fba5c863.jpg?1721426775"
    }
}
