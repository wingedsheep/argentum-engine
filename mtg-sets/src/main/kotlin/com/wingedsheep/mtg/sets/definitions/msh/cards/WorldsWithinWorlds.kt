package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Worlds Within Worlds — Marvel Super Heroes #241 (rare)
 * {5}{G}{U} · Sorcery
 *
 * Exile all creatures. Each player may put any number of creature cards from their hand onto the
 * battlefield. Then put all cards exiled this way into their owners' hands. Exile Worlds Within
 * Worlds.
 *
 * Three pipeline stages plus the self-exile, all existing primitives:
 *
 *  1. **Exile all creatures.** [GatherCardsEffect] over [CardSource.BattlefieldMatching] with
 *     `Player.Each`, then [MoveCollectionEffect] to `EXILE` recapturing the pile as
 *     `worldsExiled` — that handle *is* "cards exiled this way", so a creature exiled by something
 *     else in response is not swept up later. Tokens exiled here cease to exist and simply drop
 *     out of the return step.
 *  2. **Each player may put any number of creature cards from their hand onto the battlefield.**
 *     [Effects.ForEachPlayer] over [Player.ActivePlayerFirst] (APNAP, CR 101.4) rebinds
 *     `Player.You` to each player in turn, so the gather reads *their* hand and the move puts the
 *     cards under *their* control. [SelectionMode.ChooseAnyNumber] carries the "may" — choosing
 *     none is a legal answer. Collection keys inside the loop are deliberately distinct from
 *     `worldsExiled`: the loop wipes `storedCollections` per iteration and returns no collections
 *     of its own, so the outer handle survives untouched into step 3.
 *  3. **Then put all cards exiled this way into their owners' hands.** A second
 *     [MoveCollectionEffect] off `worldsExiled`. A card entering a non-battlefield zone is always
 *     routed to its *owner's* copy of that zone, so an opponent's creature returns to their hand,
 *     not the caster's — the destination's nominal player is irrelevant here.
 *
 * `selfExile()` covers the trailing "Exile Worlds Within Worlds" — the spell exiles itself as the
 * last thing it does instead of going to its owner's graveyard.
 *
 * Deviation worth naming: the engine performs each player's put sequentially rather than
 * simultaneously, so an earlier player's creatures are already on the battlefield when a later
 * player chooses. The choices are still made in APNAP order, which is the part players can act on.
 */
val WorldsWithinWorlds = card("Worlds Within Worlds") {
    manaCost = "{5}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Exile all creatures. Each player may put any number of creature cards from " +
        "their hand onto the battlefield. Then put all cards exiled this way into their owners' " +
        "hands. Exile Worlds Within Worlds."

    spell {
        selfExile()
        effect = Effects.Composite(
            listOf(
                // 1. Exile all creatures.
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.Each
                    ),
                    storeAs = "worldsToExile"
                ),
                MoveCollectionEffect(
                    from = "worldsToExile",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    storeMovedAs = "worldsExiled"
                ),
                // 2. Each player may put any number of creature cards from their hand onto the
                //    battlefield, in APNAP order.
                Effects.ForEachPlayer(
                    Player.ActivePlayerFirst,
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                Zone.HAND,
                                Player.You,
                                GameObjectFilter.Creature
                            ),
                            storeAs = "worldsHandCandidates"
                        ),
                        SelectFromCollectionEffect(
                            from = "worldsHandCandidates",
                            selection = SelectionMode.ChooseAnyNumber,
                            storeSelected = "worldsHandChosen",
                            prompt = "You may put any number of creature cards from your hand " +
                                "onto the battlefield."
                        ),
                        MoveCollectionEffect(
                            from = "worldsHandChosen",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                        )
                    )
                ),
                // 3. Then put all cards exiled this way into their owners' hands.
                MoveCollectionEffect(
                    from = "worldsExiled",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Michael MacRae"
        flavorText = "\"Uh, I think I might have overdone it a smidge.\"\n—Ant-Man, Scott Lang"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4765e39c-cbf2-4605-9bf1-3baad7d92cfb.jpg?1783902892"
    }
}
