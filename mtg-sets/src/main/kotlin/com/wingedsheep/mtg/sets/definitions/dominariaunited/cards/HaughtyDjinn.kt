package com.wingedsheep.mtg.sets.definitions.dominariaunited.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Haughty Djinn
 * {1}{U}{U}
 * Creature — Djinn
 * * / 4
 * Flying
 * Haughty Djinn's power is equal to the number of instant and sorcery cards in your graveyard.
 * Instant and sorcery spells you cast cost {1} less to cast.
 */
val HaughtyDjinn = card("Haughty Djinn") {
    manaCost = "{1}{U}{U}"
    typeLine = "Creature — Djinn"
    oracleText = "Flying\nHaughty Djinn's power is equal to the number of instant and sorcery cards in your graveyard.\nInstant and sorcery spells you cast cost {1} less to cast."

    // Power is dynamic based on instant and sorcery cards in controller's graveyard
    dynamicPower = CharacteristicValue.dynamic(
        DynamicAmount.Count(
            player = Player.You,
            zone = Zone.GRAVEYARD,
            filter = Filters.Unified.instantOrSorcery
        )
    )

    // Base toughness is 4
    toughness = 4

    // Flying keyword
    keywords(Keyword.FLYING)

    // Cost reduction for instant and sorcery spells
    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = Filters.Unified.instantOrSorcery,
            amount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Mike Jordana"
        flavorText = "\"I adore your outfit. It's so . . . rustic.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35095a68-b7c0-4805-b0b6-6ca15a338692.jpg?1673306736"
        ruling("2022-09-09", "The ability that defines Haughty Djinn's power functions in all zones.")
        ruling("2022-09-09", "A split card in your graveyard only counts once for Haughty Djinn's power, even if it's both an instant and a sorcery.")
    }
}
