package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arena of the Ancients
 * {3}
 * Artifact
 *
 * Legendary creatures don't untap during their controllers' untap steps.
 * When this artifact enters, tap all legendary creatures.
 */
val ArenaOfTheAncients = card("Arena of the Ancients") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Legendary creatures don't untap during their controllers' untap steps.\n" +
        "When this artifact enters, tap all legendary creatures."

    // "Legendary creatures don't untap during their controllers' untap steps." — a continuous
    // untap-step restriction over every legendary creature, the same shape as Magnetic Mountain.
    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Creature.legendary())
        )
    }

    // "When this artifact enters, tap all legendary creatures."
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.legendary()),
            Effects.Tap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9337996e-a119-4529-b422-f6d286c78e3f.jpg?1783948029"
    }
}
