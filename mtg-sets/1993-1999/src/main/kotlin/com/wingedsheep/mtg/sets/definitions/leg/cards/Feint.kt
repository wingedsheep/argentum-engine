package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feint — Legends #146
 * {R} · Instant
 *
 * Tap all creatures blocking target attacking creature. Prevent all combat damage that would be
 * dealt this turn by that creature and each creature blocking it.
 *
 * "Each creature blocking it" is read live off combat state ([GameObjectFilter.blockingEntity] over
 * the bound target) as Feint resolves; each blocker found then gets its own end-of-turn combat-damage
 * shield, so the affected set is locked in at resolution. Tapping a blocker doesn't remove it from
 * combat — it still deals combat damage — which is why the prevention half covers the blockers too.
 */
val Feint = card("Feint") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Tap all creatures blocking target attacking creature. Prevent all combat damage that " +
        "would be dealt this turn by that creature and each creature blocking it."

    spell {
        val attacker = target(TargetFilter.AttackingCreature)
        val blockers = GroupFilter(GameObjectFilter.Creature.blockingEntity(attacker))
        effect = Patterns.Group.tapAll(blockers) then
            Effects.PreventAllDamageDealtBy(attacker, scope = PreventionScope.CombatOnly) then
            Effects.ForEachInGroup(
                filter = blockers,
                effect = Effects.PreventAllDamageDealtBy(EffectTarget.IterationEntity, scope = PreventionScope.CombatOnly)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95b265bc-a94d-403b-8232-8fdfa0f8d9d5.jpg?1783948057"
    }
}
