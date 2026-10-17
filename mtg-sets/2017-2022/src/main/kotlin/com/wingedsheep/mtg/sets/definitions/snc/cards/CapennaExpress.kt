package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Capenna Express
 * {3}{G}
 * Artifact — Vehicle
 * 6/6
 *
 * Sacrifice a Treasure: This Vehicle becomes an artifact creature until end of turn.
 * Crew 3
 *
 * The longhand animate ability is the Honeymoon Hearse shape — [Effects.BecomeCreature] on self with
 * the printed base P/T until end of turn — paid with a Treasure sacrifice (the Treasure Map cost).
 * Crew 3 is the engine's [KeywordAbility.crew] activated ability.
 */
val CapennaExpress = card("Capenna Express") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Vehicle"
    power = 6
    toughness = 6
    oracleText = "Sacrifice a Treasure: This Vehicle becomes an artifact creature until end of turn.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact.withSubtype("Treasure"))
        effect = Effects.BecomeCreature(EffectTarget.Self, power = 6, toughness = 6)
        description = "Sacrifice a Treasure: This Vehicle becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5530b04-f086-4d09-97e9-eda45f8b1f7b.jpg?1783923105"
    }
}
