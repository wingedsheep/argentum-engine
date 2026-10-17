package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mr. Orfeo, the Boulder
 * {1}{B}{R}{G}
 * Legendary Creature — Rhino Warrior
 * 2/4
 *
 * Whenever you attack, double target creature's power until end of turn.
 *
 * "Whenever you attack" is `Triggers.you.attacks()` (once per combat in which you declare attackers).
 * The target is any creature, not just an attacker. Doubling is +X/+0 where X is the target's power
 * as the trigger resolves (per the 2022-04-29 ruling) — the same shape as Bulk Up.
 */
val MrOrfeoTheBoulder = card("Mr. Orfeo, the Boulder") {
    manaCost = "{1}{B}{R}{G}"
    typeLine = "Legendary Creature — Rhino Warrior"
    power = 2
    toughness = 4
    oracleText = "Whenever you attack, double target creature's power until end of turn."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = DynamicAmounts.powerOf(creature),
            toughness = DynamicAmounts.fixed(0),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Daarken"
        flavorText = "Once he was jumped by a dozen Maestros agents. You can still see their imprints in the pavement if you know where to look."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97caaa92-2a7d-4f79-8d42-86733c902072.jpg?1783923079"
        ruling(
            "2022-04-29",
            "To double a creature's power, that creature gets +X/+0, where X is that creature's power " +
                "as Mr. Orfeo's triggered ability resolves."
        )
    }
}
