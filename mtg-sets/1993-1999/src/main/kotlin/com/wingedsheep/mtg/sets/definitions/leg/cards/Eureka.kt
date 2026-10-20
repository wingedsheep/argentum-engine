package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Eureka — Legends #185
 * {2}{G}{G} · Sorcery
 *
 * Starting with you, each player may put a permanent card from their hand onto the battlefield.
 * Repeat this process until no one puts a card onto the battlefield.
 *
 * A do-while loop ([Effects.RepeatWhile]) whose body is one *round*: every player in turn order
 * starting with the caster ([Player.EachStartingWithYou] — not the active player, which differs when
 * Eureka is cast on another player's turn, e.g. under Leyline of Anticipation) may put up to one
 * permanent card from their own hand onto the battlefield under their control. The round unions
 * what everyone put down (`forEachPlayerCollecting`), and the loop repeats while that union is
 * non-empty — so it ends only after a full round in which every player declined, and a player who
 * passed in an earlier round may still put a card down in a later one (the 2004-10-04 rulings).
 *
 * The cards are put onto the battlefield, not cast or played, and anything that triggers waits
 * until the whole resolution is done.
 */
val Eureka = card("Eureka") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Starting with you, each player may put a permanent card from their hand onto the " +
        "battlefield. Repeat this process until no one puts a card onto the battlefield."

    spell {
        var putThisRound: CollectionSlot? = null
        val round = Effects.Pipeline {
            val (put) = forEachPlayerCollecting(Player.EachStartingWithYou) {
                val permanents = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Permanent))
                val chosen = chooseUpTo(
                    1,
                    from = permanents,
                    chooser = Chooser.Controller,
                    prompt = "You may put a permanent card from your hand onto the battlefield"
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                listOf(chosen)
            }
            putThisRound = put
        }
        effect = Effects.RepeatWhile(
            body = round,
            repeatCondition = RepeatCondition.WhileCondition(Conditions.CollectionContainsMatch(checkNotNull(putThisRound)))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/520db5fb-d961-45a3-af74-6f054b8be3ab.jpg?1783948048"
        ruling("2004-10-04", "In a game of N players, the process ends when all N players in sequence (starting with you) choose not to put a card onto the battlefield. It doesn't end the first time a player chooses not to put a card onto the battlefield. If a player chooses not to put a card onto the battlefield but the process continues, that player may put a card onto the battlefield the next time the process gets around to them.")
        ruling("2004-10-04", "The cards are put onto the battlefield and will not trigger effects which trigger on such cards being \"cast\" or \"played\".")
        ruling("2006-10-15", "Anything that triggers during the resolution of this will wait to be put on the stack until everything is put onto the battlefield and resolution is complete.")
    }
}
