package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Selesnya Charm
 * {G}{W}
 * Instant
 *
 * Choose one —
 * • Target creature gets +2/+2 and gains trample until end of turn.
 * • Exile target creature with power 5 or greater.
 * • Create a 2/2 white Knight creature token with vigilance.
 *
 * A plain choose-one `modal(chooseCount = 1)`. The pump mode is an [Effects.Composite] of
 * [Effects.ModifyStats]`(2, 2)` and [Effects.GrantKeyword]`(TRAMPLE)` on the same bound target,
 * both defaulting to end of turn; the removal mode is [Effects.Exile] over
 * [GameObjectFilter.Creature]`.powerAtLeast(5)`; the token mode is the plain [Effects.CreateToken]
 * facade with the printed colour, subtype and keyword.
 */
val SelesnyaCharm = card("Selesnya Charm") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets +2/+2 and gains trample until end of turn.\n" +
        "• Exile target creature with power 5 or greater.\n" +
        "• Create a 2/2 white Knight creature token with vigilance."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets +2/+2 and gains trample until end of turn") {
                val t = target("target", TargetCreature())
                effect = Effects.Composite(
                    Effects.ModifyStats(2, 2, t),
                    Effects.GrantKeyword(Keyword.TRAMPLE, t)
                )
            }
            mode("Exile target creature with power 5 or greater") {
                val t = target("target", TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtLeast(5))))
                effect = Effects.Exile(t)
            }
            mode("Create a 2/2 white Knight creature token with vigilance") {
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Knight"),
                    keywords = setOf(Keyword.VIGILANCE)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9848eab-1d3a-4ab0-adf6-c20858aa3afb.jpg"
    }
}
