package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FaceDownLookScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Revealing Wind
 * {2}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn. You may look at each face-down
 * creature that's attacking or blocking.
 *
 * The look is private to the caster: each face-down attacker or blocker is marked revealed to the
 * spell's controller only (the same look Smoke Teller takes at one target), visited one by one so
 * face-down creatures outside combat stay hidden. The set is read as the spell resolves.
 */
val RevealingWind = card("Revealing Wind") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn. You may look at each face-down creature that's attacking or blocking."

    spell {
        effect = Effects.PreventAllCombatDamage() then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.faceDown().attackingOrBlocking()),
                Effects.LookAtFaceDown(EffectTarget.IterationEntity, FaceDownLookScope.SINGLE_TARGET)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Phill Simmer"
        flavorText = "\"The sands obscure the vision of others, but clarify ours.\"\n—Faiso, Dromoka commander"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36bdd624-e412-4ec8-9929-e1f6b4720e82.jpg?1783938577"
        ruling("2015-02-25", "You can cast Revealing Wind even if there are no face-down creatures attacking or blocking. The damage-prevention effect will still apply.")
    }
}
