package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phlage, Titan of Fire's Fury
 * {1}{R}{W}
 * Legendary Creature — Elder Giant
 * 6/6
 * When Phlage enters, sacrifice it unless it escaped.
 * Whenever Phlage enters or attacks, it deals 3 damage to any target and you gain 3 life.
 * Escape—{R}{R}{W}{W}, Exile five other cards from your graveyard.
 *
 * "Unless it escaped" is checked as the first trigger resolves (not an intervening "if"), so it is
 * an [Effects.If] over [Conditions.Escaped]. The damage trigger fires on entry either way — a
 * hard-cast Phlage still deals 3 and gains 3 on its way to the graveyard.
 */
val PhlageTitanOfFiresFury = card("Phlage, Titan of Fire's Fury") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Elder Giant"
    power = 6
    toughness = 6
    oracleText = "When Phlage enters, sacrifice it unless it escaped.\n" +
        "Whenever Phlage enters or attacks, it deals 3 damage to any target and you gain 3 life.\n" +
        "Escape—{R}{R}{W}{W}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(Conditions.Not(Conditions.Escaped), SacrificeSelfEffect)
        description = "When Phlage enters, sacrifice it unless it escaped."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.GainLife(3, EffectTarget.Controller)
        description = "Whenever Phlage enters, it deals 3 damage to any target and you gain 3 life."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.GainLife(3, EffectTarget.Controller)
        description = "Whenever Phlage attacks, it deals 3 damage to any target and you gain 3 life."
    }

    keywordAbility(KeywordAbility.escape("{R}{R}{W}{W}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e419cd0b-2449-4cc5-9ead-b9e45e271700.jpg?1783911247"

        ruling(
            "2024-06-07",
            "Phlage's first ability causes you to sacrifice it if you didn't cast it, or if it was cast " +
                "using any permission other than an escape ability."
        )
        ruling(
            "2024-06-07",
            "Phlage's second ability triggers when it enters the battlefield, even if it didn't escape."
        )
        ruling(
            "2024-06-07",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. It can escape again."
        )
    }
}
