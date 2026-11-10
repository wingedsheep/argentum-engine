package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Crush of Tentacles
 * {4}{U}{U}
 * Sorcery
 * Surge {3}{U}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Return all nonland permanents to their owners' hands. If this spell's surge cost was paid, create an 8/8 blue Octopus creature token.
 *
 * The Octopus rider reads [Conditions.SurgeCostWasPaid] off the resolving spell, which answers from
 * the alternative cost it was cast for (CR 702.117a). The token is created after the bounce, so it
 * stays.
 */
val CrushOfTentacles = card("Crush of Tentacles") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Surge {3}{U}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Return all nonland permanents to their owners' hands. If this spell's surge cost was paid, create an 8/8 blue Octopus creature token."

    keywordAbility(KeywordAbility.surge("{3}{U}{U}"))

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.NonlandPermanent)) then
            Effects.If(
                Conditions.SurgeCostWasPaid,
                then = Effects.CreateToken(
                    power = 8,
                    toughness = 8,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Octopus"),
                ),
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "Jama Jurabaev"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be1434e1-a0d6-4028-918b-1e2d17d3a227.jpg?1783937919"
    }
}
