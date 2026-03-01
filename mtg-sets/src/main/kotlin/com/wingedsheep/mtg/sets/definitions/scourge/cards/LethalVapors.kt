package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lethal Vapors
 * {2}{B}{B}
 * Enchantment
 * Whenever a creature enters, destroy it.
 * {0}: Destroy Lethal Vapors. You skip your next turn. Any player may activate this ability.
 */
val LethalVapors = card("Lethal Vapors") {
    manaCost = "{2}{B}{B}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature enters, destroy it.\n{0}: Destroy Lethal Vapors. You skip your next turn. Any player may activate this ability."

    triggeredAbility {
        trigger = TriggerSpec(
            ZoneChangeEvent(filter = GameObjectFilter.Creature, to = Zone.BATTLEFIELD),
            TriggerBinding.ANY
        )
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    activatedAbility {
        cost = Costs.Free
        effect = Effects.Destroy(EffectTarget.Self)
            .then(Effects.SkipNextTurn(EffectTarget.Controller))
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "John Avon"
        flavorText = "The vapors infiltrate every crevice, poison every lung, and snuff out every life."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f96acfea-009a-4ac9-8746-64f65199024f.jpg?1562536981"
    }
}
