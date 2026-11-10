package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clip Wings
 * {1}{G}
 * Instant
 * Each opponent sacrifices a creature of their choice with flying.
 */
val ClipWings = card("Clip Wings") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Each opponent sacrifices a creature of their choice with flying."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            1,
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Howard Lyon"
        flavorText = "It is a grim duty to save the world from those created to protect it."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2835993-6c71-4a63-bad5-cb25aedc4a15.jpg?1783937734"
        ruling(
            "2016-04-08",
            "You can cast Clip Wings even if some (or all) of your opponents don't control a creature with flying. Those who do must each choose a flying creature to sacrifice, and those who don't are unaffected.",
        )
    }
}
