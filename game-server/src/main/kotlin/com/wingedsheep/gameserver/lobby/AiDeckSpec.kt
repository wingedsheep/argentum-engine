package com.wingedsheep.gameserver.lobby

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the AI seat in a quick-game lobby should play.
 *
 * Before this the answer was always "a 40-card sealed deck from whatever set the human picked",
 * decided at game start and not configurable at all. This is the host's answer to the same
 * question, held on [QuickGameLobby.aiDeckSpec] and resolved into an actual deck list by
 * [com.wingedsheep.gameserver.ai.RandomDeckResolver] when the game starts.
 *
 * Three variants rather than the four sources the UI offers, deliberately: "an example deck",
 * "one of my saved decks" and "a decklist I pasted" all reduce to a list of card names on the
 * client, exactly as they already do for the *human's* deck submission. Only [Fixed] crosses the
 * wire for all three, so adding a fourth way to arrive at a decklist needs no server change.
 *
 * Resolution is deferred to game start (rather than resolved eagerly on selection) so that
 * changing the lobby's format re-rolls the AI's deck under the new restriction, and so a
 * generated deck is as fresh as the human's own random pool.
 */
@Serializable
sealed interface AiDeckSpec {

    /**
     * The historical behaviour: the server picks for you. A generated deck mirroring the human's
     * set for limited-shaped lobbies, a format-legal constructed deck when the lobby carries a
     * deck-format restriction, or a singleton deck behind a commander the builder picks itself when
     * the lobby runs Commander rules.
     */
    @Serializable
    @SerialName("auto")
    data object Auto : AiDeckSpec

    /**
     * Build the AI a deck from [setCodes], using the same heuristics as the Auto path but with the
     * card pool pinned to the host's choice — "give me something out of Onslaught block".
     *
     * Empty is treated as [Auto] by the resolver rather than rejected, so a host who clears every
     * set gets the default instead of a failed game start.
     */
    @Serializable
    @SerialName("sets")
    data class Sets(val setCodes: List<String>) : AiDeckSpec

    /**
     * Play this exact decklist. Covers the example decks, the host's saved decks and a pasted list.
     *
     * [label] is what the lobby shows next to the AI seat ("Mono-Red Burn"); it is display-only and
     * never round-trips into the engine.
     *
     * The only source that can arrive at a Commander lobby *without* a commander — the generated
     * ones choose their own — which is why the ready-up gate asks about this variant specifically.
     */
    @Serializable
    @SerialName("deck")
    data class Fixed(
        val deckList: Map<String, Int>,
        val label: String = "Custom",
        /** Designated commander for commander-shaped formats; absent for ordinary decks. */
        val commander: String? = null,
    ) : AiDeckSpec
}

/**
 * The lobby-broadcast projection of an [AiDeckSpec].
 *
 * Deliberately *not* the spec itself: [AiDeckSpec.Fixed] carries a full decklist, and the lobby
 * state is re-broadcast on every change (deck submitted, ready toggled, format changed). Echoing a
 * 60-card map back on each of those is pure waste when the only thing the UI renders is a label and
 * a card count. The host's own selection lives in client state; this is what re-hydrates it after a
 * reconnect.
 */
@Serializable
data class AiDeckSpecView(
    /** Discriminator matching the [AiDeckSpec] variants: `auto`, `sets` or `deck`. */
    val kind: String,
    /** Chosen sets, for [AiDeckSpec.Sets]; empty otherwise. */
    val setCodes: List<String> = emptyList(),
    /** Display label for [AiDeckSpec.Fixed]; null otherwise. */
    val label: String? = null,
    /** Card count for [AiDeckSpec.Fixed]; 0 otherwise. */
    val cardCount: Int = 0,
    /** Designated commander for [AiDeckSpec.Fixed]; null otherwise. */
    val commander: String? = null,
) {
    companion object {
        fun of(spec: AiDeckSpec): AiDeckSpecView = when (spec) {
            is AiDeckSpec.Auto -> AiDeckSpecView(kind = "auto")
            is AiDeckSpec.Sets -> AiDeckSpecView(kind = "sets", setCodes = spec.setCodes)
            is AiDeckSpec.Fixed -> AiDeckSpecView(
                kind = "deck",
                label = spec.label,
                cardCount = spec.deckList.values.sum() + if (spec.commander != null) 1 else 0,
                commander = spec.commander,
            )
        }
    }
}
