package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Modal double-faced cards whose **back face is a permanent** (CR 712.3) — the Marvel Super Heroes
 * hero cycle. Engine-level, so it covers the mechanic across several cards rather than one card.
 *
 * These are modal *and* transforming, which CR 712.3 allows in as many words: *"Modal double-faced
 * cards have a Magic card face on each side. These faces are usually independent from one another,
 * but they may have an ability that allows them to 'transform' or 'convert' on either face."* Three
 * things follow, and each is pinned below:
 *
 *  1. **Either face is castable from hand.** CR 712.11b — the caster chooses a face before the card
 *     goes on the stack; CR 712.11c — only that face is evaluated for legality. The back is cast for
 *     its own printed mana cost and resolves onto the battlefield back face up (CR 712.13).
 *  2. **The front can still transform into the back**, through its printed activated ability.
 *  3. **A modal back face keeps its own mana value.** CR 712.8f says a modal DFC on the battlefield
 *     "has only the characteristics of the face that's up", with *no* mana-value exception — unlike
 *     CR 712.8e, which keeps the front's for a nonmodal DFC. So The Sensational She-Hulk is mana
 *     value 6, not Jennifer Walters' 2, however she got there — on the battlefield *and* on the
 *     stack, which is what the cast event and the turn's cast history report to every "a spell with
 *     mana value N" payoff. (The other transformed-cast route, disturb, is the opposite: CR 712.8c
 *     computes a nonmodal transformed spell's mana value from the *front* face.)
 *  4. **Timing comes off the face being cast** (CR 712.11c), so a sorcery-speed permanent back is
 *     not castable on an opponent's turn while a back with flash is — regardless of the front.
 */
class ModalDfcPermanentBackTest : ScenarioTestBase() {

