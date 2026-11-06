package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Embraal Gear-Smasher
 * {2}{R}
 * Creature — Human Warrior
 * 2/3
 * {T}, Sacrifice an artifact: This creature deals 2 damage to each opponent.
 */
val EmbraalGearSmasher = card("Embraal Gear-Smasher") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "{T}, Sacrifice an artifact: This creature deals 2 damage to each opponent."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Joseph Meehan"
        flavorText = "\"A well-made wrench is a versatile, indispensable tool.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ad5c175-581c-4fdd-b008-e7d10b0928c7.jpg?1783936756"
        ruling("2017-02-09", "In a Two-Headed Giant game, Embraal Gear-Smasher's ability causes 4 damage total to be dealt to the opposing team.")
    }
}
