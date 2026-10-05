package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dread Rider
 * {5}{B}
 * Creature — Spirit Knight
 * 3/7
 * {1}{B}, {T}, Exile a creature card from your graveyard: Target opponent loses 3 life.
 */
val DreadRider = card("Dread Rider") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit Knight"
    oracleText = "{1}{B}, {T}, Exile a creature card from your graveyard: Target opponent loses 3 life."
    power = 3
    toughness = 7

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.Tap,
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(3, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Irina Nordsol"
        flavorText = "\"What kind of monstrous grave robber leaves the treasures, but takes the bodies?\"\n—Fjall, Beskir elder"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/205eb029-68a0-4895-b142-2eb09987b5cb.jpg?1783928250"
    }
}
