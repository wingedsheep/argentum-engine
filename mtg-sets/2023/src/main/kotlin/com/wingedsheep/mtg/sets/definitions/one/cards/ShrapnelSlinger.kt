package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shrapnel Slinger
 * {1}{R}
 * Artifact Creature — Phyrexian Beast
 * 2/2
 *
 * When this creature enters, you may sacrifice a creature. When you do, destroy target
 * artifact an opponent controls.
 *
 * ETB [ReflexiveTriggerEffect] (the Glorifier of Suffering idiom): the optional action is a
 * resolution-time choice of a creature you control — the Slinger itself included, since the
 * text says "a creature", not "another" — which is then sacrificed. Only "when you do" does the
 * reflexive trigger go on the stack and target an artifact an opponent controls.
 */
val ShrapnelSlinger = card("Shrapnel Slinger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Phyrexian Beast"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice a creature. When you do, destroy target artifact an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.Creature.youControl()),
                    nonTargeting = true,
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice a creature. When you do, destroy target artifact an opponent controls."
        ) {
            val artifact = target(TargetFilter(GameObjectFilter.Artifact.opponentControls()))
            effect = Effects.Destroy(artifact)
        }
        description = "When this creature enters, you may sacrifice a creature. When you do, destroy target artifact an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Joseph Meehan"
        flavorText = "\"They may have unlimited mites, but that just means we have unlimited ammunition.\"\n—Urabrask"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6147bdc-4e02-48be-9ab9-c212f70d9194.jpg?1783918025"
    }
}
