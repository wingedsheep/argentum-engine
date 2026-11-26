package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fungal Sprouting
 * {3}{G}
 * Sorcery
 * Create X 1/1 green Saproling creature tokens, where X is the greatest power among creatures
 * you control.
 *
 * [Effects.CreateToken] with a dynamic count over the MAX-power aggregate of your creatures (as
 * Spell Rupture reads it). The amount is read on resolution, so with no creatures X is 0 and no
 * tokens are created.
 */
val FungalSprouting = card("Fungal Sprouting") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create X 1/1 green Saproling creature tokens, where X is the greatest power among creatures you control."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Brad Rigney"
        flavorText = "\"Saprolings need only a host to feed upon. Numbers will take care of the rest.\"\n—Ghave, Guru of Spores"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97413ae3-037e-4786-85a3-e92604acd771.jpg?1783940471"

        ruling(
            "2012-07-01",
            "Fungal Sprouting doesn't target any creature. The greatest power among creatures you " +
                "control is determined when Fungal Sprouting resolves."
        )
    }
}
