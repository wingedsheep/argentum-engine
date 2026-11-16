package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hardened Berserker
 * {2}{R}
 * Creature — Human Berserker
 * 3/2
 * Whenever this creature attacks, the next spell you cast this turn costs {1} less to cast.
 *
 * The discount is the one-shot `Effects.ReduceNextSpellCost` rider (Peerless Samurai's shape):
 * the next spell cast this turn spends it whether or not it had generic mana to reduce, and only
 * generic mana is reduced.
 */
val HardenedBerserker = card("Hardened Berserker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, the next spell you cast this turn costs {1} less to cast."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReduceNextSpellCost(DynamicAmounts.fixed(1))
        description = "Whenever this creature attacks, the next spell you cast this turn costs {1} less to cast."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Viktor Titov"
        flavorText = "\"Just let him loose and follow the charge.\"\n—Yikaro, Atarka warrior"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d10fb883-12b8-4050-967e-4281ea620906.jpg?1783938590"
        ruling("2015-02-25", "Hardened Berserker can't reduce the colored mana requirement of a spell you cast.")
        ruling(
            "2015-02-25",
            "If there are any additional costs to cast a spell, apply those before applying Hardened " +
                "Berserker and any other cost reductions.",
        )
        ruling("2015-02-25", "The ability can apply to alternative costs to cast a spell.")
    }
}
