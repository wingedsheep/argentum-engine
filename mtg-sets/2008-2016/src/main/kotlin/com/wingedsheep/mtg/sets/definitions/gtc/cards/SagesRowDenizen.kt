package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sage's Row Denizen (GTC #46)
 * {2}{U}  Creature — Vedalken Wizard  2/3
 *
 * Whenever another blue creature you control enters, target player mills two cards.
 */
val SagesRowDenizen = card("Sage's Row Denizen") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Wizard"
    oracleText = "Whenever another blue creature you control enters, target player mills two cards."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withColor(Color.BLUE).youControl()).enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Svetlin Velinov"
        flavorText = "\"I offer you wisdom untainted by false loyalty, learning free of any guild's agenda.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/063e6df9-2287-485a-ab46-fa4a38783884.jpg"
    }
}
