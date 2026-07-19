package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Grabby Giant // That's Mine
 * {3}{R}
 * Creature — Giant (4/3)
 * Reach
 * {2}{R}, Sacrifice an artifact or land: Draw a card.
 *
 * Adventure: That's Mine — {1}{R}, Instant — Adventure
 * Create a Treasure token.
 *
 * (CR 715: casting the Adventure exiles the card on resolution and lets the caster cast the creature
 * from exile later. The front's own draw ability feeds on the Treasure the Adventure leaves behind.)
 */
val GrabbyGiant = card("Grabby Giant") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    oracleText = "Reach\n{2}{R}, Sacrifice an artifact or land: Draw a card."
    power = 4
    toughness = 3
    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Sacrifice(GameObjectFilter.ArtifactOrLand))
        effect = Effects.DrawCards(1)
    }

    adventure("That's Mine") {
        manaCost = "{1}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Create a Treasure token. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateTreasure(1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fab7646a-61e8-446b-9dba-ac6e0db82f10.jpg?1783915094"
    }
}
