package com.wingedsheep.gameserver.deck

import com.wingedsheep.ai.engine.deck.CommanderDeckGenerator
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * The contract between the two halves of AI Commander support: whatever
 * [CommanderDeckGenerator] builds, [DeckValidator] has to accept.
 *
 * `CommanderDeckGeneratorTest` pins each construction rule against a synthetic pool, which is where
 * the rules are readable. This is the other end — the *real* card base, the *real* validator — and
 * it's the one that catches the mismatches a synthetic pool can't have: a card whose Scryfall
 * colour-identity override disagrees with the heuristic, a "deck can have any number" card, a
 * commander the eligibility rule and the validator read differently. A generated deck that fails
 * here is a lobby that refuses to start.
 */
// RANDOM_PORT rather than the default MOCK: the app's WebSocket config needs a real servlet
// container to start at all.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeneratedCommanderDeckLegalityTest(
    @param:Autowired private val commanderDeckGenerator: CommanderDeckGenerator,
    @param:Autowired private val deckValidator: DeckValidator,
) : FunSpec({

    /**
     * Enough builds to cross several commanders and colour identities; each one re-rolls, so a
     * rule the builder gets wrong for one identity shape shows up within a handful of draws.
     */
    val builds = 10

    listOf(DeckFormat.COMMANDER, DeckFormat.BRAWL, DeckFormat.STANDARD_BRAWL).forEach { format ->
        test("every generated ${format.displayName} deck passes the deck validator") {
            val decks = (1..builds).map { commanderDeckGenerator.generate(emptyList(), format) }

            if (decks.all { it == null }) {
                // A format the implemented card base can't supply a commander for. Only Standard
                // Brawl is allowed to be in that position today — no implemented set is
                // Standard-legal — and it stops being exempt the moment one is, because these
                // assertions then start running against real decks.
                withClue("no ${format.displayName} deck could be built from the whole card base") {
                    format shouldBe DeckFormat.STANDARD_BRAWL
                }
                return@test
            }

            decks.forEach { generated ->
                withClue("a ${format.displayName} build came back empty after an earlier one succeeded") {
                    generated shouldNotBe null
                }
                val deck = Deck(
                    cards = generated!!.deckList.flatMap { (name, count) -> List(count) { name } },
                    commander = generated.commander,
                )

                val result = deckValidator.validate(deck, format)

                withClue("${generated.commander}: ${result.errors.joinToString { it.message }}") {
                    result.valid shouldBe true
                }
            }
        }
    }
})
