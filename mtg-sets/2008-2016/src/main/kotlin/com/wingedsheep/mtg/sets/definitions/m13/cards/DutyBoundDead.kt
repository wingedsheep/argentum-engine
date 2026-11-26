package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Duty-Bound Dead — Magic 2013 #92 (canonical printing)
 * {B}
 * Creature — Skeleton
 * 0/2
 * Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)
 * {3}{B}: Regenerate this creature.
 */
val DutyBoundDead = card("Duty-Bound Dead") {
    manaCost = "{B}"
    typeLine = "Creature — Skeleton"
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)\n" +
        "{3}{B}: Regenerate this creature. (The next time this creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"
    power = 0
    toughness = 2

    keywords(Keyword.EXALTED)

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5150aa90-1284-4261-8625-2528139f0015.jpg?1783940496"
    }
}
