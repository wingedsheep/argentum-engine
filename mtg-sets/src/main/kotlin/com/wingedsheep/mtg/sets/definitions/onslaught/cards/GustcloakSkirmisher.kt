package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect

/**
 * Gustcloak Skirmisher
 * {3}{W}
 * Creature — Bird Soldier
 * 2/3
 * Flying
 * Whenever Gustcloak Skirmisher becomes blocked, you may untap it and remove it from combat.
 */
val GustcloakSkirmisher = card("Gustcloak Skirmisher") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = MayEffect(
            Effects.Untap(EffectTarget.Self) then Effects.RemoveFromCombat(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Dan Frazier"
        flavorText = "\"They're trained in the art of pressing their luck.\""
        imageUri = "https://cards.scryfall.io/large/front/c/b/cbbff06c-5f92-4320-8b70-df3c8344f600.jpg?1562943121"
    }
}
