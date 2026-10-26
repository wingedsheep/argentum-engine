package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.ToralfGodOfFury
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Toralf, God of Fury // Toralf's Hammer.
 *
 * Front: excess noncombat damage to an opponent's creature or planeswalker makes Toralf deal the
 * excess to any target *other than that permanent* (`notTriggeringEntity()`). Back: the Hammer grants
 * "{1}{R}, {T}, Unattach Toralf's Hammer: It deals 3 damage to any target. Return Toralf's Hammer to
 * its owner's hand." — the unattach is paid by the *granting* Equipment, not by the creature that
 * has the ability, and the Hammer is the damage source (its rulings).
 */
class ToralfGodOfFuryScenarioTest : ScenarioTestBase() {

    private val legend = card("Toralf Test Legend") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Human"
        power = 2
        toughness = 2
    }

    private val indestructibleBear = card("Toralf Test Indestructible Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(com.wingedsheep.sdk.core.Keyword.INDESTRUCTIBLE)
    }

    private val hammerAbilityId by lazy {
        ToralfGodOfFury.backFace!!.script.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id
    }

    init {
        cardRegistry.register(ToralfGodOfFury)
        cardRegistry.register(listOf(legend, indestructibleBear))

        fun toralfBoard(): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Toralf, God of Fury")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Toralf Test Indestructible Bear")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.opponentsPermanent(name: String) =
            state.getBattlefield().single { id ->
                state.projectedState.getController(id) == player2Id &&
                    state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name
            }

        context("Toralf, God of Fury") {
            test("excess noncombat damage to an opponent's creature is dealt again, never to that creature") {
                val game = toralfBoard()
                // Indestructible, so the damaged creature is still on the battlefield when the trigger
                // picks its target — only the exclusion keeps it out.
                val theirBears = game.opponentsPermanent("Toralf Test Indestructible Bear")
                val hillGiant = game.opponentsPermanent("Hill Giant")

                game.castSpell(1, "Lightning Bolt", theirBears).error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = decision.legalTargets.getValue(0)
                withClue("the damaged creature survived, so it would be a candidate without the exclusion") {
                    game.state.getBattlefield() shouldContain theirBears
                }
                withClue("any target other than that permanent: the damaged Bear is excluded") {
                    legal shouldNotContain theirBears
                    legal shouldContain hillGiant
                    legal shouldContain game.player2Id
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("3 damage to a 2/2 is 1 excess") { game.getLifeTotal(2) shouldBe 19 }
            }

            test("exactly lethal damage is not excess") {
                val game = toralfBoard()
                game.castSpell(1, "Lightning Bolt", game.opponentsPermanent("Hill Giant")).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("excess damage to your own creature doesn't trigger it") {
                val game = toralfBoard()
                val ourBears = game.state.getBattlefield().single { id ->
                    game.state.projectedState.getController(id) == game.player1Id &&
                        game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears"
                }
                game.castSpell(1, "Lightning Bolt", ourBears).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 20
            }
        }

        fun hammerBoard(host: String): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, host)
            .withCardAttachedTo(1, "Toralf's Hammer", host)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Toralf's Hammer") {
            test("equipped creature gets +3/+0 only as long as it's legendary") {
                val legendGame = hammerBoard("Toralf Test Legend")
                legendGame.state.projectedState.getPower(legendGame.findPermanent("Toralf Test Legend")!!) shouldBe 5

                val bearsGame = hammerBoard("Centaur Courser")
                bearsGame.state.projectedState.getPower(bearsGame.findPermanent("Centaur Courser")!!) shouldBe 3
            }

            test("the granted ability unattaches the Hammer as a cost, the Hammer deals 3, then returns to hand") {
                val game = hammerBoard("Centaur Courser")
                val courser = game.findPermanent("Centaur Courser")!!
                val hammer = game.findPermanent("Toralf's Hammer")!!

                game.execute(
                    ActivateAbility(game.player1Id, courser, hammerAbilityId, targets = listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldBe null

                withClue("the unattach is a cost: paid before the ability resolves, detaching the Hammer, not the creature") {
                    game.state.getEntity(hammer)?.get<AttachedToComponent>() shouldBe null
                    game.findPermanent("Toralf's Hammer") shouldBe hammer
                }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("back in hand — a modal DFC in hand shows its front face") {
                    game.isInHand(1, "Toralf, God of Fury") shouldBe true
                }
                game.findPermanent("Centaur Courser") shouldNotBe null
            }

            test("if the target is gone on resolution, nothing happens and the Hammer stays unattached on the battlefield") {
                val game = hammerBoard("Centaur Courser")
                val courser = game.findPermanent("Centaur Courser")!!
                val theirBears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(game.player1Id, courser, hammerAbilityId, targets = listOf(ChosenTarget.Permanent(theirBears)))
                ).error shouldBe null
                // Kill the target in response with the third Mountain.
                game.castSpell(1, "Lightning Bolt", theirBears).error shouldBe null
                game.resolveStack()

                withClue("the ability fizzled: the Hammer is neither returned nor reattached") {
                    game.isInHand(1, "Toralf, God of Fury") shouldBe false
                    val hammer = game.findPermanent("Toralf's Hammer")
                    hammer shouldNotBe null
                    game.state.getEntity(hammer!!)?.get<AttachedToComponent>() shouldBe null
                }
            }
        }
    }
}
