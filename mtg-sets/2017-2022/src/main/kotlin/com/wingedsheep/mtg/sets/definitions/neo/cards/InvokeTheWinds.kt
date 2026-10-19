package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invoke the Winds — Kamigawa: Neon Dynasty #58 (canonical printing)
 * {1}{U}{U}{U}{U} · Sorcery
 *
 * Gain control of target artifact or creature. Untap it.
 */
val InvokeTheWinds = card("Invoke the Winds") {
    manaCost = "{1}{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact or creature. Untap it."

    spell {
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.GainControl(t) then Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Olivier Bernard"
        flavorText = "\"Are you seriously telling me your heavy combat mech . . . blew away?\"\n" +
            "—Risona, leader of the Asari Uprisers"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad7b423a-bfd1-47aa-bd2c-64a169b96924.jpg?1783923902"
        ruling(
            "2022-02-18",
            "You will not be able to attack with the creature you gain control of or use its activated " +
                "abilities with {T} in the cost unless it has haste."
        )
        ruling(
            "2022-02-18",
            "Gaining control of a permanent doesn't cause you to gain control of any Auras or Equipment " +
                "attached to that permanent."
        )
    }
}
