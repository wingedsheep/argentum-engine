package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ayara, Widow of the Realm // Ayara, Furnace Queen (MOM #90).
 *
 *   Front — "{T}, Sacrifice another creature or artifact: Ayara deals X damage to target opponent or
 *            battle and you gain X life, where X is the sacrificed permanent's mana value."
 *            "{5}{R/P}: Transform Ayara. Activate only as a sorcery."
 *   Back  — "At the beginning of combat on your turn, return up to one target artifact or creature
 *            card from your graveyard to the battlefield. It gains haste. Exile it at the beginning
 *            of the next end step."
 */
class AyaraWidowOfTheRealmScenarioTest : ScenarioTestBase() {

    private val sacAbility get() = cardRegistry.getCard("Ayara, Widow of the Realm")!!.activatedAbilities[0].id
    private val transformAbility get() = cardRegistry.getCard("Ayara, Widow of the Realm")!!.activatedAbilities[1].id

    init {
        context("Ayara, Widow of the Realm") {

            test("sacrificing a mana-value-2 artifact deals 2 to an opponent and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ayara, Widow of the Realm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ayara = game.findPermanent("Ayara, Widow of the Realm")!!
                val millstone = game.findPermanent("Millstone")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = ayara, abilityId = sacAbility,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(millstone))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Millstone") shouldBe false
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }

            test("can't target its own controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ayara, Widow of the Realm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Ayara, Widow of the Realm")!!,
                        abilityId = sacAbility,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Millstone")!!))
                    )
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Millstone") shouldBe true
            }

            test("can hit a battle, removing defense counters equal to the creature's mana value") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ayara, Widow of the Realm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()

                val siege = game.findPermanent("Invasion of Innistrad")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Ayara, Widow of the Realm")!!,
                        abilityId = sacAbility,
                        targets = listOf(ChosenTarget.Permanent(siege)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears (MV 2) removes 2 of the Siege's 5 defense counters") {
                    game.state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) shouldBe 3
                }
                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("Ayara, Furnace Queen") {

            test("transforms, then reanimates a creature with haste at combat and exiles it at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ayara, Widow of the Realm", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ayara = game.findPermanent("Ayara, Widow of the Realm")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = ayara, abilityId = transformAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.getEntity(ayara)!!.get<CardComponent>()!!.name shouldBe "Ayara, Furnace Queen"

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.resolveStack()
                val td = game.state.pendingDecision as ChooseTargetsDecision
                val bears = game.state.getGraveyard(game.player1Id).single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bears)))).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears is back with haste") {
                    returned shouldNotBe null
                    game.state.projectedState.hasKeyword(returned!!, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("exiled at the beginning of the next end step") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
