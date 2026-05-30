package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kavu Monarch
 * {2}{R}{R}
 * Creature — Kavu
 * 3/3
 * Kavu creatures have trample.
 * Whenever another Kavu enters, put a +1/+1 counter on this creature.
 *
 * The trample lord affects all Kavu (any controller, including itself). The counter
 * trigger fires on any other Kavu entering under any player's control (OTHER binding).
 */
val KavuMonarch = card("Kavu Monarch") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu"
    power = 3
    toughness = 3
    oracleText = "Kavu creatures have trample.\n" +
        "Whenever another Kavu enters, put a +1/+1 counter on this creature."

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Kavu"))
        )
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype("Kavu"),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea63dfd5-d8d7-45b8-8219-1cc2b3de5666.jpg?1562942087"
    }
}
