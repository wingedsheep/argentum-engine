package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Soul Diviner — War of the Spark #218 (canonical printing)
 * {U}{B}
 * Creature — Zombie Wizard
 * 2/3
 * {T}, Remove a counter from an artifact, creature, land, or planeswalker you control: Draw a card.
 *
 * The cost is a typeless `Costs.RemoveCounters` restricted to the four permanent types: the player
 * picks the permanent and which kind of counter comes off it (the per-kind breakdown the
 * distributed counter-removal prompt offers), so a loyalty counter off a planeswalker works too.
 */
val SoulDiviner = card("Soul Diviner") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Wizard"
    power = 2
    toughness = 3
    oracleText = "{T}, Remove a counter from an artifact, creature, land, or planeswalker you control: Draw a card."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounters(
                count = 1,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature or
                    GameObjectFilter.Land or GameObjectFilter.Planeswalker
            )
        )
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Randy Vargas"
        flavorText = "As the Eternal's cold fingers tightened around his throat, Domri realized what kind of master he had rushed to serve."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fb66ccd-a930-4757-832c-c4b0259741e2.jpg?1783933386"
    }
}
