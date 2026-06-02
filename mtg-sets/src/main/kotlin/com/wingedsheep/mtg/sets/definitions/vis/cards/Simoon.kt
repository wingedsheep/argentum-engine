package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Simoon
 * {R}{G}
 * Instant
 * Simoon deals 1 damage to each creature target opponent controls.
 *
 * Canonical printing is Visions (earliest real expansion). Later printings
 * (Invasion, etc.) contribute only `Printing(...)` rows.
 */
val Simoon = card("Simoon") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Simoon deals 1 damage to each creature target opponent controls."

    spell {
        target = Targets.Opponent
        effect = Effects.ForEachInGroup(
            GroupFilter(Filters.Creature.targetOpponentControls()),
            DealDamageEffect(1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642d9239-82e0-4696-ad99-10796042d1f8.jpg?1587913163"
    }
}
