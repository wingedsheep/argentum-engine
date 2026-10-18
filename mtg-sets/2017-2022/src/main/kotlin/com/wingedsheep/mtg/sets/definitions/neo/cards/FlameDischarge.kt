package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Flame Discharge — Kamigawa: Neon Dynasty #142 (canonical printing)
 * {X}{R} · Instant
 *
 * Flame Discharge deals X damage to target creature or planeswalker. If you controlled a modified
 * creature as you cast this spell, it deals X plus 2 damage instead.
 *
 * Whether you controlled a modified creature is frozen as the spell is cast (CR 601.2i) with
 * `captureAtCast`, so losing the modification before resolution doesn't drop the bonus.
 * "Modified" is [StatePredicate.IsModified] (CR 700.9).
 */
val FlameDischarge = card("Flame Discharge") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flame Discharge deals X damage to target creature or planeswalker. If you controlled a " +
        "modified creature as you cast this spell, it deals X plus 2 damage instead. (Equipment, Auras " +
        "you control, and counters are modifications.)"

    spell {
        captureAtCast(
            "controlledModified",
            Conditions.YouControl(GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified))
        )
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.If(
            condition = Conditions.CapturedAtCast("controlledModified"),
            then = Effects.DealDamage(DynamicAmounts.xValue() + 2, victim),
            otherwise = Effects.DealDamage(DynamicAmounts.xValue(), victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Joshua Raphael"
        flavorText = "\"What? Why would it need a cooling unit?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b44534f-d10c-4754-8585-5fe9db5fd4ea.jpg?1783923867"
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
    }
}
