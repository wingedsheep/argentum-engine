package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spirit of the Hunt
 * {1}{G}{G}
 * Creature — Wolf Spirit
 * 3/3
 * Flash
 * When this creature enters, each other creature you control that's a Wolf or a Werewolf gets
 * +0/+3 until end of turn.
 *
 * The affected set is locked in as the trigger resolves ([Effects.ForEachInGroup] over the
 * Wolves and Werewolves you control then), so a creature that becomes a Wolf or Werewolf later
 * in the turn doesn't get the bonus.
 */
val SpiritOfTheHunt = card("Spirit of the Hunt") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Spirit"
    oracleText = "Flash\nWhen this creature enters, each other creature you control that's a Wolf or a Werewolf gets +0/+3 until end of turn."
    power = 3
    toughness = 3
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)).youControl(),
                excludeSelf = true
            ),
            Effects.ModifyStats(0, 3, EffectTarget.IterationEntity)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Bastien L. Deharme"
        flavorText = "The bonds of the pack transcend time and death."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c5f7b11-49ac-4078-aab7-efe3aff746b2.jpg?1783937439"
    }
}
