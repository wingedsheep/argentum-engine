package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Jeskai Student
 * {1}{W}
 * Creature — Human Monk
 * 1/3
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val JeskaiStudent = card("Jeskai Student") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.PROWESS)

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = ModifyStatsEffect(
            powerModifier = 1,
            toughnessModifier = 1,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "David Gaillet"
        flavorText = "Discipline is the first pillar of the Jeskai Way. Each member of the clan trains in a weapon, perfecting its use over a lifetime."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9007c528-0107-40fd-a97e-e5576f297eb2.jpg?1562790315"
    }
}
