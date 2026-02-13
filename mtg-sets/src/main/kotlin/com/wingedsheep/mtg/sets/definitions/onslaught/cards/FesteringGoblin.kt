package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Festering Goblin
 * {B}
 * Creature — Zombie Goblin
 * 1/1
 * When Festering Goblin dies, target creature gets -1/-1 until end of turn.
 */
val FesteringGoblin = card("Festering Goblin") {
    manaCost = "{B}"
    typeLine = "Creature — Zombie Goblin"
    power = 1
    toughness = 1
    oracleText = "When Festering Goblin dies, target creature gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.Dies
        target = Targets.Creature
        effect = ModifyStatsEffect(
            powerModifier = -1,
            toughnessModifier = -1,
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Thomas M. Baxa"
        flavorText = "In life, it was a fetid, disease-ridden thing. In death, not much changed."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7209cc8-b519-4f27-87d8-b12e239a121f.jpg"
    }
}
