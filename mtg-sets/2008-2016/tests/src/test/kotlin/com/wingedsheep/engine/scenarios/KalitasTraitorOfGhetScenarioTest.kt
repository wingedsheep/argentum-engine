package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kalitas, Traitor of Ghet (OGW #86): "If a nontoken creature an opponent controls would die,
 * instead exile that card and create a 2/2 black Zombie creature token." The token is the rest of
 * the replacement — a [com.wingedsheep.sdk.scripting.RedirectZoneChangeWith] WITH_REPLACEMENT
 * rider that isn't on the inline whitelist, so the engine queues it as a pending replacement rider.
 */
class KalitasTraitorOfGhetScenarioTest : ScenarioTestBase() {

    init {
        context("Kalitas, Traitor of Ghet") {

            test("an opponent's creature dying to damage is exiled and you create a 2/2 black Zombie") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kalitas, Traitor of Ghet")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("The Bears are exiled instead of going to the graveyard") {
                    game.state.getGraveyard(game.player2Id) shouldNotContain bears
                    game.state.getExile(game.player2Id) shouldContain bears
                }
                val zombies = game.findAllPermanents("Zombie Token")
                withClue("Kalitas's controller creates one 2/2 black Zombie") {
                    zombies shouldHaveSize 1
                    val zombie = zombies.single()
                    game.state.projectedState.getController(zombie) shouldBe game.player1Id
                    game.state.projectedState.getPower(zombie) shouldBe 2
                    game.state.projectedState.getToughness(zombie) shouldBe 2
                    game.state.projectedState.getColors(zombie) shouldBe setOf(Color.BLACK.name)
                }
            }

            test("Kalitas dying alongside opponents' creatures still exiles them and makes that many Zombies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kalitas, Traitor of Ghet")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Walking Corpse")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Day of Judgment")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kalitas = game.findPermanent("Kalitas, Traitor of Ghet")!!
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val corpse = game.findPermanent("Walking Corpse")!!

                game.castSpell(1, "Day of Judgment").error shouldBe null
                game.resolveStack()

                withClue("Your own creatures, Kalitas included, die normally") {
                    game.state.getGraveyard(game.player1Id) shouldContain kalitas
                    game.state.getGraveyard(game.player1Id) shouldContain giant
                }
                withClue("Both of the opponent's creatures are exiled") {
                    game.state.getExile(game.player2Id) shouldContain bears
                    game.state.getExile(game.player2Id) shouldContain corpse
                }
                withClue("One Zombie per exiled card, created after Day of Judgment, so they survive it (ruling)") {
                    val zombies = game.findAllPermanents("Zombie Token")
                    zombies shouldHaveSize 2
                    zombies.forEach { game.state.projectedState.getController(it) shouldBe game.player1Id }
                }
            }

            test("your own creature dies normally, with no Zombie") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kalitas, Traitor of Ghet")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mine = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = mine).error shouldBe null
                game.resolveStack()

                game.state.getGraveyard(game.player1Id) shouldContain mine
                game.findAllPermanents("Zombie Token") shouldHaveSize 0
            }

            test("{2}{B}, sacrifice another Vampire or Zombie: two +1/+1 counters on Kalitas") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kalitas, Traitor of Ghet")
                    .withCardOnBattlefield(1, "Vampire Nighthawk")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kalitas = game.findPermanent("Kalitas, Traitor of Ghet")!!
                val nighthawk = game.findPermanent("Vampire Nighthawk")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.requireCard("Kalitas, Traitor of Ghet").activatedAbilities.single().id

                fun activate(sacrificed: List<com.wingedsheep.sdk.model.EntityId>) = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kalitas,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = sacrificed),
                    )
                )

                withClue("Kalitas can't sacrifice itself (ruling)") {
                    activate(listOf(kalitas)).error shouldNotBe null
                }
                withClue("A creature that is neither a Vampire nor a Zombie can't be sacrificed") {
                    activate(listOf(bears)).error shouldNotBe null
                }

                activate(listOf(nighthawk)).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(1, "Vampire Nighthawk") shouldBe true
                game.state.getEntity(kalitas)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }
        }
    }
}
