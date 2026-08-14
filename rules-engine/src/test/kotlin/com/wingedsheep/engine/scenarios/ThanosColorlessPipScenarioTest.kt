package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thanos, the Mad Titan — "Power-up — {C}{W}{U}{B}{R}{G}: Put two +1/+1 counters on Thanos. Choose
 * odd or even. Destroy each other creature with mana value of the chosen quality."
 *
 * Two things about him are unlike the rest of the power-up cycle, and each gets its own context:
 *
 *  - **The `{C}`** is a *colorless* mana symbol (CR 107.4c), not generic: it can only be paid with
 *    colorless mana, and no number of colored sources covers it. Reduced by his own `{R}{W}{B}` on
 *    the turn he enters, the ability asks for `{C}{U}{G}`. Both halves of that are asserted
 *    separately because they run through different code: the enumerator's affordability check (what
 *    the player is offered) and the handler's payment (what the engine accepts if the action is
 *    submitted directly).
 *  - **The odd/even sweep** is a [com.wingedsheep.sdk.scripting.effects.ModalEffect] nested inside
 *    the ability's `Composite`, so it takes the resolution-time mode-picking path of CR 603.3c —
 *    the choice is made as the ability resolves, not as it is activated, and it has to survive being
 *    resumed from inside a composite continuation. `notSourceItself()` is load-bearing there: Thanos
 *    is mana value 3, so "odd" would otherwise destroy him along with the rest of the board.
 */
class ThanosColorlessPipScenarioTest : ScenarioTestBase() {

    private val abilityId
        get() = cardRegistry.getCard("Thanos, the Mad Titan")!!.script.activatedAbilities[0].id

    private fun TestGame.powerUp() =
        getLegalActions(1).firstOrNull { it.description.startsWith("Power-up —") }

    /** Activate the power-up and pay for it, leaving the ability on the stack ready to resolve. */
    private fun TestGame.activatePowerUp(thanos: EntityId) {
        execute(ActivateAbility(player1Id, thanos, abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    /**
     * Vanilla bodies at one mana value each, so the sweep's parity test is the only thing that can
     * decide which of them dies. Defined inline rather than picked from a set: `ScenarioTestBase`
     * only loads the registered sets, and pinning the mana values here keeps the test readable.
     */
    private fun bystander(name: String, manaCost: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = setOf(Subtype.HUMAN),
        power = 1,
        toughness = 1
    )

    init {
        cardRegistry.register(bystander("Odd Bystander", "{2}{G}"))
        cardRegistry.register(bystander("Even Bystander", "{1}{G}"))
        cardRegistry.register(bystander("Nil Bystander", ""))

        context("Thanos, the Mad Titan — the {C} pip") {

            test("colored sources alone can't pay the {C}, so the power-up isn't affordable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thanos, the Mad Titan", enteredThisTurn = true)
                    // Blue and green for the {U}{G}, plus plenty of other colored mana. No
                    // colorless source anywhere.
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.powerUp()
                withClue("the reduced cost is {C}{U}{G}") {
                    action shouldNotBe null
                    action!!.description.startsWith("Power-up — {C}{U}{G}:") shouldBe true
                }
                withClue("15 colored lands still cannot produce the one {C} this needs") {
                    action!!.isAffordable shouldBe false
                }

                val thanos = game.findPermanent("Thanos, the Mad Titan")!!
                val result = game.execute(ActivateAbility(game.player1Id, thanos, abilityId))
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                withClue("the handler must refuse to pay a colorless pip with colored mana") {
                    val paid = game.state.getEntity(thanos)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                        ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    (result.error != null || paid == 0) shouldBe true
                }
            }

            test("adding one colorless source makes it payable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thanos, the Mad Titan", enteredThisTurn = true)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    // Gathering Place: "{T}: Add {C}."
                    .withLandsOnBattlefield(1, "Gathering Place", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("with a real colorless source the same {C}{U}{G} is payable") {
                    game.powerUp()!!.isAffordable shouldBe true
                }
            }
        }

        context("Thanos, the Mad Titan — the odd/even sweep") {

            /** A board of one creature at each parity, plus the printed "Zero is even" case. */
            fun sweepBoard() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Thanos, the Mad Titan", enteredThisTurn = true)
                .withCardOnBattlefield(2, "Odd Bystander")
                .withCardOnBattlefield(2, "Even Bystander")
                .withCardOnBattlefield(2, "Nil Bystander")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                // Gathering Place: "{T}: Add {C}."
                .withLandsOnBattlefield(1, "Gathering Place", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            /**
             * Resolve the ability and answer the mode prompt. Asserting the prompt exists is half
             * the point: a modal nested inside a `Composite` only asks at *resolution* (CR 603.3c),
             * so a regression that pre-chose at activation time would leave no decision here.
             */
            fun TestGame.sweep(modeIndex: Int) {
                val thanos = findPermanent("Thanos, the Mad Titan")!!
                activatePowerUp(thanos)
                resolveStack()

                val decision = getPendingDecision()
                withClue("the mode must be chosen on resolution, not at activation") {
                    decision shouldNotBe null
                    (decision is ChooseOptionDecision) shouldBe true
                }
                submitDecision(OptionChosenResponse((decision as ChooseOptionDecision).id, modeIndex))
                resolveStack()
            }

            test("choosing odd destroys the odd creature and spares Thanos himself") {
                val game = sweepBoard()
                game.sweep(modeIndex = 0)

                withClue("mana value 3 is odd") { game.isInGraveyard(2, "Odd Bystander") shouldBe true }
                withClue("mana value 2 is even") { game.isInGraveyard(2, "Even Bystander") shouldBe false }
                withClue("mana value 0 is even") { game.isInGraveyard(2, "Nil Bystander") shouldBe false }
                withClue("Thanos is mana value 3 — 'each OTHER creature' is what keeps him alive") {
                    game.findPermanent("Thanos, the Mad Titan") shouldNotBe null
                }
                withClue("the counters go on before the sweep and stay on") {
                    game.state.getEntity(game.findPermanent("Thanos, the Mad Titan")!!)
                        ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("choosing even destroys the even creatures, and zero counts as even") {
                val game = sweepBoard()
                game.sweep(modeIndex = 1)

                withClue("mana value 2 is even") { game.isInGraveyard(2, "Even Bystander") shouldBe true }
                withClue("mana value 0 is even (printed 'Zero is even')") {
                    game.isInGraveyard(2, "Nil Bystander") shouldBe true
                }
                withClue("mana value 3 is odd") { game.isInGraveyard(2, "Odd Bystander") shouldBe false }
                withClue("Thanos is odd, so he survives this mode on mana value alone") {
                    game.findPermanent("Thanos, the Mad Titan") shouldNotBe null
                }
            }
        }
    }
}
