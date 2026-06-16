package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emergent Haunting — Outlaws of Thunder Junction #46
 * {1}{U} · Enchantment · Uncommon
 *
 * At the beginning of your end step, if you haven't cast a spell from your hand this turn and
 * this enchantment isn't a creature, it becomes a 3/3 Spirit creature with flying in addition
 * to its other types.
 * {2}{U}: Surveil 1.
 *
 * The animation is a permanent [Effects.BecomeCreature] on the source: it adds the CREATURE
 * type (no `removeTypes`, so "in addition to its other types" — it stays an Enchantment), sets
 * base 3/3, adds the Spirit subtype, and grants flying. No duration (CR ruling 2024-04-12: "the
 * ability doesn't have a duration … it remains a creature indefinitely").
 *
 * Intervening-if (CR 603.4): the [triggerCondition] is checked both when the ability would
 * trigger and again as it resolves. Both clauses are ANDed — "haven't cast a spell from your
 * hand this turn" (via the spells-from-hand tally) and "this enchantment isn't a creature" (the
 * source is currently noncreature, read from projected state so it self-disables once animated).
 *
 * {2}{U}: Surveil 1 composes the standard `Patterns.Library.surveil` pipeline.
 */
val EmergentHaunting = card("Emergent Haunting") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if you haven't cast a spell from your hand this turn " +
        "and this enchantment isn't a creature, it becomes a 3/3 Spirit creature with flying in addition " +
        "to its other types.\n" +
        "{2}{U}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.All(
            Conditions.Not(Conditions.YouCastSpellsThisTurn(1, fromZone = Zone.HAND)),
            Conditions.SourceMatches(GameObjectFilter.Noncreature)
        )
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Spirit"),
            duration = Duration.Permanent
        )
        description = "At the beginning of your end step, if you haven't cast a spell from your hand " +
            "this turn and this enchantment isn't a creature, it becomes a 3/3 Spirit creature with " +
            "flying in addition to its other types."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Patterns.Library.surveil(1)
        description = "{2}{U}: Surveil 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/623053a8-7abe-45ec-9f26-97e1c037120b.jpg?1712355411"
    }
}
