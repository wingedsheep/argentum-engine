package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCombatPhaseEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Éomer, Marshal of Rohan
 * {2}{R}{R}
 * Legendary Creature — Human Knight
 * 4/4
 *
 * Haste
 * Whenever one or more other attacking legendary creatures you control die, untap all creatures
 * you control. After this phase, there is an additional combat phase. This ability triggers only
 * once each turn.
 */
val EomerMarshalOfRohan = card("Éomer, Marshal of Rohan") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Haste\nWhenever one or more other attacking legendary creatures you control die, untap all creatures you control. After this phase, there is an additional combat phase. This ability triggers only once each turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().legendary().attacking(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER
        )
        oncePerTurn = true
        effect = Effects.Composite(
            listOf(
                // Untap all creatures you control
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    TapUntapEffect(EffectTarget.Self, tap = false)
                ),
                // After this phase, there is an additional combat phase
                AddCombatPhaseEffect
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Jesper Ejsing"
        flavorText = "\"Need brooks no delay, yet late is better than never.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bd31ce9-9551-4efe-8bd2-b97d8efbf75e.jpg?1686968857"
    }
}
