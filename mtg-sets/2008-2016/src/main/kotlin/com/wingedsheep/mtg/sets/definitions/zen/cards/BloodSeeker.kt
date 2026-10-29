package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Seeker {1}{B} — Creature — Vampire Shaman 1/1
 *
 * Whenever a creature an opponent controls enters, you may have that player lose 1 life.
 */
val BloodSeeker = card("Blood Seeker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever a creature an opponent controls enters, you may have that player lose 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).enters()
        optional = true
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity)
        description = "Whenever a creature an opponent controls enters, you may have that player lose 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Greg Staples"
        flavorText = "A drop now is all he needs to find you later."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1abc9e8-9ecf-4665-9ea5-ee18ab83c148.jpg?1783942157"
        ruling("2011-09-22", "Life loss is not the same as damage. Blood Seeker's ability will not cause creatures with bloodthirst to enter with +1/+1 counters.")
    }
}
