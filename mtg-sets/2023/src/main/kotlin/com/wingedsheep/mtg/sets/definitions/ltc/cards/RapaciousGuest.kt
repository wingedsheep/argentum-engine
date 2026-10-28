package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rapacious Guest — Tales of Middle-earth Commander #28
 * {2}{B} · Creature — Halfling Citizen · 2/2
 *
 * Menace
 * Whenever one or more creatures you control deal combat damage to a player, create a Food token.
 * Whenever you sacrifice a Food, put a +1/+1 counter on this creature.
 * When this creature leaves the battlefield, target opponent loses life equal to its power.
 *
 * The combat-damage trigger is the batched "one or more" form (Elegy Acolyte). The sacrifice trigger
 * fires per Food sacrificed for any reason (Nuka-Cola Vending Machine's shape). The leaves trigger
 * reads [DynamicAmounts.sourcePower], which falls back to the power captured on the zone-change
 * event once the creature is gone — its last-known power, not the printed 2.
 */
val RapaciousGuest = card("Rapacious Guest") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Halfling Citizen"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "Whenever one or more creatures you control deal combat damage to a player, create a Food token.\n" +
        "Whenever you sacrifice a Food, put a +1/+1 counter on this creature.\n" +
        "When this creature leaves the battlefield, target opponent loses life equal to its power."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).dealCombatDamageToAPlayer()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(DynamicAmounts.sourcePower(), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Bartłomiej Gaweł"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff9dfba4-07d2-4407-a441-88acc7284d43.jpg?1783916032"
        ruling(
            "2023-06-16",
            "Rapacious Guest's third ability triggers whenever you sacrifice a Food for any reason, not " +
                "just to activate a Food's activated ability."
        )
        ruling(
            "2023-06-16",
            "To determine the amount of life the target opponent loses from Rapacious Guest's last ability, " +
                "use Rapacious Guest's power as it last existed on the battlefield, not its power in the graveyard."
        )
    }
}
