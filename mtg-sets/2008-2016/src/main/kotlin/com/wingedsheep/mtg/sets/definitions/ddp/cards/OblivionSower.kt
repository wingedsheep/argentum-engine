package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oblivion Sower
 * {6}
 * Creature — Eldrazi
 * 5/8
 * When you cast this spell, target opponent exiles the top four cards of their library, then you
 * may put any number of land cards that player owns from exile onto the battlefield under your
 * control.
 *
 * Canonical here: Duel Decks: Zendikar vs. Eldrazi (2015-08-28) printed it five weeks before
 * Battle for Zendikar, which carries a `Printing` row.
 *
 *  - A cast trigger, so it resolves while Oblivion Sower is still on the stack.
 *  - The pick is over *every* land card the targeted opponent owns in exile — not only the four
 *    just exiled — however those cards got there (ruling). The opponent's exile zone is keyed by
 *    owner, so the gather is exactly "that player owns".
 *  - `faceUp()`: a card exiled face down has no characteristics (ruling), so it is never a land
 *    card here even if you're allowed to look at it.
 *  - "Any number" is the 0..all pick; the chosen lands enter under the trigger controller's
 *    control.
 */
val OblivionSower = card("Oblivion Sower") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 8
    oracleText = "When you cast this spell, target opponent exiles the top four cards of their library, " +
        "then you may put any number of land cards that player owns from exile onto the battlefield " +
        "under your control."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.exileTop(4, opponent) then Effects.Pipeline {
            val exiledLands = gather(
                CardSource.FromZone(
                    zone = Zone.EXILE,
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Land.faceUp(),
                )
            )
            val chosen = chooseAnyNumber(
                exiledLands,
                prompt = "You may put any number of land cards that player owns from exile onto the " +
                    "battlefield under your control",
                selectedLabel = "Put onto the battlefield",
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "When you cast this spell, target opponent exiles the top four cards of their library, " +
            "then you may put any number of land cards that player owns from exile onto the battlefield " +
            "under your control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "41"
        artist = "Aleksi Briclot"
        flavorText = "The Eldrazi hunger without limit and consume without pause."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f3a5dea-0422-49f8-a35b-757ea500a52d.jpg?1783938246"
        ruling(
            "2015-08-25",
            "Oblivion Sower's ability allows you to put any land cards the player owns from exile onto the " +
                "battlefield, regardless of how those cards were exiled."
        )
        ruling(
            "2015-08-25",
            "Cards that are face down in exile have no characteristics. Such cards can't be put onto the " +
                "battlefield with Oblivion Sower's ability."
        )
    }
}
