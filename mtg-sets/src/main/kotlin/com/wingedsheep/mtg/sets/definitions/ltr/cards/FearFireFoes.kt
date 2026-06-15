package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fear, Fire, Foes!
 * {X}{R}
 * Sorcery
 *
 * Damage can't be prevented this turn. Fear, Fire, Foes! deals X damage to target creature and
 * 1 damage to each other creature with the same controller.
 *
 * - "Damage can't be prevented this turn." sets a turn-scoped flag (DamageCantBePreventedThisTurn)
 *   so every damage event this turn ignores prevention shields / protection's prevention clause
 *   (CR 615.6).
 * - The group clause deals 1 to each OTHER creature controlled by the target creature's controller:
 *   `targetPlayerControls(TargetController)` scopes to the target's controller and `otherThanTarget()`
 *   excludes the target creature itself.
 */
val FearFireFoes = card("Fear, Fire, Foes!") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Damage can't be prevented this turn. Fear, Fire, Foes! deals X damage to target creature and 1 damage to each other creature with the same controller."

    spell {
        target("target creature", Targets.Creature)
        effect = Effects.DamageCantBePreventedThisTurn()
            .then(Effects.DealDamage(DynamicAmount.XValue, EffectTarget.ContextTarget(0)))
            .then(
                Patterns.Group.dealDamageToAll(
                    1,
                    GroupFilter(
                        GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
                    ).otherThanTarget()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Hristo D. Chukov"
        flavorText = "\"Open, in the name of Mordor!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37be98a4-0cba-46b4-be93-a9805fe77160.jpg?1686968914"
    }
}
