package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dft.cards.RedshiftRocketeerChief
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Redshift, Rocketeer Chief (DFT #218) — {R}{G} Legendary Goblin Pilot, 2/3.
 *
 * Every printed clause has a paired test:
 *  - "Vigilance" — snapshot/keyword net; not re-tested here.
 *  - "{T}: Add X mana of any one color, where X is Redshift's power. Spend this mana only to
 *    activate abilities." — one *chosen* color, X of it, X read from **projected** power (so +1/+1
 *    counters raise it), tagged [ManaRestriction.AbilityActivationOnly], and it really does fund
 *    another source's activated ability.
 *  - "Exhaust — {10}{R}{G}: Put any number of permanent cards from your hand onto the battlefield."
 *    — the unbounded `ChooseAnyNumber` selection over permanent cards only (an instant in hand is
 *    not an option), zero is a legal choice, and the exhaust ability is spent after one activation.
 */
class RedshiftRocketeerChiefScenarioTest : ScenarioTestBase() {

    private val manaAbilityId = RedshiftRocketeerChief.activatedAbilities.single { it.isManaAbility }.id
    private val exhaustAbilityId = RedshiftRocketeerChief.activatedAbilities.single { it.isExhaust }.id

    init {
        // A sink for the restricted mana: an activated ability with a plain {2} mana cost, so we can
        // prove Redshift's mana pays for an *ability* of another source.
        cardRegistry.register(
            card("Redshift Test Cache") {
                manaCost = "{0}"
                typeLine = "Artifact"
                oracleText = "{2}: You gain 3 life."
                activatedAbility {
                    cost = Costs.Mana("{2}")
                    effect = Effects.GainLife(3)
                    timing = TimingRule.InstantSpeed
                }
            }
        )

        /** Tap Redshift for mana, choosing [color]; returns the resulting pool. */
        fun TestGame.tapForMana(redshift: EntityId, color: Color): ManaPoolComponent {
            execute(ActivateAbility(player1Id, redshift, manaAbilityId)).error shouldBe null
            val decision = getPendingDecision()
            decision.shouldBeInstanceOf<ChooseColorDecision>()
            submitDecision(ColorChosenResponse(decision.id, color)).error shouldBe null
            return state.getEntity(player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        }

        context("Redshift, Rocketeer Chief") {

            test("{T} adds power-many mana of one chosen color, restricted to ability activation") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                val pool = game.tapForMana(redshift, Color.RED)

                withClue("Redshift is a 2/3, so X = 2 — all of the one chosen color") {
                    pool.restrictedMana.size shouldBe 2
                    pool.restrictedMana.all { it.color == Color.RED } shouldBe true
                }
                withClue("the mana is spendable only on ability activations") {
                    pool.restrictedMana.all {
                        it.restriction == ManaRestriction.AbilityActivationOnly
                    } shouldBe true
                }
                withClue("nothing unrestricted was added") {
                    pool.red shouldBe 0
                    pool.green shouldBe 0
                    pool.colorless shouldBe 0
                }
                withClue("{T} taps Redshift") {
                    game.state.getEntity(redshift)?.has<TappedComponent>() shouldBe true
                }
            }

            test("X tracks Redshift's projected power, not its printed power") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                // Two +1/+1 counters — a 2/3 becomes a projected 4/5.
                game.state = game.state.updateEntity(redshift) { container ->
                    container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }

                val pool = game.tapForMana(redshift, Color.GREEN)

                withClue("a 4/5 Redshift adds four mana") {
                    pool.restrictedMana.size shouldBe 4
                    pool.restrictedMana.all { it.color == Color.GREEN } shouldBe true
                }
            }

            test("the restricted mana pays for another source's activated ability") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    .withCardOnBattlefield(1, "Redshift Test Cache")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                game.tapForMana(redshift, Color.RED).restrictedMana.size shouldBe 2

                val cache = game.findPermanent("Redshift Test Cache")!!
                val gainId = cardRegistry.getCard("Redshift Test Cache")!!.activatedAbilities.single().id
                val lifeBefore = game.getLifeTotal(1)

                game.execute(ActivateAbility(game.player1Id, cache, gainId)).error shouldBe null
                withClue("the two restricted mana paid the {2} activation cost") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                        ?.restrictedMana?.size shouldBe 0
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe lifeBefore + 3
            }

            test("exhaust puts any number of permanent cards from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    // {10}{R}{G}: eleven Mountains cover {10}{R}, the Forest covers {G}.
                    .withLandsOnBattlefield(1, "Mountain", 11)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")   // permanent card
                    .withCardInHand(1, "Island")          // permanent card (land)
                    .withCardInHand(1, "Lightning Bolt")  // NOT a permanent card
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                game.execute(
                    ActivateAbility(game.player1Id, redshift, exhaustAbilityId)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the two permanent cards are selectable — Lightning Bolt is not") {
                    decision.options.size shouldBe 2
                }

                // "Any number" means both at once, not one at a time.
                game.selectCards(decision.options).error shouldBe null
                game.resolveStack()

                withClue("both permanent cards entered the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Island") shouldBe true
                }
                withClue("the instant stayed in hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                }
            }

            test("exhaust may be declined — any number includes zero") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 11)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                game.execute(
                    ActivateAbility(game.player1Id, redshift, exhaustAbilityId)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("nothing was put onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("the card is still in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("the exhaust ability may be activated only once") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Redshift, Rocketeer Chief", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 22)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val redshift = game.findPermanent("Redshift, Rocketeer Chief")!!
                game.execute(
                    ActivateAbility(game.player1Id, redshift, exhaustAbilityId)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("mana is still available, but the exhaust ability is spent for this object") {
                    game.execute(
                        ActivateAbility(game.player1Id, redshift, exhaustAbilityId)
                    ).error shouldNotBe null
                }
                withClue("and it is no longer offered as a legal action") {
                    game.getLegalActions(1).none { info ->
                        (info.action as? ActivateAbility)?.abilityId == exhaustAbilityId
                    } shouldBe true
                }
            }
        }
    }
}
