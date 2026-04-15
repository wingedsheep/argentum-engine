package com.wingedsheep.engine.legalactions.support

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId

/**
 * Build an [EnumerationTestDriver] with Player 1's zones populated to a
 * specific shape via state surgery.
 *
 * After init, this helper empties P1's hand back into the library and then
 * deterministically moves named cards from library into the requested zones.
 * That sequence is critical: without it, shuffle luck can leave extra copies
 * of a requested card sitting in hand, and other enumerators (most notably
 * `CastSpellEnumerator`) emit them as plain hand-casts that pollute
 * assertions about graveyard/exile/battlefield-source actions.
 *
 * @param hand cards to place in P1's hand
 * @param battlefield cards to place on P1's battlefield (untapped)
 * @param graveyard cards to place in P1's graveyard
 * @param exile cards to place in P1's exile zone
 * @param extraLibrary cards that must be present in the deck even though
 *   nothing places them anywhere — useful for top-of-library tests where the
 *   test does its own surgery after the helper returns
 * @param extraSetCards additional [CardDefinition]s to register beyond
 *   [TestCards.all] (e.g., cards from sets not already in TestCards)
 * @param atStep step to advance the game to before returning
 */
fun setupP1(
    hand: List<String> = emptyList(),
    battlefield: List<String> = emptyList(),
    graveyard: List<String> = emptyList(),
    exile: List<String> = emptyList(),
    extraLibrary: List<String> = emptyList(),
    extraSetCards: List<CardDefinition> = emptyList(),
    atStep: Step = Step.PRECOMBAT_MAIN
): EnumerationTestDriver {
    val driver = EnumerationTestDriver()
    driver.registerCards(TestCards.all)
    driver.registerCards(extraSetCards)
    val deckSpec = (hand + battlefield + graveyard + exile + extraLibrary)
        .groupingBy { it }.eachCount()
        .map { (name, count) -> name to (count + 4) }
        .toMutableList()
    deckSpec.add("Forest" to 30)
    driver.game.initMirrorMatch(
        deck = Deck.of(*deckSpec.toTypedArray()),
        skipMulligans = true
    )
    driver.game.passPriorityUntil(atStep)

    var state: GameState = driver.game.state
    val handKey = ZoneKey(driver.player1, Zone.HAND)
    val libraryKey = ZoneKey(driver.player1, Zone.LIBRARY)

    // Empty P1's hand back into library so we control hand contents exactly.
    for (handId in state.getZone(handKey).toList()) {
        state = state.moveToZone(handId, handKey, libraryKey)
    }

    fun pluck(name: String): EntityId =
        state.getZone(libraryKey).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        } ?: error("setupP1: '$name' not in P1's library — add it to the deck spec")

    fun moveAll(names: List<String>, dest: Zone) {
        val destKey = ZoneKey(driver.player1, dest)
        for (name in names) {
            state = state.moveToZone(pluck(name), libraryKey, destKey)
        }
    }
    moveAll(hand, Zone.HAND)
    moveAll(battlefield, Zone.BATTLEFIELD)
    moveAll(graveyard, Zone.GRAVEYARD)
    moveAll(exile, Zone.EXILE)
    driver.game.replaceState(state)
    return driver
}
