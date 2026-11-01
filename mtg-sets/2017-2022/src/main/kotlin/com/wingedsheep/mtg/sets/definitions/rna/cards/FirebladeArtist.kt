package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fireblade Artist
 * {B}{R}
 * Creature — Human Shaman
 * 2/2
 *
 * Haste
 * At the beginning of your upkeep, you may sacrifice a creature. When you do, this creature
 * deals 2 damage to target opponent or planeswalker.
 *
 * Upkeep [ReflexiveTriggerEffect] (the Shrapnel Slinger idiom): the upkeep trigger has no
 * target; while it resolves you may choose a creature you control (the Artist itself included —
 * "a creature", not "another") and sacrifice it. Only "when you do" does the reflexive trigger
 * go on the stack and target an opponent or planeswalker.
 */
val FirebladeArtist = card("Fireblade Artist") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "At the beginning of your upkeep, you may sacrifice a creature. When you do, this creature " +
        "deals 2 damage to target opponent or planeswalker."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.Creature.youControl()),
                    nonTargeting = true,
                    prompt = "Choose a creature to sacrifice"
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice a creature. When you do, this creature deals 2 damage " +
                "to target opponent or planeswalker."
        ) {
            val victim = target(Targets.OpponentOrPlaneswalker)
            effect = Effects.DealDamage(2, victim)
        }
        description = "At the beginning of your upkeep, you may sacrifice a creature. When you do, this " +
            "creature deals 2 damage to target opponent or planeswalker."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Steve Argyle"
        flavorText = "\"Let's find out what kind of audience you are. Festive? Fearless?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21e1161f-bd2c-45a7-a86b-3b2e5210f148.jpg?1783933650"

        ruling(
            "2019-01-25",
            "Fireblade Artist's triggered ability goes on the stack without a target. While that ability is " +
                "resolving, you may sacrifice a creature. When you do, the reflexive triggered ability triggers " +
                "and you pick a target opponent or planeswalker to be dealt damage. This is different from " +
                "effects that say \"If you do . . .\" in that players may take actions after you've sacrificed " +
                "the creature but before damage is dealt."
        )
    }
}
