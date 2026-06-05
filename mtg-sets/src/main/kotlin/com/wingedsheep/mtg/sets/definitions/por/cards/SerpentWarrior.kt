// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Serpent Warrior
 * {2}{B}
 * Creature — Snake Warrior
 * 3/3
 * When this creature enters, you lose 3 life.
 */
val SerpentWarrior = card("Serpent Warrior") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake Warrior"
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LoseLifeEffect(3, EffectTarget.Controller)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Roger Raupp"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c364fd06-64c5-45f6-8ed5-64f44a1e8bda.jpg"
    }
}
