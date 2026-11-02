package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plague Mare
 * {1}{B}{B}
 * Creature — Nightmare Horse
 * 2/2
 * This creature can't be blocked by white creatures.
 * When this creature enters, creatures your opponents control get -1/-1 until end of turn.
 */
val PlagueMare = card("Plague Mare") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Horse"
    power = 2
    toughness = 2
    oracleText = "This creature can't be blocked by white creatures.\n" +
        "When this creature enters, creatures your opponents control get -1/-1 until end of turn."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.WHITE))
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity),
        )
        description = "When this creature enters, creatures your opponents control get -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "G-host Lee"
        flavorText = "When it passes, those who sleep dream of sickness and death."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/995596a3-343d-4272-86bf-d76fd32ab78e.jpg?1783934564"
    }
}
