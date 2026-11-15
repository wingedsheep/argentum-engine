package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination

/**
 * Soulfire Grand Master
 * {1}{W}
 * Creature — Human Monk
 * 2/2
 * Lifelink
 * Instant and sorcery spells you control have lifelink.
 * {2}{U/R}{U/R}: The next time you cast an instant or sorcery spell from your hand this turn, put
 * that card into your hand instead of into your graveyard as it resolves.
 *
 * The last ability is a one-shot next-spell rider, not a trigger: the next matching cast from hand
 * consumes it whether or not the spell resolves (a countered spell goes to the graveyard), and a
 * spell that exiles itself is not returned.
 */
val SoulfireGrandMaster = card("Soulfire Grand Master") {
    manaCost = "{1}{W}"
    colorIdentity = "WUR"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "Lifelink\nInstant and sorcery spells you control have lifelink.\n" +
        "{2}{U/R}{U/R}: The next time you cast an instant or sorcery spell from your hand this turn, " +
        "put that card into your hand instead of into your graveyard as it resolves."

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.LIFELINK,
            spellFilter = GameObjectFilter.InstantOrSorcery
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U/R}{U/R}")
        effect = Effects.GrantNextSpellResolveDestination(
            destination = AfterResolveDestination.HAND,
            spellFilter = GameObjectFilter.InstantOrSorcery,
            fromZones = setOf(Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc3a4e7d-6667-4c2f-b6b4-484f401b0455.jpg?1783938709"
    }
}
