package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Aftermath (CR 702.127a) — three static abilities of one half of a split card:
 *  - "You may cast this half of this split card from your graveyard";
 *  - "This half of this split card can't be cast from any zone other than a graveyard";
 *  - "If this spell was cast from a graveyard, exile it instead of putting it anywhere else any
 *    time it would leave the stack."
 *
 * Exercised through an inline split card whose second half has aftermath: Test Rise (a sorcery,
 * "you gain 3 life") // Test Fall (an instant with aftermath, "deals 2 damage to target creature").
 * Every rule — and the Amonkhet rulings that the first half is *not* castable from the graveyard
 * and that the exile applies whether the spell resolves, is countered, or fizzles — gets an
 * assertion, both on what the enumerator offers and on what the validator accepts, because the
 * `CastSpell` action is client-supplied.
 */
class AftermathScenarioTest : ScenarioTestBase() {

    private val riseFall = card("Test Rise // Test Fall") {
        layout = CardLayout.SPLIT
        colorIdentity = "WR"

        face("Test Rise") {
            manaCost = "{1}{W}"
            typeLine = "Sorcery"
            oracleText = "You gain 3 life."
            spell {
                effect = Effects.GainLife(3)
            }
        }

        face("Test Fall") {
            manaCost = "{2}{R}"
            typeLine = "Instant"
            oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
                "Test Fall deals 2 damage to target creature."
            keywords(Keyword.AFTERMATH)
            spell {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(2, creature)
            }
        }
    }

    private val rise = 0
    private val fall = 1

    init {
        cardRegistry.register(riseFall)

        fun boardWithCardIn(zone: String): TestGame {
            val builder = scenario()
                .withPlayers("Caster", "Opponent")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withLandsOnBattlefield(2, "Island", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(2, "Counterspell")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            when (zone) {
                "hand" -> builder.withCardInHand(1, "Test Rise // Test Fall")
                "graveyard" -> builder.withCardInGraveyard(1, "Test Rise // Test Fall")
            }
            return builder.build()
        }

        fun TestGame.splitCardId(): EntityId =
            (state.getHand(player1Id) + state.getGraveyard(player1Id)).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Test Rise // Test Fall"
            }

        fun TestGame.castFall(): ExecutionResult =
            execute(
                CastSpell(
                    player1Id, splitCardId(), faceIndex = fall,
                    targets = listOf(ChosenTarget.Permanent(findPermanent("Hill Giant")!!))
                )
            )

        context("CR 702.127a — the aftermath half can't be cast from any zone other than a graveyard") {

            test("from hand only the other half is offered") {
                val game = boardWithCardIn("hand")
                val descriptions = game.getLegalActions(1).map { it.description }

                withClue("the non-aftermath half is an ordinary split half, castable from hand") {
                    descriptions.contains("Cast Test Rise") shouldBe true
                }
                withClue("the aftermath half is not offered from hand: $descriptions") {
                    descriptions.none { it.startsWith("Cast Test Fall") } shouldBe true
                }
            }

            test("a hand-constructed cast of the aftermath half from hand is rejected") {
                val game = boardWithCardIn("hand")
                val result = game.castFall()

                result.error.shouldNotBeNull()
                withClue("the card stays in hand") {
                    game.state.getHand(game.player1Id).contains(game.splitCardId()) shouldBe true
                }
            }
        }

        context("CR 702.127a — the aftermath half may be cast from your graveyard") {

            test("the graveyard offers the aftermath half, labelled and sourced from the graveyard") {
                val game = boardWithCardIn("graveyard")
                val offer = game.getLegalActions(1).firstOrNull { it.description == "Cast Test Fall (Aftermath)" }

                offer.shouldNotBeNull()
                offer.sourceZone shouldBe "GRAVEYARD"
                (offer.action as CastSpell).faceIndex shouldBe fall
                withClue("the aftermath half's own target requirement is surfaced") {
                    offer.requiresTargets shouldBe true
                }
            }

            test("the non-aftermath half gains no graveyard permission — not offered, and rejected") {
                val game = boardWithCardIn("graveyard")

                withClue("only the aftermath half is offered from the graveyard") {
                    game.getLegalActions(1).none { it.description.startsWith("Cast Test Rise") } shouldBe true
                }
                val result = game.execute(CastSpell(game.player1Id, game.splitCardId(), faceIndex = rise))
                result.error.shouldNotBeNull()
                game.state.getGraveyard(game.player1Id).contains(game.splitCardId()) shouldBe true
            }

            test("only its owner may cast it from their graveyard") {
                val game = boardWithCardIn("graveyard")
                val cardId = game.splitCardId()
                game.passPriority()

                val result = game.execute(
                    CastSpell(
                        game.player2Id, cardId, faceIndex = fall,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                    )
                )
                result.error.shouldNotBeNull()
            }

            test("the aftermath half is cast for its own mana cost and resolves its own effect") {
                val game = boardWithCardIn("graveyard")
                val cardId = game.splitCardId()

                val result = game.castFall()
                withClue("casting Test Fall from the graveyard: ${result.error}") { result.error.shouldBeNull() }
                withClue("{2}{R} — the half's own printed cost — taps three of the eight lands") {
                    game.state.getBattlefield(game.player1Id).count {
                        game.state.getEntity(it)?.has<TappedComponent>() == true
                    } shouldBe 3
                }
                game.resolveStack()

                withClue("Hill Giant (3/3) took 2 damage") {
                    game.state.getEntity(game.findPermanent("Hill Giant")!!)?.get<DamageComponent>()?.amount shouldBe 2
                }
                withClue("CR 702.127a — a graveyard-cast aftermath spell is exiled when it resolves") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe true
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe false
                }
            }
        }

