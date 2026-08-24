package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.FlippedCoinsThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.FlipAdditionalCoins
import com.wingedsheep.sdk.scripting.WinCoinFlips

/**
 * Shared application path for coin-flip replacement, consulted from [CoinFlipService] — the single
 * place a coin is actually flipped — so every coin in the game runs through the same filter.
 *
 * Two replacements live here:
 *
 * - [WinCoinFlips] (Edgar, King of Figaro's Two-Headed Coin) dictates the *result* (CR 705.3, "An
 *   effect may state that a coin flip has a certain result and/or that a certain player wins a coin
 *   flip … ignore the actual results and use the indicated results instead"). "Heads" is modeled as
 *   a won flip throughout the coin plumbing, so a forced win is a forced heads.
 * - [FlipAdditionalCoins] (Krark's Thumb) dictates *how many coins are flipped* in place of each one
 *   (CR 614.1a — an "instead" effect is a replacement effect), leaving the flipper to ignore all but
 *   one.
 *
 * The two compose without interacting: a forced win makes every coin in an enlarged batch heads, so
 * there is nothing left to ignore and the flipper is never asked.
 */
object CoinFlipModifiers {

    /**
     * Whether the coin-flip event [playerId] is about to make should be forced to a win — every
     * coin in it comes up heads (CR 705.3). True when [playerId] controls a permanent with a
     * [WinCoinFlips] static ability that applies to this flip: an "all coin flips" ability always
     * applies, while a `firstFlipEachTurn` ability (Edgar) applies only on the player's first
     * coin-flip event of the turn — detected via [FlippedCoinsThisTurnComponent], which
     * [markFlipped] sets after every flip regardless of any replacement.
     *
     * Uses projected controllers so a stolen coin-flip source routes the "you" to its current
     * controller.
     */
    fun shouldForceWin(state: GameState, cardRegistry: CardRegistry, playerId: EntityId): Boolean {
        val alreadyFlippedThisTurn = state.getEntity(playerId)?.has<FlippedCoinsThisTurnComponent>() == true
        return state.projectedState.getBattlefieldControlledBy(playerId).any { permanentId ->
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: return@any false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@any false
            cardDef.script.staticAbilities.any { ability ->
                ability is WinCoinFlips && (!ability.firstFlipEachTurn || !alreadyFlippedThisTurn)
            }
        }
    }

    /**
     * Record that [playerId] has flipped one or more coins this turn (idempotent). Set on every
     * flip — even ones no replacement affected — so a `firstFlipEachTurn` ability can tell that a
     * later flip that turn is no longer the first. Cleared at end of turn by CleanupPhaseManager.
     */
    fun markFlipped(state: GameState, playerId: EntityId): GameState =
        state.updateEntity(playerId) { it.with(FlippedCoinsThisTurnComponent) }

    /**
     * How many coins [playerId] really flips in place of each single coin — 1 when they control no
     * [FlipAdditionalCoins] source, 2 under one Krark's Thumb, 4 under two.
     *
     * Instances **multiply** rather than add: a second Thumb replaces each of the coins the first
     * one produced, so two Thumbs are "flip four coins and ignore three", not "flip three and ignore
     * two". A [FlipAdditionalCoins.coinsPerFlip] below 2 would flip fewer coins than the game asked
     * for, which no replacement may do, so such a value is ignored rather than shrinking the batch.
     *
     * Uses projected controllers so a stolen Krark's Thumb helps its current controller.
     */
    fun coinsPerFlip(state: GameState, cardRegistry: CardRegistry, playerId: EntityId): Int =
        state.projectedState.getBattlefieldControlledBy(playerId).fold(1) { coins, permanentId ->
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: return@fold coins
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@fold coins
            cardDef.script.staticAbilities
                .filterIsInstance<FlipAdditionalCoins>()
                .fold(coins) { running, ability -> running * ability.coinsPerFlip.coerceAtLeast(1) }
        }
}
