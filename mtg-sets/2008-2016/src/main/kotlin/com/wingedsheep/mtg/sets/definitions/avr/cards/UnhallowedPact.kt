package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unhallowed Pact — Avacyn Restored #124 (earliest printing)
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature dies, return that card to the battlefield under your control.
 *
 * - The trigger belongs to the Aura, so "you" is Unhallowed Pact's controller (the ability's
 *   controller), whoever controlled or owned the creature: `ToZone(BATTLEFIELD)` defaults to
 *   `Player.You`, i.e. the trigger's controller.
 * - The dead creature is gathered by its triggering reference, which names it only while it is
 *   still the same object (CR 400.7): a token (ceased to exist) or a card that already left the
 *   graveyard (e.g. via undying) gathers nothing, so nothing happens (2012-05-01 rulings).
 */
val UnhallowedPact = card("Unhallowed Pact") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, return that card to the battlefield under your control."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Pipeline {
            val deadCreature = gather(CardSource.Entity(EffectTarget.TriggeringEntity))
            move(deadCreature, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Volkan Baǵa"
        flavorText = "Agreements with demons seldom end at the grave."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b26d73e6-9138-43b2-8031-6e3b25fa33f9.jpg?1783940690"
        ruling(
            "2012-05-01",
            "The card will return to the battlefield under your control only if it's still in the graveyard " +
                "when Unhallowed Pact's ability resolves. If it's not (perhaps because an ability like undying " +
                "has already returned it to the battlefield), nothing happens."
        )
        ruling("2012-05-01", "If Unhallowed Pact is enchanting a token creature, that creature can't return to the battlefield.")
    }
}
