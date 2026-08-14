package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Elektra, Daughter of the Hand — Marvel Super Heroes #97
 * {2}{B}{B} · Legendary Creature — Human Ninja Villain · 3/3
 *
 * Sneak {1}{B}{B} (You may cast this spell for {1}{B}{B} if you also return an unblocked attacker
 * you control to hand during the declare blockers step. She enters tapped and attacking.)
 * When Elektra enters, destroy target creature an opponent controls with power 3 or less.
 *
 * Implementation notes:
 * - Sneak is the existing TMT keyword ([com.wingedsheep.sdk.core.Keyword.SNEAK], CR 702.190) via
 *   the `sneak(cost)` helper; all of the alternative-cost behavior — declare-blockers timing, the
 *   unblocked-attacker bounce, and entering tapped and attacking — lives in the engine.
 * - The ETB is a plain targeted destroy. "with power 3 or less" is part of the *target*
 *   restriction, so it's a `powerAtMost(3)` narrowing of the opponent-controls creature filter
 *   rather than a resolution-time condition: an opponent's creature that grows out of range in
 *   response makes the ability fizzle for having no legal target.
 */
val ElektraDaughterOfTheHand = card("Elektra, Daughter of the Hand") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Ninja Villain"
    power = 3
    toughness = 3
    oracleText = "Sneak {1}{B}{B} (You may cast this spell for {1}{B}{B} if you also return an " +
        "unblocked attacker you control to hand during the declare blockers step. She enters " +
        "tapped and attacking.)\n" +
        "When Elektra enters, destroy target creature an opponent controls with power 3 or less."

    sneak("{1}{B}{B}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "target creature an opponent controls with power 3 or less",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls.powerAtMost(3)),
        )
        effect = Effects.Destroy(victim)
        description = "When Elektra enters, destroy target creature an opponent controls with " +
            "power 3 or less."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Bastien L. Deharme"
        flavorText = "\"I am Elektra Natchios. Not even the stars are safe in the sky.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac3e586c-d654-4631-beda-a5e29cf04717.jpg?1783902943"
    }
}
