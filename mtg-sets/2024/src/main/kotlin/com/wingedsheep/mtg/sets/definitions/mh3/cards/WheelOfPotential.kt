package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** One player's exiled hand, named so the per-player loop can hand it out to the aggregate. */
private val wheelOfPotentialExiledHand = CollectionSlot("wheelOfPotentialExiledHand")

/**
 * Wheel of Potential
 * {2}{R}
 * Sorcery
 * You get {E}{E}{E} (three energy counters), then you may pay any amount of {E}.
 * Each player may exile their hand and draw a number of cards equal to the amount of {E} paid
 * this way. If seven or more {E} was paid this way, you may play cards you own exiled this way
 * until the end of your next turn.
 *
 * 1. `GetEnergy(3)` then `PayCounters(ENERGY)` stores the paid amount as "paid" (Galvanic
 *    Discharge's shape).
 * 2. A per-player loop in APNAP order (CR 101.4): each player's body is a `May` decided by that
 *    player; a yes exiles their whole hand (tracked) and draws "paid" cards. The exiled hands are
 *    appended into one aggregate collection across the loop.
 * 3. Back in the caster's frame, the aggregate is narrowed to cards the caster owns, and — only if
 *    seven or more was paid — they get "until the end of your next turn" play permission.
 *
 * Deviation: iterations run one after another, so a later player decides after an earlier
 * player's hand has already been exiled and replaced — the ruling has everyone decide in turn
 * order first and then exile simultaneously. Later players know who chose to exile in both
 * models; here they additionally see the exiled cards. This matches every other symmetric hand
 * effect in the engine (Step Between Worlds, Reforge the Soul).
 */
val WheelOfPotential = card("Wheel of Potential") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You get {E}{E}{E} (three energy counters), then you may pay any amount of {E}.\n" +
        "Each player may exile their hand and draw a number of cards equal to the amount of {E} " +
        "paid this way. If seven or more {E} was paid this way, you may play cards you own exiled " +
        "this way until the end of your next turn."

    spell {
        effect = Effects.GetEnergy(3) then
            Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.Pipeline {
                val (exiledHands) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                    run(
                        Effects.May(
                            decisionMaker = EffectTarget.Controller,
                            effect = Effects.Pipeline {
                                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                                moveTracked(
                                    hand,
                                    CardDestination.ToZone(Zone.EXILE, Player.You),
                                    name = wheelOfPotentialExiledHand.key
                                )
                                run(Effects.DrawCards(DynamicAmounts.storedNumber("paid")))
                            }
                        )
                    )
                    listOf(wheelOfPotentialExiledHand)
                }
                val yours = filter(exiledHands, GameObjectFilter.Any.ownedByYou())
                run(
                    Effects.If(
                        Conditions.CompareAmounts(
                            DynamicAmounts.storedNumber("paid"),
                            ComparisonOperator.GTE,
                            7
                        ),
                        Effects.GrantMayPlayFromExile(yours, expiry = MayPlayExpiry.UntilEndOfNextTurn)
                    )
                )
            }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Drew Baker"
        flavorText = "The wheel only has one sure output: chaos."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4eb9a82-91f7-4741-a029-a07a3ff6af78.jpg?1783911264"
        ruling("2024-06-07", "Players decide and announce in turn order, starting with the active player, whether or not they want to exile their hand. Then, all players who chose to do so exile their hands and draw X cards. Players later in the turn order will know if players before them are exiling their hands, but they don't get to see what those players will exile to help them decide whether to exile their hands themselves.")
        ruling("2024-06-07", "You pay all costs and follow all normal timing rules for cards played this way. For example, if one of the exiled cards is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
