package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mire in Misery — Commander 2019 #19 (canonical printing)
 * {1}{B} · Sorcery
 *
 * Each opponent sacrifices a creature or enchantment of their choice.
 *
 * A non-targeted edict over every opponent: [Effects.Sacrifice] with the opponents as its target
 * and a creature-or-enchantment filter, so each opponent chooses their own sacrifice.
 */
val MireInMisery = card("Mire in Misery") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent sacrifices a creature or enchantment of their choice."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature or GameObjectFilter.Enchantment,
            1,
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Anna Podedworna"
        flavorText = "Misery loves company."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37b2cc43-307c-429f-a4f7-ac339a97e0bb.jpg?1783932810"
    }
}
