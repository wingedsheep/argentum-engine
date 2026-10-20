package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aisling Leprechaun
 * {G}
 * Creature — Faerie
 * 1/1
 * Whenever this creature blocks or becomes blocked by a creature, that creature becomes green.
 * (This effect lasts indefinitely.)
 *
 * Abomination's per-partner `blocksOrBecomesBlocked` trigger with Lifelace's effect: the combat
 * partner is the `TriggeringEntity`, and a `Duration.Permanent` Layer-5 colour change replaces its
 * colours with green for as long as it stays on the battlefield.
 */
val AislingLeprechaun = card("Aisling Leprechaun") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Faerie"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, that creature " +
        "becomes green. (This effect lasts indefinitely.)"

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.ChangeColor(EffectTarget.TriggeringEntity, setOf(Color.GREEN), Duration.Permanent)
        description = "Whenever this creature blocks or becomes blocked by a creature, that creature " +
            "becomes green. (This effect lasts indefinitely.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/640a161d-ad7b-4e5b-8f2d-d3753cb9daa3.jpg?1783948050"
    }
}
