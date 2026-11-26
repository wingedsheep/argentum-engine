package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Joint Assault
 * {G}
 * Instant
 * Target creature gets +2/+2 until end of turn. If it's paired with a creature, that creature
 * also gets +2/+2 until end of turn.
 *
 * "That creature" is the group of creatures soulbond-paired with the target — at most one, empty
 * when the target is unpaired, which makes the "if" self-enforcing. The group is snapshotted at
 * resolution, so the partner keeps its bonus even if the pair later breaks.
 */
val JointAssault = card("Joint Assault") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's paired with a creature, " +
        "that creature also gets +2/+2 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.pairedWith(creature)),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Raymond Swanland"
        flavorText = "\"By my power, werewolves shall become the wolfir, our allies in combat against darkness.\"\n—Avacyn's Cursemute Decree"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6516c7c6-d49b-49c5-8968-563622c2c8c1.jpg?1783940665"
    }
}
