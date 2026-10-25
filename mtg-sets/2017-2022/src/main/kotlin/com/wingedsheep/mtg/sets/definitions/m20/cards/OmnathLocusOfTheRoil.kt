package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Omnath, Locus of the Roil — Core Set 2020 #216
 * {1}{G}{U}{R} · Legendary Creature — Elemental 3/3
 *
 * When Omnath enters, it deals damage to any target equal to the number of Elementals you control.
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target Elemental you
 * control. If you control eight or more lands, draw a card.
 *
 * "Elementals" is the bare tribal noun, so both the count and the landfall target are Elemental
 * *permanents*. The count is read on resolution (Omnath counts itself if still there). The landfall
 * draw is gated by `Conditions.ControlLandsAtLeast(8)` on resolution; if the target became illegal
 * the whole ability fizzles, so no card is drawn (ruling).
 */
val OmnathLocusOfTheRoil = card("Omnath, Locus of the Roil") {
    manaCost = "{1}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "When Omnath enters, it deals damage to any target equal to the number of Elementals you control.\n" +
        "Landfall — Whenever a land you control enters, put a +1/+1 counter on target Elemental you control. " +
        "If you control eight or more lands, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL)).count(),
            t
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val elemental = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL).youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, elemental) then
            Effects.If(Conditions.ControlLandsAtLeast(8), then = Effects.DrawCards(1))
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target Elemental " +
            "you control. If you control eight or more lands, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "216"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb53a29d-2de2-4874-a6f3-0fecbfa14cf2.jpg?1783932948"
        ruling("2019-07-12", "You may choose Omnath as the target of its own second ability.")
        ruling("2019-07-12", "The number of Elementals you control is counted as Omnath's first ability resolves. If Omnath is still on the battlefield, it will count itself.")
        ruling("2019-07-12", "If the target Elemental is an illegal target by the time Omnath's second ability tries to resolve, the ability doesn't resolve. You don't draw a card if you control eight or more lands. If the target is legal but can't have a +1/+1 counter put on it (most likely because an opponent controls Blightbeetle), you do draw a card if you control eight or more lands.")
    }
}
