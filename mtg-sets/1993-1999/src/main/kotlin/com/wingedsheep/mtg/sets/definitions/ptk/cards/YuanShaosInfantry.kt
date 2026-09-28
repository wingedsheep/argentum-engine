package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yuan Shao's Infantry
 * {3}{R}
 * Creature — Human Soldier
 * 2/2
 * Whenever this creature attacks alone, this creature can't be blocked this combat.
 */
val YuanShaosInfantry = card("Yuan Shao's Infantry") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks alone, this creature can't be blocked this combat."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.Alone))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Lin Yan"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2623481-cee3-4a9d-933b-235fcde9a27b.jpg?1783946103"
    }
}
