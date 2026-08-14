package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lizard, Connors's Curse
 * {2}{G}{G}
 * Legendary Creature — Lizard Villain
 * 5/5
 *
 * Trample
 * Lizard Formula — When Lizard, Connors's Curse enters, up to one other target
 * creature loses all abilities and becomes a green Lizard creature with base power
 * and toughness 4/4.
 *
 * "Lizard Formula" is an ability word (flavor label), so it adds no rules meaning.
 * The transform is modeled as two floating continuous effects keyed to the target,
 * both [Duration.Permanent] (it lasts indefinitely — it does not expire when Lizard
 * leaves the battlefield):
 *  - [RemoveAllAbilitiesEffect] (Layer 6) — "loses all abilities"
 *  - [Effects.BecomeCreature] — sets base P/T 4/4 (Layer 7b), replaces creature
 *    subtypes with Lizard (Layer 4) and sets color to green (Layer 5).
 *
 * The target is "up to one OTHER target creature": optional (the controller may
 * choose no target) and [TargetFilter.OtherCreature] excludes Lizard itself.
 */
val LizardConnorssCurse = card("Lizard, Connors's Curse") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Lizard Villain"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Lizard Formula — When Lizard, Connors's Curse enters, up to one other target creature " +
        "loses all abilities and becomes a green Lizard creature with base power and toughness 4/4."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "up to one other target creature",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreature)
        )
        effect = Effects.Composite(
            RemoveAllAbilitiesEffect(t, Duration.Permanent),
            Effects.BecomeCreature(
                target = t,
                power = 4,
                toughness = 4,
                creatureTypes = setOf("Lizard"),
                colors = setOf("GREEN"),
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Steve Prescott"
        flavorText = "Dr. Curt Connors used lizard DNA to regrow his missing arm, but it didn't stop there."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6add5d2a-950e-4bee-9850-e68f5f6d6142.jpg?1783905326"
    }
}
