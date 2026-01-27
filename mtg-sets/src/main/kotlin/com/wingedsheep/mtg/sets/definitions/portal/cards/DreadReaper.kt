package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect

/**
 * Dread Reaper
 * {3}{B}{B}{B}
 * Creature — Horror
 * 6/5
 * Flying
 * When Dread Reaper enters the battlefield, you lose 5 life.
 */
val DreadReaper = card("Dread Reaper") {
    manaCost = "{3}{B}{B}{B}"
    typeLine = "Creature — Horror"
    power = 6
    toughness = 5
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LoseLifeEffect(5, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb25d674-11f3-42d2-ba2f-e9a5d55a7852.jpg"
    }
}
