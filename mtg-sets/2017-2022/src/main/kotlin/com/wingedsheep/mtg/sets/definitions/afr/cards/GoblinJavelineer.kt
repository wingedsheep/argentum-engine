package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Javelineer
 * {R}
 * Creature — Goblin Warrior
 * 1/1
 *
 * Haste
 * Whenever this creature becomes blocked, it deals 1 damage to target creature blocking it.
 *
 * The target uses the source-relative `blockingSource()` filter (as Arc Spitter does), so only
 * creatures blocking the Javelineer itself are legal — not blockers of other attackers.
 */
val GoblinJavelineer = card("Goblin Javelineer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Haste\nWhenever this creature becomes blocked, it deals 1 damage to target creature blocking it."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        val blocker = target(TargetFilter(GameObjectFilter.Creature.blockingSource()))
        effect = Effects.DealDamage(amount = 1, target = blocker, damageSource = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Mike Jordana"
        flavorText = "Hate gets you only so far. A pointy stick can close the rest of the distance."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d32b3f1b-40cb-413d-adb9-0809e4a2abdd.jpg?1783926479"
    }
}
