package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Defiant Ogre
 * {5}{R}
 * Creature — Ogre Warrior
 * 3/5
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Destroy target artifact.
 */
val DefiantOgre = card("Defiant Ogre") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 5
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Destroy target artifact."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Craig J Spearing"
        flavorText = "\"I have no clan, but I still have purpose.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09cfd3d5-1b44-4dc7-b35d-703f3a75da88.jpg"
    }
}
