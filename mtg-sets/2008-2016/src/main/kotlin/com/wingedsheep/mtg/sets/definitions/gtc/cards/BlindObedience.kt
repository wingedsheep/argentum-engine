package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

private const val BLIND_OBEDIENCE_EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Blind Obedience — Gatecrash #6 (canonical printing)
 * {1}{W} · Enchantment
 *
 * Extort
 * Artifacts and creatures your opponents control enter tapped.
 *
 * - Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 *   `{W/B}` over [Effects.DrainLife] (the gain is the total life lost).
 * - The tap clause is one global [PermanentsEnterTapped] replacement over
 *   `CreatureOrArtifact.opponentControls()` — the Authority of the Consuls / Dauntless Dismantler
 *   shape, joined into a single filter so an artifact creature is matched once. Opponents' tokens are
 *   covered too.
 */
val BlindObedience = card("Blind Obedience") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "$BLIND_OBEDIENCE_EXTORT_TEXT\nArtifacts and creatures your opponents control enter tapped."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = BLIND_OBEDIENCE_EXTORT_TEXT
    }

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrArtifact.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Seb McKinnon"
        flavorText = "\"By the time your knees have worn through your robe, you may have begun to learn your place.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07c3e78d-d917-4552-842f-feff99c059e0.jpg?1783940144"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
