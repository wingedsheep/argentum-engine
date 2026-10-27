package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Coruscation Mage
 * {1}{R}
 * Creature — Otter Wizard
 * 2/2
 *
 * Offspring {2} (You may pay an additional {2} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 * Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent.
 */
val CoruscationMage = card("Coruscation Mage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Otter Wizard"
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, " +
        "when this creature enters, create a 1/1 token copy of it.)\n" +
        "Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent."
    power = 2
    toughness = 2

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.offspring("{2}"))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // Whenever you cast a noncreature spell, deal 1 damage to each opponent
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc2c1de0-6233-469a-be72-a050b97d2c8f.jpg?1721426600"
    }
}
