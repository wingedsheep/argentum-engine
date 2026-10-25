package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Spectacle Mage
 * {1}{U}{R}
 * Creature — Bird Shaman
 * 2/2
 * Flying
 * Instant and sorcery spells you cast with mana value 5 or greater cost {1} less to cast.
 *
 * An X in the spell's mana cost counts at its chosen value (CR 202.3e) — the cost calculator
 * prices the cast with the announced X.
 */
val SpectacleMage = card("Spectacle Mage") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Bird Shaman"
    power = 2
    toughness = 2
    oracleText = "Flying\nInstant and sorcery spells you cast with mana value 5 or greater cost {1} less to cast."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery.manaValueAtLeast(5)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Caroline Gariba"
        flavorText = "\"It would be a crime to withhold my light from the world.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f74223e1-b66b-42fa-aaac-001f5dab2aac.jpg?1783927290"
    }
}
