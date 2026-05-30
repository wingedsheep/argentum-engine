package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devoted Duelist
 * {1}{R}
 * Creature — Goblin Monk
 * 2/1
 *
 * Haste
 * Flurry — Whenever you cast your second spell each turn, this creature deals 1 damage
 * to each opponent.
 */
val DevotedDuelist = card("Devoted Duelist") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Monk"
    power = 2
    toughness = 1
    oracleText = "Haste\nFlurry — Whenever you cast your second spell each turn, this creature deals 1 damage to each opponent."

    keywords(Keyword.HASTE)

    flurry {
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Nathaniel Himawan"
        flavorText = "\"Arrows? Ha! Wood and feather? Toys!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbf9c673-37b4-48ed-a9ea-13f8e3e6c47b.jpg?1743204378"
    }
}
