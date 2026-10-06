package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan's Rage — Dragons of Tarkir #153 (canonical printing)
 * {4}{R} · Instant
 *
 * Sarkhan's Rage deals 5 damage to any target. If you control no Dragons, Sarkhan's Rage deals
 * 2 damage to you.
 *
 * "Dragons" is any permanent with the subtype, not just creatures; the check is made at
 * resolution (per the ruling), after the 5 damage is dealt.
 */
val SarkhansRage = card("Sarkhan's Rage") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Sarkhan's Rage deals 5 damage to any target. If you control no Dragons, " +
        "Sarkhan's Rage deals 2 damage to you."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(5, t) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON), negate = true),
            then = Effects.DealDamage(2, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Chris Rahn"
        flavorText = "The people of Tarkir speak of an ancient legend, of the dragon-man called Sarkhan " +
            "who was greatest of all khans."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4787924f-3186-4e18-b53c-dd67c5f42220.jpg?1783938586"
        ruling("2015-02-25", "Sarkhan's Rage checks whether you control a Dragon as it resolves.")
    }
}
