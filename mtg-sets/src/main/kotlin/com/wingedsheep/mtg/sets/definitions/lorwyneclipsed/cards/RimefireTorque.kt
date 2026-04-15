package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rimefire Torque
 * {1}{U}
 * Artifact
 * As Rimefire Torque enters, choose a creature type.
 * Whenever a permanent you control of the chosen type enters, put a charge counter on Rimefire Torque.
 * {T}, Remove three charge counters from Rimefire Torque: When you next cast an instant or sorcery
 * spell this turn, copy it. You may choose new targets for the copy.
 */
val RimefireTorque = card("Rimefire Torque") {
    manaCost = "{1}{U}"
    typeLine = "Artifact"
    oracleText = "As Rimefire Torque enters, choose a creature type.\n" +
            "Whenever a permanent you control of the chosen type enters, put a charge counter on Rimefire Torque.\n" +
            "{T}, Remove three charge counters from Rimefire Torque: When you next cast an instant or sorcery spell this turn, copy it. You may choose new targets for the copy."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Any.youControl().withChosenSubtype(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = AddCountersEffect(Counters.CHARGE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(Counters.CHARGE, 3))
        effect = Effects.CopyNextSpellCast(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8931b2-ff3f-4167-8a40-f33460c2d27e.jpg?1767952164"
    }
}
