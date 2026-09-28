package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Battering Ram
 * {2}
 * Artifact Creature — Construct
 * 1/1
 *
 * At the beginning of combat on your turn, this creature gains banding until end of combat.
 * Whenever this creature becomes blocked by a Wall, destroy that Wall at end of combat.
 *
 * The banding half composes from existing primitives: a `Triggers.you.beginningOf(Step.BEGIN_COMBAT)` trigger granting
 * [Keyword.BANDING] to itself for [Duration.EndOfCombat]. The Wall half uses the blocker-filtered
 * `Triggers.self.becomesBlocked(by = Wall)` trigger — for SELF binding the filter applies to
 * the **blocker**, firing once per blocking Wall with the Wall as `TriggeringEntity` — then a
 * [CreateDelayedTriggerEffect] at [Step.END_COMBAT] destroys that Wall (same shape as Serpentine
 * Basilisk's "destroy that creature at end of combat").
 */
val BatteringRam = card("Battering Ram") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "At the beginning of combat on your turn, this creature gains banding until end " +
        "of combat.\n" +
        "Whenever this creature becomes blocked by a Wall, destroy that Wall at end of combat."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.GrantKeyword(Keyword.BANDING, EffectTarget.Self, Duration.EndOfCombat)
        description = "At the beginning of combat on your turn, this creature gains banding until end of combat."
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature.withSubtype(Subtype.WALL))
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature becomes blocked by a Wall, destroy that Wall at end of combat."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Jeff A. Menges"
        flavorText = "By the time Mishra was defeated, no mage was foolish enough to rely heavily on walls."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a69e35-d209-41c0-aa3c-c78414617075.jpg?1562947317"
    }
}
