package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decommission
 * {2}{W}
 * Instant
 *
 * Destroy target artifact or enchantment.
 * Revolt — If a permanent left the battlefield under your control this turn, you gain 3 life.
 *
 * Revolt is checked at resolution, after the destroy, through the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker — so destroying your own
 * artifact or enchantment enables Decommission's own revolt (2017-02-09 ruling). If the target
 * is illegal on resolution the spell doesn't resolve and no life is gained.
 *
 * Aether Revolt is the earliest real printing; the only other is The List (PLST).
 */
val Decommission = card("Decommission") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment.\n" +
        "Revolt — If a permanent left the battlefield under your control this turn, you gain 3 life."

    spell {
        val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(artifactOrEnchantment) then Effects.If(
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn,
            then = Effects.GainLife(3)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Josh Hass"
        flavorText = "\"A key weakness in the design is its vulnerability to repeated, forceful blows.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cca66e9-be21-4fc7-8951-cd99e9b213dc.jpg?1783936780"
        ruling("2017-02-09", "If Decommission destroys a permanent you control, it will enable its own revolt ability and you'll gain 3 life.")
        ruling("2017-02-09", "If the target artifact or enchantment becomes an illegal target, Decommission doesn't resolve and none of its effects happen. You won't gain life.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
