package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Step Between Worlds {3}{U}{U}
 * Sorcery
 *
 * Each player may shuffle their hand and graveyard into their library. Each player who does
 * draws seven cards. Exile Step Between Worlds.
 * Plot {4}{U}{U}
 *
 * A per-player optional wheel: [ForEachPlayerEffect] over [Player.Each] iterates every player; the
 * body's controller is rebound to the current player, so [Player.You] and `EffectTarget.Controller`
 * resolve to them. Each player's body is wrapped in [MayEffect] with `decisionMaker = Controller`,
 * so every player independently chooses yes/no. A player who says yes gathers their hand + graveyard
 * in one combined pass ([CardSource.FromMultipleZones]) and shuffles them into their library, then
 * draws seven — a player who declines does neither (so "each player who does draws seven cards" is
 * honored automatically: declining means no shuffle and no draw).
 *
 * "Exile Step Between Worlds." is the `selfExile()` flag — the spell goes to exile instead of the
 * graveyard on resolution (CR 608.2; the engine routes it via `CardScript.selfExileOnResolve`).
 *
 * Plot is the standard [KeywordAbility.plot] exile-from-hand-and-cast-later mechanic; it composes
 * cleanly with self-exile (a plotted cast still exiles itself on resolution).
 */
val StepBetweenWorlds = card("Step Between Worlds") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player may shuffle their hand and graveyard into their library. Each player " +
        "who does draws seven cards. Exile Step Between Worlds.\n" +
        "Plot {4}{U}{U} (You may pay {4}{U}{U} and exile this card from your hand. Cast it as a " +
        "sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    spell {
        selfExile()
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                MayEffect(
                    decisionMaker = EffectTarget.Controller,
                    effect = Effects.Composite(
                        GatherCardsEffect(
                            source = CardSource.FromMultipleZones(
                                zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                                player = Player.You
                            ),
                            storeAs = "stepBetweenWorldsShuffle"
                        ),
                        MoveCollectionEffect(
                            from = "stepBetweenWorldsShuffle",
                            destination = CardDestination.ToZone(
                                Zone.LIBRARY, Player.You, ZonePlacement.Shuffled
                            )
                        ),
                        Effects.DrawCards(7)
                    )
                )
            )
        )
    }

    keywordAbility(KeywordAbility.plot("{4}{U}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Chris Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70ea2054-3d22-42ce-ab50-501ef09c2128.jpg?1712355510"
    }
}
