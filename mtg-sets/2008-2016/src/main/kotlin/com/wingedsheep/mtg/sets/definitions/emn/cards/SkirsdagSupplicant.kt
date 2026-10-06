package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Skirsdag Supplicant — Eldritch Moon #104. */
val SkirsdagSupplicant = card("Skirsdag Supplicant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    oracleText = "{B}, {T}, Discard a card: Each player loses 2 life."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"I can no longer stand by as these Skirsdag infiltrators rot the church from within. " +
            "Even the Lunarch Council is compromised. What a fool I've been.\"\n—Odric"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3f63897-15a1-4b34-9916-d9df62f66785.jpg?1783937475"
        ruling("2016-07-13", "If Skirsdag Supplicant's ability causes each player's life total to become 0 or less, the game ends in a draw.")
        ruling("2016-07-13", "In a Two-Headed Giant game, Skirsdag Supplicant's ability causes each player to lose 2 life, so each team loses a total of 4 life.")
    }
}
