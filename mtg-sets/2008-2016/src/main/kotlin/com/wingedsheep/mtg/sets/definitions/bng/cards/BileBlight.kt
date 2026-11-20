package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bile Blight
 * {B}{B}
 * Instant
 * Target creature and all other creatures with the same name as that creature get -3/-3 until
 * end of turn.
 *
 * The target is shrunk directly; every other creature whose name matches the target's (read as the
 * spell resolves) is snapshotted and shrunk in turn. Creatures that share the name are affected no
 * matter who controls them.
 */
val BileBlight = card("Bile Blight") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature and all other creatures with the same name as that creature get -3/-3 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, -3, creature) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.sharingNameWith(creature), excludeTarget = true),
            Effects.ModifyStats(-3, -3, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Vincent Proce"
        flavorText = "Not an arrow loosed, javelin thrown, nor sword raised. None were needed."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ca11057-e50a-4817-924a-5bb504d0780f.jpg?1783939561"
    }
}
