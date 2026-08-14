package com.wingedsheep.ai.engine.deck

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition

/**
 * The card pool a constructed build draws from: every card legal in a [DeckFormat], plus the basic
 * lands whose art the finished deck should use.
 *
 * Shared by [ConstructedDeckGenerator] (60-card constructed) and [CommanderDeckGenerator]
 * (singleton commander shapes) because "what may I build out of" is the same question for both and
 * the answer has two non-obvious parts — reprint resolution and the empty-set-codes default — that
 * are easy to get subtly different in a second copy.
 */
class FormatCardPool(
    private val boosterGenerator: BoosterGenerator,
    private val cardRegistry: CardRegistry,
) {
    /**
     * Every card legal in [format], scoped to [setCodes] when non-empty.
     *
     * Set scoping reads [BoosterGenerator.SetConfig.cards] *and* resolves the set's reprint
     * [BoosterGenerator.SetConfig.printings] rows through the registry: a reprint's canonical
     * `CardDefinition` lives in its earliest printing's set, so a set that is mostly reprints
     * (a core set, a precon set) would otherwise look almost empty.
     *
     * @throws IllegalArgumentException if a set code doesn't resolve.
     */
    fun legalPool(setCodes: List<String>, format: DeckFormat): List<CardDefinition> {
        val candidates = if (setCodes.isEmpty()) {
            cardRegistry.allCardNames().mapNotNull { cardRegistry.getCard(it) }
        } else {
            setCodes.flatMap { setCode ->
                val config = boosterGenerator.availableSets[setCode]
                    ?: throw IllegalArgumentException("Unknown set code: $setCode")
                config.cards + config.printings.mapNotNull { cardRegistry.getCard(it.name) }
            }
        }
        return candidates
            .distinctBy { it.name }
            // An empty `legalFormats` means we have no Scryfall legality data for the card at all
            // (custom/unreleased content). Excluding it keeps the deck honestly format-legal
            // rather than silently smuggling unknowns in.
            .filter { format in it.legalFormats }
            // A meld result is format-legal on Scryfall (the meld *parts* are the legal card) but
            // is never a card you own — building it into a deck just deals a permanent that can't
            // be cast.
            .filterNot { it.meldResult }
    }

    /**
     * Basic-land printings to pin the deck's basics to, so their art matches the pool the deck was
     * built from. An all-sets build has no set to match, so it takes whatever the generator has
     * registered first.
     */
    fun basicLands(setCodes: List<String>): List<CardDefinition> =
        if (setCodes.isEmpty()) {
            boosterGenerator.availableSets.values.firstOrNull()?.basicLands.orEmpty()
        } else {
            boosterGenerator.getBasicLands(setCodes).values.toList()
        }
}
