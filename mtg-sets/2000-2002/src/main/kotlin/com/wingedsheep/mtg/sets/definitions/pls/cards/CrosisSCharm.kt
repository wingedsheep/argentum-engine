package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Crosis's Charm
 * {U}{B}{R}
 * Instant
 *
 * Choose one —
 * • Return target permanent to its owner's hand.
 * • Destroy target nonblack creature. It can't be regenerated.
 * • Destroy target artifact.
 *
 * A plain choose-one `modal(chooseCount = 1)`, one target per mode. The bounce mode is
 * [Effects.ReturnToHand] over an unfiltered [TargetPermanent]; the removal mode is
 * [Effects.Destroy] with `noRegenerate = true` (the facade composes the "can't be regenerated"
 * marker ahead of the destroy) over [TargetFilter.Creature]`.notColor(BLACK)`; the last mode is a
 * bare [Effects.Destroy] on [Targets.Artifact].
 */
val CrosisSCharm = card("Crosis's Charm") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target permanent to its owner's hand.\n" +
        "• Destroy target nonblack creature. It can't be regenerated.\n" +
        "• Destroy target artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Return target permanent to its owner's hand") {
                val t = target("target", TargetPermanent())
                effect = Effects.ReturnToHand(t)
            }
            mode("Destroy target nonblack creature. It can't be regenerated") {
                val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
                effect = Effects.Destroy(t, noRegenerate = true)
            }
            mode("Destroy target artifact") {
                val t = target("target", Targets.Artifact)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b59a9e75-9988-4040-a718-b1655fc20d11.jpg"
    }
}
