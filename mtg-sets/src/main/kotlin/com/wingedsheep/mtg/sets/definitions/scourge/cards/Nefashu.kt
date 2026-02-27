package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nefashu
 * {4}{B}{B}
 * Creature — Zombie Mutant
 * 5/3
 * Whenever Nefashu attacks, up to five target creatures each get -1/-1 until end of turn.
 */
val Nefashu = card("Nefashu") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie Mutant"
    power = 5
    toughness = 3
    oracleText = "Whenever Nefashu attacks, up to five target creatures each get -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        target = Targets.UpToCreatures(5)
        effect = ForEachTargetEffect(
            listOf(
                ModifyStatsEffect(
                    powerModifier = -1,
                    toughnessModifier = -1,
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.EndOfTurn
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "rk post"
        flavorText = "Where the nefashu pass, blood rolls like a silk carpet."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7046acc2-e2fd-43e6-9d46-a729d48ba562.jpg?1562530235"
    }
}
