package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Canyon Jerboa — Zendikar Rising #7
 * {2}{W} · Creature — Mouse · 1/2
 *
 * Landfall — Whenever a land you control enters, creatures you control get +1/+1 until end of turn.
 *
 * The affected set is locked in on resolution via [Effects.ForEachInGroup]: creatures that
 * enter later in the turn don't get the bonus.
 */
val CanyonJerboa = card("Canyon Jerboa") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Mouse"
    oracleText = "Landfall — Whenever a land you control enters, creatures you control get +1/+1 until end of turn."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Antonio José Manzanedo"
        flavorText = "\"Move, you wretched thing! You're scaring the hurdas!\"\n—Bruse Tarl, Goma Fada nomad"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26b3938e-ebd7-4668-be3a-422c02ff5b08.jpg"
    }
}
