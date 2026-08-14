package com.wingedsheep.ai.engine.deck

/**
 * A deck a generator built for a seat that didn't bring one: the library, plus the commander when
 * the format wants one.
 *
 * The two travel together because they are one decision. They used to be two: the server resolved a
 * deck list from the seat's spec and read the commander off that spec separately, so a *generated*
 * commander deck had nowhere to put the commander it picked and commander-shaped formats had to be
 * refused. Anything that can answer "what does this seat play" answers both halves here.
 *
 * [deckList] **excludes** the commander, matching
 * [com.wingedsheep.sdk.model.Deck.cards] and `GameSession.addPlayer` — the commander starts in the
 * command zone, not the library (CR 903.6). Note the *wire* convention is the opposite: a submitted
 * deck list counts the commander, and the handlers strip it before game init.
 */
data class GeneratedDeck(
    /** Card names (or `Name#CollectorNumber` basic-land variants) to counts. Never the commander. */
    val deckList: Map<String, Int>,
    /** The designated commander, or null for an ordinary deck. */
    val commander: String? = null,
) {
    /** Total cards including the commander — the number Commander's exact-size rule counts. */
    val totalCards: Int get() = deckList.values.sum() + if (commander != null) 1 else 0

    /**
     * The deck list in *submission* form: the commander counted in with the rest.
     *
     * That is the convention every deck-submission path uses — a lobby seat's `submittedDeck` is
     * expected to contain its commander, and the match handlers strip one copy back out at game
     * start. Two things depend on it that a bare [deckList] would get wrong: the card count the
     * lobby shows next to the seat, and a limited lobby's derived sideboard, which is `pool −
     * deck` and would otherwise put the seat's commander in its sideboard as well as its command
     * zone.
     */
    val submissionList: Map<String, Int>
        get() = commander?.let { deckList + (it to (deckList[it] ?: 0) + 1) } ?: deckList
}