    init {
        // A modal DFC whose two faces *disagree* about flash: the front has it, the back does not.
        // Nothing in the MSH cycle splits that way (King T'Challa prints flash on both faces), so
        // without this card a timing gate that read the front face by mistake would still pass.
        val flashFront = card("Quickstep Courier") {
            manaCost = "{1}{U}"
            colorIdentity = "U"
            typeLine = "Creature — Human Scout"
            power = 2
            toughness = 1
            keywords(Keyword.FLASH)
        }
        val groundedBack = card("Grounded Colossus") {
            manaCost = "{4}{U}{U}"
            colorIdentity = "U"
            typeLine = "Creature — Golem"
            power = 6
            toughness = 6
        }
        cardRegistry.register(
            CardDefinition.modalDoubleFacedPermanent(frontFace = flashFront, backFace = groundedBack)
        )

        /** Six lands covering `{3}{G}{W}{W}` (and, with three Forests, `{1}{W}` too). */
        fun sixLands() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Jennifer Walters")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.manaValueOf(cardName: String): Int =
            state.getEntity(findPermanent(cardName)!!)!!.get<CardComponent>()!!.manaCost.cmc

        test("the front face is castable from hand for its own cost") {
            val game = sixLands()

            game.castSpell(1, "Jennifer Walters").error shouldBe null
            game.resolveStack()

            withClue("cast normally, the card enters as its front face (CR 712.13)") {
                game.isOnBattlefield("Jennifer Walters") shouldBe true
                game.isOnBattlefield("The Sensational She-Hulk") shouldBe false
            }
            withClue("front face up → the front's mana value") {
                game.manaValueOf("Jennifer Walters") shouldBe 2
            }
        }

        test("the back face is castable from hand for its own mana cost (CR 712.11b)") {
            val game = sixLands()
            val cardId = game.findCardsInHand(1, "Jennifer Walters").first()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    cardId,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.MODAL_BACK_FACE
                )
            )
            withClue("casting the back face is legal with {3}{G}{W}{W} available") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("it resolved onto the battlefield back face up (CR 712.13)") {
                game.isOnBattlefield("The Sensational She-Hulk") shouldBe true
                game.isOnBattlefield("Jennifer Walters") shouldBe false
            }
            withClue("with the back face's P/T, not the front's 2/3") {
                val hulk = game.findPermanent("The Sensational She-Hulk")!!
                val stats = game.state.getEntity(hulk)!!.get<CardComponent>()!!.baseStats
                stats?.basePower shouldBe 6
                stats?.baseToughness shouldBe 6
            }
            withClue("CR 712.8f: a modal back face keeps its *own* mana value, not the front's 2") {
                game.manaValueOf("The Sensational She-Hulk") shouldBe 6
            }
            withClue("the engine still knows which face is up, so it can transform back") {
                game.state.getEntity(game.findPermanent("The Sensational She-Hulk")!!)!!
                    .get<DoubleFacedComponent>()?.currentFace shouldBe DoubleFacedComponent.Face.BACK
            }
        }

        test("the back-face *spell* reports the back's mana value, not the front's (CR 712.8f)") {
            // CR 712.8f gives a modal double-faced spell on the stack "only the characteristics of
            // the face that's up", with none of CR 712.8c's front-face mana-value exception. Both
            // readouts feed rules text, not just the log: the cast event drives
            // TRIGGERING_SPELL_MANA_VALUE (Kellan, the Kid's "equal or lesser mana value") and the
            // cast record drives the turn's "you cast a spell with mana value N" history.
            val game = sixLands()
            val cardId = game.findCardsInHand(1, "Jennifer Walters").first()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    cardId,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.MODAL_BACK_FACE
                )
            )
            result.error shouldBe null

            val castEvent = result.events.filterIsInstance<SpellCastEvent>().firstOrNull()
            withClue("the cast emitted a SpellCastEvent") { castEvent.shouldNotBeNull() }
            withClue("the spell on the stack is The Sensational She-Hulk, mana value 6") {
                castEvent!!.manaValue shouldBe 6
            }
            withClue("and the turn's cast history agrees") {
                game.state.spellsCastThisTurnByPlayer[game.player1Id]!!.last().manaValue shouldBe 6
            }
        }

        test("a front-face cast still reports the front's mana value") {
            // The regression guard for the branch above: only a *modal* back-face cast switches.
            val game = sixLands()

            val result = game.castSpell(1, "Jennifer Walters")
            result.error shouldBe null

            val castEvent = result.events.filterIsInstance<SpellCastEvent>().firstOrNull()
            castEvent.shouldNotBeNull()
            castEvent.manaValue shouldBe 2
            game.state.spellsCastThisTurnByPlayer[game.player1Id]!!.last().manaValue shouldBe 2
        }

        test("the back-face cast is offered as a legal action alongside the front") {
            val game = sixLands()

            val descriptions = game.getLegalActions(1).map { it.description }
            withClue("both faces belong in the action menu (CR 712.11b): $descriptions") {
                descriptions.any { "Jennifer Walters" in it && "Cast" in it } shouldBe true
                descriptions.any { "The Sensational She-Hulk" in it && "Cast" in it } shouldBe true
            }
        }

        test("the back-face cast is not offered without enough mana for the back's cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Jennifer Walters")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val affordable = game.getLegalActions(1)
                .filter { "The Sensational She-Hulk" in it.description }
            withClue("two Plains cannot pay {3}{G}{W}{W}, so the offer is not affordable") {
                affordable.none { it.isAffordable } shouldBe true
            }
            withClue("but the front face is castable for {1}{W}") {
                game.castSpell(1, "Jennifer Walters").error shouldBe null
            }
        }

        /**
         * Hand [cardName] to Player 1 on Player 2's turn, with eight lands of each relevant colour,
         * and pass priority so Player 1 holds it at instant speed only.
         */
        fun opponentsTurnWith(cardName: String): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, cardName)
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passPriority() // active player (Player2) passes; Player1 gets priority
            return game
        }

        fun TestGame.castBackFaceOf(cardName: String) = execute(
            CastSpell(
                player1Id,
                findCardsInHand(1, cardName).first(),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.MODAL_BACK_FACE
            )
        )

        test("a sorcery-speed back face is not castable on an opponent's turn (CR 712.11c)") {
            val game = opponentsTurnWith("Jennifer Walters")

            withClue("The Sensational She-Hulk is a creature with no flash, so it isn't offered") {
                game.getLegalActions(1).none { "The Sensational She-Hulk" in it.description } shouldBe true
            }
            withClue("and the handler refuses it too, not just the enumerator") {
                (game.castBackFaceOf("Jennifer Walters").error != null) shouldBe true
            }
        }

        test("a back face with flash is castable on an opponent's turn (CR 712.11c)") {
            val game = opponentsTurnWith("King T'Challa")

            withClue("Black Panther, Hope Enduring has flash, so the back face is offered") {
                game.getLegalActions(1)
                    .any { "Black Panther" in it.description && it.isAffordable } shouldBe true
            }
            game.castBackFaceOf("King T'Challa").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Black Panther, Hope Enduring") shouldBe true
        }

        test("timing reads the face being cast, not the front face (CR 712.11c)") {
            // Quickstep Courier has flash; its back, Grounded Colossus, does not. Only the face
            // being cast is evaluated, so on an opponent's turn the *front* is castable and the
            // *back* is not — the front's flash grants the back nothing.
            val game = opponentsTurnWith("Quickstep Courier")

            withClue("the flash front is castable at instant speed") {
                game.getLegalActions(1)
                    .any { "Quickstep Courier" in it.description && it.isAffordable } shouldBe true
            }
            withClue("but its non-flash back is not, despite the front's flash") {
                game.getLegalActions(1).none { "Grounded Colossus" in it.description } shouldBe true
                (game.castBackFaceOf("Quickstep Courier").error != null) shouldBe true
            }
        }

        test("transforming still reaches the same back face, with the back's mana value") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jennifer Walters", summoningSickness = false)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val jennifer = game.findPermanent("Jennifer Walters")!!
            val transformAbility = cardRegistry.getCard("Jennifer Walters")!!
                .script.activatedAbilities.first().id
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = jennifer, abilityId = transformAbility)
            )
            withClue("activating the transform ability should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("CR 712.3 — a modal DFC may still carry a transform ability") {
                game.isOnBattlefield("The Sensational She-Hulk") shouldBe true
            }
            withClue("however she got there, the face that's up sets the mana value (CR 712.8f)") {
                game.manaValueOf("The Sensational She-Hulk") shouldBe 6
            }
        }

        test("every MSH hero back face carries a real cost and no color indicator") {
            // The colors of these backs come from their own mana cost, which is why the printed
            // cards show no CR 204 color indicator. `modalDoubleFacedPermanent` enforces both, so
            // this is really a guard that the whole cycle goes through that factory.
            val heroes = listOf(
                "Jennifer Walters" to "The Sensational She-Hulk",
                "Bruce Banner" to "The Incredible Hulk",
                "King T'Challa" to "Black Panther, Hope Enduring",
                "Tony Stark" to "The Invincible Iron Man",
                "Monica Rambeau" to "Photon, Living Light",
            )
            heroes.forEach { (frontName, backName) ->
                val front = cardRegistry.getCard(frontName)!!
                withClue("$frontName is a modal DFC") {
                    front.layout shouldBe com.wingedsheep.sdk.model.CardLayout.MODAL_DFC
                }
                val back = front.backFace
                withClue("$frontName has $backName as a permanent back face") {
                    back?.name shouldBe backName
                    back?.isPermanent shouldBe true
                }
                withClue("$backName is castable, so it carries its own mana cost") {
                    back!!.manaCost.isEmpty() shouldBe false
                }
                withClue("$backName takes no color indicator — its colors come from that cost") {
                    back!!.colorIndicator shouldBe null
                }
                withClue("$backName's colors match its mana cost") {
                    back!!.colors shouldBe back.manaCost.colors
                }
            }
        }
    }
}
