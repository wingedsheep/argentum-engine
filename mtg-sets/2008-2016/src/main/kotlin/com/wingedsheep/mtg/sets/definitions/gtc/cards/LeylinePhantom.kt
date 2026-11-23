package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leyline Phantom — Gatecrash #41
 * {4}{U} · Creature — Illusion · 5/5
 *
 * When this creature deals combat damage, return it to its owner's hand.
 * (Return it only if it survived combat.)
 *
 * The trigger names no recipient, so damage to a player or to a blocking creature both count
 * (`dealsCombatDamage()` with no recipient, as Sunhome Enforcer). The bounce is gated on
 * `fromZone = BATTLEFIELD`: if the Phantom died in the same combat damage step it stays in the
 * graveyard — the reminder text's "only if it survived combat".
 */
val LeylinePhantom = card("Leyline Phantom") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    power = 5
    toughness = 5
    oracleText = "When this creature deals combat damage, return it to its owner's hand. " +
        "(Return it only if it survived combat.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage()
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Ryan Yee"
        flavorText = "\"Is the maze itself a phantom? Or is one as real as the other? Perhaps I am as mad as the dragon.\"\n—Jace Beleren, journal"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0867865-d9eb-45d2-a359-064f0f61197b.jpg?1783940136"
    }
}
