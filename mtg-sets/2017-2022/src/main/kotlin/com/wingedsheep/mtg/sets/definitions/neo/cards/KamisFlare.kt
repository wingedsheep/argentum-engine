package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kami's Flare — Kamigawa: Neon Dynasty #150 (canonical printing)
 * {1}{R} · Instant
 *
 * Kami's Flare deals 3 damage to target creature or planeswalker. Kami's Flare also deals 2 damage
 * to that permanent's controller if you control a modified creature.
 *
 * Unlike Flame Discharge, the modified-creature check is made on resolution ("if you control"), so
 * it's a plain [Conditions.YouControl]. "Modified" is [StatePredicate.IsModified].
 */
val KamisFlare = card("Kami's Flare") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Kami's Flare deals 3 damage to target creature or planeswalker. Kami's Flare also deals " +
        "2 damage to that permanent's controller if you control a modified creature. (Equipment, Auras " +
        "you control, and counters are modifications.)"

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.DealDamage(3, permanent) then
            Effects.If(
                condition = Conditions.YouControl(
                    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified)
                ),
                then = Effects.DealDamage(2, EffectTarget.TargetController)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Tia Masic"
        flavorText = "\"The flames are a gift from my companion. They won't hurt me. You, on the other hand...\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bef5d58e-b490-4682-9a44-12cd61a94c0f.jpg?1783923863"
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
