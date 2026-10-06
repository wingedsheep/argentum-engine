package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Innocent Bystander — Murders at Karlov Manor #133
 * {1}{R} · Creature — Goblin Citizen · 2/1
 *
 * Whenever this creature is dealt 3 or more damage, investigate.
 *
 * A 2/1 that turns overkill into a card: anything that bothers to point a real burn spell or a
 * big blocker at it hands you a Clue on the way out.
 *
 * The "3 or more" gate is measured **per damage event**, not cumulatively over the turn (its
 * ruling: "all at once"). Simultaneous damage is one event, so being blocked by two 2/2s is 4
 * damage at once and fires it — the detector folds simultaneous combat damage into one
 * "is dealt damage" trigger carrying the total — while a 2-damage ping followed by another does
 * not. `Triggers.self.isDealtDamage()` is the SELF "whenever this is dealt damage" event and the
 * threshold rides on it as a `triggerRestriction` comparing that event's damage
 * ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]) against 3 with [ComparisonOperator.GTE] — the same
 * idiom Spinneret and Spiderling uses on the outgoing side.
 *
 * Lethal damage doesn't suppress the trigger: state-based actions kill the Bystander before the
 * ability resolves, but the trigger was already detected off the damage event (CR 603.10), and
 * `DamageTriggerDetector.detectDamageReceivedTriggers` deliberately looks the creature up after
 * it has left the battlefield for exactly this case. Dying to a Lightning Bolt still investigates.
 */
val InnocentBystander = card("Innocent Bystander") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Citizen"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature is dealt 3 or more damage, investigate. (Create a Clue " +
        "token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggerDamageAmount(),
            ComparisonOperator.GTE,
            3,
        )
        effect = Effects.Investigate()
        description = "Whenever this creature is dealt 3 or more damage, investigate."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Warren Mahy"
        flavorText = "\"Whoops, this isn't my street! Don't mind me! I'll just be on my way while " +
            "you enjoy your . . . very sharp knives.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/085f4595-4ae5-428e-a934-e918774df6fd.jpg?1783912882"

        ruling(
            "2024-02-02",
            "Innocent Bystander's ability triggers only if it's dealt 3 or more damage all at " +
                "once. It doesn't trigger if damage that adds up to 3 or more is dealt to it at " +
                "different times."
        )
    }
}
