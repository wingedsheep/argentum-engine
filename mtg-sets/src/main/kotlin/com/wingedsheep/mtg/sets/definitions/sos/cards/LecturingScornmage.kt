package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lecturing Scornmage
 * {B}
 * Creature — Human Warlock
 * 1/1
 * Repartee — Whenever you cast an instant or sorcery spell that targets a creature, put a +1/+1
 * counter on this creature.
 *
 * "Repartee" is an ability word (flavor only). The trigger is a standard
 * `youCastSpell(InstantOrSorcery)` narrowed by `CardPredicate.TargetsMatching(Creature)` so it
 * fires only when one of the cast spell's chosen targets is a creature.
 */
val LecturingScornmage = card("Lecturing Scornmage") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 1
    oracleText = "Repartee — Whenever you cast an instant or sorcery spell that targets a " +
        "creature, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery.targetsMatching(GameObjectFilter.Creature)
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Tuan Duong Chu"
        flavorText = "\"I expected better from you.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad07091e-8c24-43af-8ce8-031847bcaf30.jpg?1775937516"
    }
}
