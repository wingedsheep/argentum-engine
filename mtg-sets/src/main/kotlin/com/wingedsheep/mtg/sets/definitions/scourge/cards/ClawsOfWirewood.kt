package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect

/**
 * Claws of Wirewood
 * {3}{G}
 * Sorcery
 * Claws of Wirewood deals 3 damage to each creature with flying and each player.
 * Cycling {2}
 */
val ClawsOfWirewood = card("Claws of Wirewood") {
    manaCost = "{3}{G}"
    typeLine = "Sorcery"
    oracleText = "Claws of Wirewood deals 3 damage to each creature with flying and each player.\nCycling {2}"

    spell {
        effect = Effects.DealDamageToAll(3, Filters.Group.creatures { withKeyword(Keyword.FLYING) }) then
            DealDamageToPlayersEffect(3)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Tony Szczudlo"
        flavorText = "They say the forest has eyes. They never mention its claws."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b94cd33f-40b6-4b11-97a4-8676ef27631e.jpg?1562533774"
    }
}
