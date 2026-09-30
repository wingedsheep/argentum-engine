package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.LukkaBoundToRuin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lukka, Bound to Ruin (ONE #207, {2}{R}{R/G/P}{G}, Loyalty 5).
 *
 *   +1: Add {R}{G}. Spend this mana only to cast creature spells or activate abilities of creatures.
 *   −1: Create a 3/3 green Phyrexian Beast creature token with toxic 1.
 *   −4: Lukka deals X damage divided as you choose among any number of target creatures and/or
 *       planeswalkers, where X is the greatest power among creatures you control as you activate
 *       this ability.
 *
 * The −4 is the engine-interesting one: X is defined by the ability's text and fixed as it is
 * activated (CR 107.3c), which is also when the division is announced (CR 601.2d). The legal action
 * offers that X to divide and caps the targets at it, the validator checks a division against it,
 * and the resolving ability deals it even if the creature that set it is gone by then.
 */
class LukkaBoundToRuinScenarioTest : ScenarioTestBase() {

    private val plusOne = LukkaBoundToRuin.activatedAbilities[0].id
    private val minusOne = LukkaBoundToRuin.activatedAbilities[1].id
    private val minusFour = LukkaBoundToRuin.activatedAbilities[2].id

    init {
        context("Lukka, Bound to Ruin") {

            test("+1 adds {R}{G} restricted to creature spells and creature abilities") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)

                game.execute(ActivateAbility(game.player1Id, lukka, plusOne)).error shouldBe null
                game.resolveStack()

                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                withClue("one red and one green, both carrying the creature-only restriction") {
                    pool.restrictedMana.map { it.color }.toSet() shouldBe setOf(Color.RED, Color.GREEN)
                    pool.red shouldBe 0
                    pool.green shouldBe 0
                }
                loyalty(game, lukka) shouldBe 6
            }

            test("−1 creates a 3/3 green Phyrexian Beast with toxic 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)

                game.execute(ActivateAbility(game.player1Id, lukka, minusOne)).error shouldBe null
                game.resolveStack()

                val beast = game.findPermanent("Phyrexian Beast Token") ?: game.findPermanent("Phyrexian Beast")
                beast shouldNotBe null
                val projected = game.state.projectedState
                projected.getPower(beast!!) shouldBe 3
                projected.getToughness(beast) shouldBe 3
                withClue("the token carries numeric toxic 1") {
                    ("TOXIC_1" in projected.getKeywords(beast)) shouldBe true
                }
                loyalty(game, lukka) shouldBe 4
            }

            test("−4 offers the greatest power among your creatures to divide, and caps targets at it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withCardOnBattlefield(1, "Grizzly Bears")       // 2/2 — the greatest power
                    .withCardOnBattlefield(1, "Savannah Lions")      // 2/1
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)

                val action = game.getLegalActions(1).single { it.actionInfoAbilityId() == minusFour }
                withClue("X = 2, so the client divides 2 and may choose at most 2 targets") {
                    action.requiresDamageDistribution shouldBe true
                    action.totalDamageToDistribute shouldBe 2
                    action.minDamagePerTarget shouldBe 1
                    action.targetCount shouldBe 2
                }
            }

            test("−4 accepts a division of exactly X and rejects one of any other total") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withCardOnBattlefield(1, "Force of Nature")     // 5/5 — X = 5
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser))

                withClue("a division of 8 isn't X") {
                    game.execute(
                        ActivateAbility(game.player1Id, lukka, minusFour, targets = targets,
                            damageDistribution = mapOf(bears to 3, courser to 5))
                    ).error shouldNotBe null
                }
                loyalty(game, lukka) shouldBe 5

                game.execute(
                    ActivateAbility(game.player1Id, lukka, minusFour, targets = targets,
                        damageDistribution = mapOf(bears to 2, courser to 3))
                ).error shouldBe null
                game.resolveStack()

                withClue("2 to the 2/2 and 3 to the 3/3 — both die, no prompt at resolution") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                loyalty(game, lukka) shouldBe 1
            }

            test("−4's X is locked on activation — losing the creature that set it changes nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withCardOnBattlefield(1, "Force of Nature")     // 5/5 — X = 5
                    .withCardOnBattlefield(1, "Grizzly Bears")       // 2/2 — the power left behind
                    .withCardOnBattlefield(2, "Force of Nature")     // the target, 5/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)
                val forces = game.findPermanents("Force of Nature")
                val mine = forces.single { game.state.projectedState.getController(it) == game.player1Id }
                val theirs = forces.single { it != mine }

                // A lone target needs no announced division: the whole X lands on it at resolution.
                game.execute(
                    ActivateAbility(game.player1Id, lukka, minusFour, targets = listOf(ChosenTarget.Permanent(theirs)))
                ).error shouldBe null

                // Our 5/5 leaves in response; the greatest power among our creatures is now 2.
                game.state = game.state.moveToZone(
                    mine,
                    ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player1Id, Zone.GRAVEYARD),
                )
                game.resolveStack()

                withClue("5 damage was dealt, not the 2 a resolution-time re-count would give") {
                    game.findPermanents("Force of Nature").none { it == theirs } shouldBe true
                }
            }

            test("−4 with no creatures has X = 0 and may be activated with no targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lukka, Bound to Ruin")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val lukka = game.findPermanent("Lukka, Bound to Ruin")!!
                seedLoyalty(game, lukka, 5)
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("no target can be given the 1 damage it needs") {
                    game.execute(
                        ActivateAbility(game.player1Id, lukka, minusFour, targets = listOf(ChosenTarget.Permanent(bears)))
                    ).error shouldNotBe null
                }
                game.execute(ActivateAbility(game.player1Id, lukka, minusFour)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldBe bears
                (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                loyalty(game, lukka) shouldBe 1
            }
        }
    }

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder drops permanents straight onto the battlefield without running the
        // "enters with its starting loyalty" step, so seed it explicitly.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun com.wingedsheep.engine.view.LegalActionInfo.actionInfoAbilityId(): AbilityId? =
        (action as? ActivateAbility)?.abilityId
}
