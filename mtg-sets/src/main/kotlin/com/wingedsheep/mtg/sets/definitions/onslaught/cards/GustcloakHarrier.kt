package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Gustcloak Harrier
 * {1}{W}{W}
 * Creature — Bird Soldier
 * 2/2
 * Flying
 * Whenever Gustcloak Harrier becomes blocked, you may untap it and remove it from combat.
 */
val GustcloakHarrier = card("Gustcloak Harrier") {
    manaCost = "{1}{W}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever Gustcloak Harrier becomes blocked, you may untap it and remove it from combat."
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = MayEffect(
            Effects.Untap(EffectTarget.Self) then Effects.RemoveFromCombat(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Dan Frazier"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/b/5/b5ff5c7d-7823-4d1e-8abb-77e2d8126996.jpg?1562937868"
    }
}
