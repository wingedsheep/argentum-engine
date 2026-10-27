package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Haunting Voyage
 * {4}{B}{B}
 * Sorcery
 * Choose a creature type. Return up to two creature cards of that type from your graveyard to the
 * battlefield. If this spell was foretold, return all creature cards of that type from your
 * graveyard to the battlefield instead.
 * Foretell {5}{B}{B}
 *
 * [Conditions.WasForetold] (CR 702.143c) picks the branch; both branches choose the creature type
 * on resolution, then return either up to two or every creature card of that type.
 */
val HauntingVoyage = card("Haunting Voyage") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. Return up to two creature cards of that type from your " +
        "graveyard to the battlefield. If this spell was foretold, return all creature cards of that " +
        "type from your graveyard to the battlefield instead.\n" +
        "Foretell {5}{B}{B} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        effect = Effects.If(
            Conditions.WasForetold,
            then = Effects.Pipeline {
                run(ChooseCreatureTypeEffect)
                val graveyardCreatures = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature)
                )
                val chosen = selectAll(from = graveyardCreatures, matchChosenCreatureType = true)
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            },
            otherwise = Effects.Pipeline {
                run(ChooseCreatureTypeEffect)
                val graveyardCreatures = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature)
                )
                val chosen = chooseUpTo(
                    2,
                    from = graveyardCreatures,
                    matchChosenCreatureType = true,
                    prompt = "Choose up to two creature cards of the chosen type to return to the battlefield",
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            },
        )
    }

    keywordAbility(KeywordAbility.foretell("{5}{B}{B}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "98"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ac5bca0-35fe-47be-a78c-d5eac68a3bfa.jpg?1783928246"
    }
}
