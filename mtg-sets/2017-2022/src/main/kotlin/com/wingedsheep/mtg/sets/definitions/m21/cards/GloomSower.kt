package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gloom Sower
 * {5}{B}{B}
 * Creature — Horror
 * 8/6
 * Whenever this creature becomes blocked by a creature, that creature's controller loses 2 life
 * and you gain 2 life.
 *
 * The filtered `becomesBlocked(by = …)` fires once per blocking creature with that blocker as the
 * triggering entity; `ControllerOfTriggeringEntity` falls back to the blocker's last-known
 * controller if it has left the battlefield before resolution (ruling).
 */
val GloomSower = card("Gloom Sower") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 8
    toughness = 6
    oracleText = "Whenever this creature becomes blocked by a creature, that creature's controller " +
        "loses 2 life and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.LoseLife(2, EffectTarget.ControllerOfTriggeringEntity) then Effects.GainLife(2)
        description = "Whenever this creature becomes blocked by a creature, that creature's " +
            "controller loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Chris Cold"
        flavorText = "\"First, the darkness fell. Terror came next. After that, only despair remained.\"\n" +
            "—The Tragedy of Morein"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1b4f7ec-ea2e-4d90-98cd-0c92bd9f64c1.jpg?1783930706"
        ruling("2020-06-23", "If the creature that blocked Gloom Sower leaves the battlefield before Gloom Sower's triggered ability resolves, the player who controlled it before it left is the player who loses 2 life.")
    }
}
