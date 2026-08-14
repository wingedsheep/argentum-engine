package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Colleen Wing, Street Samurai — Marvel Super Heroes #13 (uncommon)
 * {1}{W} · Legendary Creature — Human Samurai Hero · 2/2
 *
 * Whenever you cast a spell that targets a creature you control, put a +1/+1 counter on
 * Colleen Wing. Scry 1.
 *
 * Same trigger shape as Mockingbird, Ace Agent — [Triggers.youCastSpellTargeting], i.e. a
 * `SpellCastEvent` gated on `SpellCastPredicate.TargetsMatching(Creature.youControl())`
 * evaluated against the spell's chosen targets relative to Colleen's controller. It fires once
 * per qualifying spell regardless of how many of your creatures the spell targets, and Colleen
 * herself counts as "a creature you control", so a spell aimed at her triggers it too. The body
 * is the counter followed by [Effects.Scry]; the scry still happens if Colleen has left the
 * battlefield before the trigger resolves (the counter half simply does nothing).
 */
val ColleenWingStreetSamurai = card("Colleen Wing, Street Samurai") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai Hero"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a spell that targets a creature you control, put a +1/+1 " +
        "counter on Colleen Wing. Scry 1. (Look at the top card of your library. You may put " +
        "that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.youCastSpellTargeting(GameObjectFilter.Creature.youControl())
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.Scry(1),
        )
        description = "Whenever you cast a spell that targets a creature you control, put a " +
            "+1/+1 counter on Colleen Wing. Scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Jurijus Chitrovas"
        flavorText = "\"This sword is my heritage. I respect it by having the skill to wield it.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bffc505-608e-4a5b-9ed9-2321e4cab484.jpg?1783902974"
    }
}
