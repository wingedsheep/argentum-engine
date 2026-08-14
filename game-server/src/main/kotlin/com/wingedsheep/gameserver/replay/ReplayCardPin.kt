package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.serialization.CardExporter
import com.wingedsheep.sdk.serialization.CardLoader
import org.slf4j.LoggerFactory

/**
 * Archives the compiled card definitions a recorded game ran on, and stacks them back over the live
 * corpus when that game is re-simulated.
 *
 * This is the single biggest reason a stored replay stops replaying. Cards in this engine are
 * *data* — a `cardDef { }` tree compiled at startup — and that data is the code the engine folds the
 * recorded action stream through. Fix a card's targeting, split one effect into two, re-word a
 * trigger, and every replay that ever used the card now folds through a different function than the
 * one that produced the recording. The action stream is still perfectly valid; it just describes a
 * game the current corpus can no longer produce.
 *
 * Pinning removes the whole class of failure for anything reachable from the decks: alongside the
 * inputs we store each distinct definition as compact JSON ([CardExporter.exportToCompactJson]), and
 * reconstruction resolves them through a child [CardRegistry] that shadows the live corpus, so the
 * replay folds through the card code it was recorded with no matter what the corpus looks like
 * today.
 *
 * **It is an overlay, not a sandbox.** Objects a game can conjure without naming them in a decklist
 * — predefined tokens, `CreateRandomCreatureTokenWithManaValue`'s registry sweep, cards fetched from
 * a sideboard by a wish — still resolve against the live corpus, and core engine changes (priority,
 * layers, SBAs) are not versioned by anything here at all. Those residual cases are what
 * [ReplayFingerprint] checkpoints detect and the materialized presentation stream
 * ([ReplayPresentation]) covers.
 */
object ReplayCardPin {

    private val logger = LoggerFactory.getLogger(ReplayCardPin::class.java)

    /**
     * Archive every distinct definition the [setup]'s decklists resolve to, as compact JSON.
     *
     * Deck entries are registry lookup keys (`"Lightning Bolt"`, or `"Plains#POR-196"` for a
     * collector-number variant), so we resolve them exactly the way [com.wingedsheep.engine.core.GameInitializer]
     * will. Entries that don't resolve are skipped rather than failing the save — a replay with a
     * partial pin is strictly better than no replay.
     */
    fun capture(registry: CardRegistry, setup: ReplaySetup): List<String> {
        val seen = LinkedHashMap<String, CardDefinition>()
        for (player in setup.players) {
            val deck = player.deck
            // Sideboards are in scope too: a wish can pull one onto the stack mid-game.
            val entries = deck.cards +
                deck.cardEntries.map { it.name } +
                deck.sideboard.map { it.name } +
                listOfNotNull(deck.commander, player.commanderCardName)
            for (entry in entries) {
                val card = registry.getCard(entry) ?: continue
                // Key by identity, not by the lookup string: two entries ("Plains", "Plains#POR-196")
                // can resolve to the same definition, and the registry re-derives its own keys on
                // registration anyway.
                seen.putIfAbsent(identityKey(card), card)
            }
        }
        return seen.values.map { CardExporter.exportToCompactJson(it) }
    }

    /**
     * A copy of [live] with [pinned] definitions layered on top, or [live] itself when nothing was
     * pinned (replays recorded before pinning existed, and dev sessions).
     *
     * Definitions that fail to decode are skipped with a warning: an archived card the current SDK
     * can no longer parse is exactly the situation where falling back to the live definition is the
     * best remaining option.
     */
    fun overlay(live: CardRegistry, pinned: List<String>): CardRegistry {
        if (pinned.isEmpty()) return live
        val decoded = pinned.mapNotNull { json ->
            runCatching { CardLoader.fromJsonPreservingIds(json) }
                .onFailure { logger.warn("Skipping unreadable pinned card definition in replay: {}", it.message) }
                .getOrNull()
        }
        if (decoded.isEmpty()) return live

        return CardRegistry(parent = live).apply { register(decoded) }
    }

    private fun identityKey(card: CardDefinition): String =
        listOfNotNull(card.name, card.setCode, card.metadata.collectorNumber).joinToString("#")
}
