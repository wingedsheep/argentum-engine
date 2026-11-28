package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ghoulcaller's Bell
 * {1}
 * Artifact
 * {T}: Each player mills a card.
 */
val GhoulcallersBell = card("Ghoulcaller's Bell") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Each player mills a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
        description = "Each player mills a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Lars Grant-West"
        flavorText = "The bell's chime heralds the rise of the unhallowed from the grave and the fall " +
            "of their victims into the earth."
        imageUri =
            "https://cards.scryfall.io/normal/front/8/6/863e7c2a-698c-4dce-a10b-ca58e4affa57.jpg?1783940901"
    }
}
