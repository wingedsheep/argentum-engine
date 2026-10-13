package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

/**
 * Balduvian Berserker — the dies trigger reads the creature's power as it last existed on the
 * battlefield (the source's last-known-information fallback), so an enlist boost counts.
 */
val BalduvianBerserker = card("Balduvian Berserker") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Kor Berserker"
    power = 1
    toughness = 3
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nWhen this creature dies, it deals damage equal to its power to any target."

    staticAbility { ability = Enlist }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c35bab2f-8464-4968-8c50-8807638d687b.jpg?1783921322"
        ruling("2022-09-09", "To determine the amount of damage it deals, use Balduvian Berserker's power as it last existed on the battlefield, not its power in the graveyard.")
    }
}
