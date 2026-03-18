package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Corrosive Ooze
 * {1}{G}
 * Creature — Ooze
 * 2/2
 * Whenever Corrosive Ooze blocks or becomes blocked by an equipped creature,
 * destroy all Equipment attached to that creature at end of combat.
 */
val CorrosiveOoze = card("Corrosive Ooze") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Ooze"
    power = 2
    toughness = 2
    oracleText = "Whenever Corrosive Ooze blocks or becomes blocked by an equipped creature, destroy all Equipment attached to that creature at end of combat."

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlockedBy(GameObjectFilter.Creature.equipped())
        effect = CreateDelayedTriggerEffect(
            step = Step.END_COMBAT,
            effect = Effects.DestroyAllEquipmentOnTarget(EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Daniel Ljunggren"
        flavorText = "Nothing tastes finer to an ooze than a priceless family heirloom."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d13deb9c-5985-4355-8716-ee1c7b54b8e2.jpg?1562743360"
    }
}
