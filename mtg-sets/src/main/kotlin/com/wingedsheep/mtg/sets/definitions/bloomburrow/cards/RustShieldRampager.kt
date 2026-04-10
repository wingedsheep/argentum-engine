package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Rust-Shield Rampager
 * {3}{G}
 * Creature — Raccoon Warrior
 * 4/4
 *
 * Offspring {2}
 * This creature can't be blocked by creatures with power 2 or less.
 */
val RustShieldRampager = card("Rust-Shield Rampager") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Raccoon Warrior"
    power = 4
    toughness = 4
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, " +
        "when this creature enters, create a 1/1 token copy of it.)\n" +
        "This creature can't be blocked by creatures with power 2 or less."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c96b01f5-83de-4237-a68d-f946c53e31a6.jpg?1721426907"
    }
}
