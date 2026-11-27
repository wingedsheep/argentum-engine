package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Essence Harvest
 * {2}{B}
 * Sorcery
 * Target player loses X life and you gain X life, where X is the greatest power among creatures
 * you control.
 *
 * X is the MAX-power aggregate of your creatures (as Fungal Sprouting reads it), measured on
 * resolution. The gain is X itself, not the life actually lost, so this is a lose-then-gain pair
 * rather than a drain.
 */
val EssenceHarvest = card("Essence Harvest") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player loses X life and you gain X life, where X is the greatest power among creatures you control."

    spell {
        val t = target(Targets.Player)
        val x = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        effect = Effects.LoseLife(x, t) then Effects.GainLife(x)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Daarken"
        flavorText = "Captain Lewenheart uncovered evidence of a demon cult among the clergy. The Skirsdag never gave him a chance to prove his case."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c3fac03-a019-4faa-bc1c-09e3a394fff7.jpg?1783940699"

        ruling(
            "2012-05-01",
            "The value of X is determined when Essence Harvest resolves. If you control no creatures that " +
                "time, no life will be lost or gained."
        )
        ruling(
            "2012-05-01",
            "Essence Harvest targets only the player. It doesn't target any creatures. If that player is an " +
                "illegal target when Essence Harvest tries to resolve, it won't resolve and none of its effects " +
                "will happen. No life will be lost or gained."
        )
    }
}
