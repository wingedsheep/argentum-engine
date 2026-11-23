package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Undercity Informer — Gatecrash #82 (canonical printing)
 * {2}{B} · Creature — Human Rogue · 2/3
 *
 * {1}, Sacrifice a creature: Target player reveals cards from the top of their library until
 * they reveal a land card, then puts those cards into their graveyard.
 *
 * The same GatherUntilMatch + RevealCollection + MoveCollection pipeline as Balustrade Spy's
 * enters trigger, behind a {1} + sacrifice-a-creature cost (the Informer itself can be the
 * sacrifice). The revealed land goes to the graveyard with the rest of the pile.
 */
val UndercityInformer = card("Undercity Informer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "{1}, Sacrifice a creature: Target player reveals cards from the top of their library " +
        "until they reveal a land card, then puts those cards into their graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Creature))
        target(Targets.Player)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, player = Player.TargetPlayer)
            reveal(allRevealed)
            toGraveyard(allRevealed, Player.TargetPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Raymond Swanland"
        flavorText = "\"Scandal and slander are never welcome news. That's why I'm never the messenger.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/822d0f73-cfb0-41d9-b4eb-09c605112a13.jpg?1783940127"
        ruling(
            "2024-01-12",
            "The land card they reveal, if any, is included in the cards put into their graveyard."
        )
        ruling(
            "2017-11-17",
            "If the target player has no land cards in their library, all cards from that library " +
                "will be revealed and put into their graveyard."
        )
    }
}
