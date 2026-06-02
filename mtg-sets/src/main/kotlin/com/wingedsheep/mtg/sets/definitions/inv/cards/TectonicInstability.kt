package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Tectonic Instability
 * {2}{R}
 * Enchantment
 * Whenever a land enters, tap all lands its controller controls.
 *
 * Triggers on any land entering (ANY binding). The effect taps every land controlled by the
 * entering land's controller, resolved via [EffectTarget.ControllerOfTriggeringEntity] threaded
 * into the group filter's controller predicate.
 */
val TectonicInstability = card("Tectonic Instability") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a land enters, tap all lands its controller controls."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Land,
            binding = TriggerBinding.ANY
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Land.targetPlayerControls(EffectTarget.ControllerOfTriggeringEntity)
            ),
            effect = TapUntapEffect(target = EffectTarget.Self, tap = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0476cc6b-ecc6-44d6-9f44-a90d4ee85daa.jpg?1618485193"
    }
}