        context("CR 702.127a — exiled instead of anywhere else any time it would leave the stack") {

            test("countered — exiled, not put into the graveyard") {
                val game = boardWithCardIn("graveyard")
                val cardId = game.splitCardId()

                game.castFall().error.shouldBeNull()
                game.passPriority()
                val counter = game.state.getHand(game.player2Id).first()
                game.execute(
                    CastSpell(game.player2Id, counter, targets = listOf(ChosenTarget.Spell(cardId)))
                ).error.shouldBeNull()
                game.resolveStack()

                withClue("Hill Giant was never dealt damage") { game.findPermanent("Hill Giant").shouldNotBeNull() }
                withClue("the countered aftermath spell is exiled") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe true
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe false
                }
            }

            test("fizzled — exiled, not put into the graveyard") {
                val game = boardWithCardIn("graveyard")
                val cardId = game.splitCardId()

                game.castFall().error.shouldBeNull()
                // Holding priority, the caster bolts Test Fall's only target away, so the target is
                // illegal on resolution (CR 608.2b) and the spell doesn't resolve.
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error.shouldBeNull()
                game.resolveStack()

                withClue("Hill Giant died to the Bolt before Test Fall could resolve") {
                    game.findPermanent("Hill Giant").shouldBeNull()
                }
                withClue("the fizzled aftermath spell is exiled") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe true
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe false
                }
            }
        }

        context("the non-aftermath half cast from hand is an ordinary spell") {

            test("Test Rise resolves and goes to the graveyard, then Test Fall is castable from there") {
                val game = boardWithCardIn("hand")
                val cardId = game.splitCardId()

                game.execute(CastSpell(game.player1Id, cardId, faceIndex = rise)).error.shouldBeNull()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 23
                withClue("a split half cast from hand goes to its owner's graveyard as usual") {
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe true
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe false
                }
                withClue("…from where its aftermath half can now be cast") {
                    game.getLegalActions(1).any { it.description == "Cast Test Fall (Aftermath)" } shouldBe true
                }
                game.castFall().error.shouldBeNull()
                game.resolveStack()
                game.state.getExile(game.player1Id).contains(cardId) shouldBe true
            }
        }
    }
}
