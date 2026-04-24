package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Eclipsed Realms.
 *
 * - Land
 *   As Eclipsed Realms enters, choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin, Merfolk, or Treefolk.
 *   {T}: Add {C}.
 *   {T}: Add one mana of any color. Spend this mana only to cast a spell of the chosen type
 *   or activate an ability of a source of the chosen type.
 */
class EclipsedRealmsScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.playLandByName(playerNumber: Int, landName: String) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val hand = state.getHand(playerId)
        val cardId = hand.find { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == landName
        } ?: error("Card '$landName' not found in player $playerNumber's hand")
        execute(PlayLand(playerId, cardId))
    }

    init {
        context("Eclipsed Realms - entry choice") {

            test("playing Eclipsed Realms prompts for creature type choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eclipsed Realms")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLandByName(1, "Eclipsed Realms")
                game.hasPendingDecision() shouldBe true
                game.chooseCreatureType("Elf")

                val landId = game.findPermanent("Eclipsed Realms")!!
                val chosen = game.state.getEntity(landId)?.get<ChosenCreatureTypeComponent>()
                chosen.shouldNotBeNull()
                chosen.creatureType shouldBe "Elf"
            }
        }

        context("Eclipsed Realms - mana abilities") {

            test("{T}: Add {C} produces one colorless mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eclipsed Realms")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLandByName(1, "Eclipsed Realms")
                game.chooseCreatureType("Elf")

                val landId = game.findPermanent("Eclipsed Realms")!!
                val cardDef = cardRegistry.getCard("Eclipsed Realms")!!
                val colorlessAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = landId,
                        abilityId = colorlessAbility.id
                    )
                )
                result.error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                pool.shouldNotBeNull()
                pool.colorless shouldBe 1
                pool.restrictedMana shouldBe emptyList()
            }

            test("{T}: Add one mana of any color produces restricted mana tagged with chosen subtype") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eclipsed Realms")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLandByName(1, "Eclipsed Realms")
                game.chooseCreatureType("Elf")

                val landId = game.findPermanent("Eclipsed Realms")!!
                val cardDef = cardRegistry.getCard("Eclipsed Realms")!!
                val anyColorAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = landId,
                        abilityId = anyColorAbility.id,
                        manaColorChoice = Color.GREEN
                    )
                )
                result.error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                pool.shouldNotBeNull()
                withClue("Unrestricted green pool stays empty; mana is restricted") {
                    pool.green shouldBe 0
                }
                pool.restrictedMana.size shouldBe 1
                val entry = pool.restrictedMana[0]
                entry.color shouldBe Color.GREEN
                val restriction = entry.restriction
                restriction.shouldBeInstanceOf<ManaRestriction.SubtypeSpellsOrAbilitiesOnly>()
                restriction.subtype shouldBe "Elf"
            }
        }

        context("Eclipsed Realms - conditional mana spending") {

            test("restricted mana CAN cast a spell matching the chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Elvish Warrior") // Elf Warrior, cost {G}{G}
                    .withCardInHand(1, "Eclipsed Realms")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLandByName(1, "Eclipsed Realms")
                game.chooseCreatureType("Elf")

                val landId = game.findPermanent("Eclipsed Realms")!!
                val cardDef = cardRegistry.getCard("Eclipsed Realms")!!
                val anyColorAbility = cardDef.script.activatedAbilities[1]

                // Seed the pool with two restricted green mana tagged Elf (equivalent to
                // tapping two Eclipsed Realms). This isolates the spending-restriction test
                // from the untap cycle.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    container.with(
                        pool.addRestricted(Color.GREEN, 2, ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf"))
                    )
                }

                val warriorId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Elvish Warrior"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = warriorId,
                        paymentStrategy = PaymentStrategy.FromPool
                    )
                )
                withClue("Elvish Warrior is an Elf — restricted mana should pay. Error: ${cast.error}") {
                    cast.error shouldBe null
                }
            }

            test("restricted mana CANNOT cast a spell with a different subtype") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears") // Bear (not Elf), cost {1}{G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed pool with {G}{G} restricted to Elf — Grizzly Bears (Bear) shouldn't be castable.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    container.with(
                        pool.addRestricted(Color.GREEN, 2, ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf"))
                    )
                }

                val bearsId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = bearsId,
                        paymentStrategy = PaymentStrategy.FromPool
                    )
                )
                withClue("Grizzly Bears is a Bear, not an Elf — restricted mana must not pay") {
                    cast.error shouldNotBe null
                }
            }
        }
    }
}

