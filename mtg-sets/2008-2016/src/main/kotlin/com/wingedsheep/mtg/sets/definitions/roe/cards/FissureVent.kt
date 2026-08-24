package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Fissure Vent
 * {3}{R}{R}
 * Sorcery
 * Choose one or both —
 * • Destroy target artifact.
 * • Destroy target nonbasic land.
 *
 * A choose-one-or-both modal spell — [modal] with `chooseCount = 2, minChooseCount = 1`, where
 * `chooseCount` is the maximum. Each mode declares its own target, so the two halves are
 * independent: [TargetPermanent] over [TargetFilter.Artifact] and over [TargetFilter.NonbasicLand].
 * Both bodies are the plain [Effects.Destroy], which regeneration and indestructible still answer.
 */
val FissureVent = card("Fissure Vent") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target nonbasic land."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Destroy target artifact") {
                val t = target("target", TargetPermanent(filter = TargetFilter.Artifact))
                effect = Effects.Destroy(t)
            }
            mode("Destroy target nonbasic land") {
                val t = target("target", TargetPermanent(filter = TargetFilter.NonbasicLand))
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Philip Straub"
        flavorText = "\"Something down there has an appetite for what we're standing on. Let's hope it doesn't want seconds.\"\n—Samila, Murasa Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15f4f813-f04c-4309-a007-b0549c00d6ab.jpg"
        ruling("2010-06-15", "You may choose just the first mode (targeting an artifact), just the second mode (targeting a nonbasic land), or both modes (targeting an artifact and a nonbasic land). You can’t choose a mode unless there’s a legal target for it.")
    }
}
