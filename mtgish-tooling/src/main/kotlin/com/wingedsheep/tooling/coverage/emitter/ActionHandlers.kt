package com.wingedsheep.tooling.coverage.emitter

/**
 * The per-`_Action` rendering registry — the heart of the "mapping" knowledge, split across themed
 * `*Handlers.kt` files. [EmitCtx.renderAction] looks a card's action up here.
 *
 * To add support for a new mtgish action: drop a `reg("TheActionTag") { node, args, tvar -> ... }`
 * line into whichever themed file fits (or add a new `*Handlers.kt` map and include it below). One
 * handler can answer several action tags, and one tag added here helps every set at once.
 */
internal val ACTION_HANDLERS: Map<String, ActionHandler> =
    damageDrawLifeHandlers + zoneHandlers + tapLayerStateHandlers + playerContinuousHandlers
