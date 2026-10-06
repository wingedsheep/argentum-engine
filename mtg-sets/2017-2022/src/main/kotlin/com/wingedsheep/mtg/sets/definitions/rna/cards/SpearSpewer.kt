package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spear Spewer
 * {R}
 * Creature — Goblin Warrior
 * 0/2
 * Defender
 * {T}: This creature deals 1 damage to each player.
 */
val SpearSpewer = card("Spear Spewer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Defender\n{T}: This creature deals 1 damage to each player."
    power = 0
    toughness = 2
    keywords(Keyword.DEFENDER)
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachPlayer(Player.Each, Effects.DealDamage(1, EffectTarget.Controller))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Carl Critchlow"
        flavorText = "\"Don't waste time aiming, you lazy gob-slug! Fire!\"\n—Krenko, mob boss"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbee1daa-b4ba-49ee-bed1-e70fa09942a2.jpg?1783933675"
        ruling("2019-01-25", "In a Two-Headed Giant game, Spear Spewer's ability causes each team to lose 2 life.")
    }
}
