package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greasewrench Goblin — Aetherdrift #132
 * {R} · Creature — Goblin Artificer · 2/1
 *
 * Exhaust — {2}{R}: Discard up to two cards, then draw that many cards. Put a +1/+1 counter on
 * this creature.
 *
 * `isExhaust = true` carries both halves of CR 702.177: the "Exhaust — " prefix and the
 * [com.wingedsheep.sdk.scripting.ActivationRestriction.Once] that makes it once per *object* — so a
 * Goblin that leaves and returns may exhaust again.
 *
 * The discard is genuinely optional (Scryfall ruling 2025-02-07: "You may activate this creature's
 * exhaust ability without discarding cards if you just want to put a +1/+1 counter on it"), and the
 * counter is a sibling of the loot rather than a rider on it — it lands even when zero cards are
 * discarded and zero drawn.
 */
val GreasewrenchGoblin = card("Greasewrench Goblin") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Artificer"
    power = 2
    toughness = 1
    oracleText = "Exhaust — {2}{R}: Discard up to two cards, then draw that many cards. Put a " +
        "+1/+1 counter on this creature. (Activate each exhaust ability only once.)"

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        isExhaust = true
        // The printed text goes on the *effect*, not the ability: `description` here would be a
        // `descriptionOverride` that swallows the auto-rendered "Exhaust — {2}{R}: " prefix, leaving
        // the action button with no cost and no exhaust marker.
        effect = Effects.Composite(
            effects = listOf(
                Patterns.Hand.discardUpToThenDraw(2),
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ),
            descriptionOverride = "Discard up to two cards, then draw that many cards. " +
                "Put a +1/+1 counter on this creature."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Alexandre Honoré"
        flavorText = "Killswitch finally had the right wrench for the job. Now she needed to remember what the job was."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8f0b123-fdb0-4f3e-ba78-fa155c227e20.jpg?1783907881"
    }
}
