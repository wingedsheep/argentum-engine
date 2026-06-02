package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect

/**
 * Territorial Bruntar
 * {4}{R}{R}
 * Creature — Beast
 * 6/6
 * Reach
 * Landfall — Whenever a land you control enters, exile cards from the top of your library
 * until you exile a nonland card. You may cast that card this turn.
 *
 * Pipeline: `GatherUntilMatch(Nonland)` walks the library top-down, storing every walked
 * card in `exiledCards` and the matching nonland (if any) in `impulseCard`. `MoveCollection`
 * routes the whole revealed pile — lands and the nonland alike — into exile. Finally
 * `GrantMayPlayFromExile("impulseCard")` grants may-play (default end-of-turn) on just
 * the nonland; the lands remain in exile without any may-play permission.
 *
 * If no nonland is found the entire library is exiled and `impulseCard` is empty, so the
 * grant is a no-op.
 */
val TerritorialBruntar = card("Territorial Bruntar") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Reach\n" +
        "Landfall — Whenever a land you control enters, exile cards from the top of your library " +
        "until you exile a nonland card. You may cast that card this turn."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.Composite(
            listOf(
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Nonland,
                    storeMatch = "impulseCard",
                    storeRevealed = "exiledCards"
                ),
                MoveCollectionEffect(
                    from = "exiledCards",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                Effects.GrantMayPlayFromExile(from = "impulseCard")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Julie Dillon"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbb25585-6048-4a85-828e-675bf0da6508.jpg?1752947221"
    }
}
