package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.gameserver.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CavernOfSoulsScenarioTest : ScenarioTestBase() {

    init {

        context("Cavern of Souls — colorless mana ability") {
            test("tap for colorless adds C to pool") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                val colorlessAbility = cardRegistry.getCard("Cavern of Souls")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(game.player1Id, cavernId, colorlessAbility.id))

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Pool should contain 1 colorless") { pool.colorless shouldBe 1 }
            }
        }

        context("Cavern of Souls — restricted colored mana ability (floating pool path)") {
            test("produces restricted entry carrying MakesSpellUncounterable rider") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                val restrictedAbility = cardRegistry.getCard("Cavern of Souls")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(game.player1Id, cavernId, restrictedAbility.id, manaColorChoice = Color.RED))

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Pool should have 1 restricted entry") { pool.restrictedMana.size shouldBe 1 }
                val entry = pool.restrictedMana.first()
                withClue("Entry should carry MakesSpellUncounterable rider") {
                    entry.riders shouldBe setOf(ManaSpellRider.MakesSpellUncounterable)
                }
                withClue("Entry restriction should be SubtypeSpellsOrAbilitiesOnly(Goblin, creatureOnly = true)") {
                    val r = entry.restriction as ManaRestriction.SubtypeSpellsOrAbilitiesOnly
                    r.subtype shouldBe "Goblin"
                    r.creatureOnly shouldBe true
                }
            }

            test("Goblin cast with Cavern mana has CantBeCounteredComponent on stack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withCardInHand(1, "Goblin Sledder")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInHand(2, "Complicate")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                val restrictedAbility = cardRegistry.getCard("Cavern of Souls")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(game.player1Id, cavernId, restrictedAbility.id, manaColorChoice = Color.RED))
                game.castSpell(1, "Goblin Sledder")

                val sledderOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Sledder"
                }
                withClue("Goblin Sledder should be on the stack") { sledderOnStack shouldNotBe null }
                withClue("Goblin Sledder should have CantBeCounteredComponent") {
                    game.state.getEntity(sledderOnStack!!)?.has<CantBeCounteredComponent>() shouldBe true
                }
            }

            test("Complicate cannot counter a Goblin cast with Cavern mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withCardInHand(1, "Goblin Sledder")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInHand(2, "Complicate")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                val restrictedAbility = cardRegistry.getCard("Cavern of Souls")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(game.player1Id, cavernId, restrictedAbility.id, manaColorChoice = Color.RED))
                game.castSpell(1, "Goblin Sledder")
                game.passPriority()

                game.castSpellTargetingStackSpell(2, "Complicate", "Goblin Sledder")
                game.resolveStack() // Complicate resolves but can't counter

                withClue("Goblin Sledder should NOT be in graveyard (uncounterable)") {
                    game.isInGraveyard(1, "Goblin Sledder") shouldBe false
                }

                game.resolveStack() // Goblin Sledder resolves

                withClue("Goblin Sledder should be on the battlefield") {
                    game.isOnBattlefield("Goblin Sledder") shouldBe true
                }
            }
        }

        context("Cavern of Souls — AutoPay path: solver taps Cavern directly") {
            test("Goblin cast when Cavern is the only mana source gets CantBeCounteredComponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls") // only mana source
                    .withCardInHand(1, "Goblin Sledder")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                // No pre-floating: AutoPay taps Cavern directly via the solver path
                game.castSpell(1, "Goblin Sledder")

                val sledderOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Sledder"
                }
                withClue("AutoPay solver path should also stamp CantBeCounteredComponent") {
                    game.state.getEntity(sledderOnStack!!)?.has<CantBeCounteredComponent>() shouldBe true
                }
            }
        }

        context("Cavern of Souls — spell cast WITHOUT Cavern mana can be countered") {
            test("Goblin cast using regular Mountain mana IS counterable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Goblin Sledder")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInHand(2, "Complicate")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                // Cast using AutoPay — Mountain will be preferred over Cavern's restricted mana
                game.castSpell(1, "Goblin Sledder")

                val sledderOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Goblin Sledder"
                }
                withClue("Goblin Sledder should NOT have CantBeCounteredComponent") {
                    game.state.getEntity(sledderOnStack!!)?.has<CantBeCounteredComponent>() shouldBe false
                }

                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Complicate", "Goblin Sledder")
                game.resolveStack()

                withClue("Goblin Sledder should be countered") {
                    game.isInGraveyard(1, "Goblin Sledder") shouldBe true
                }
            }
        }

        context("Cavern of Souls — restricted mana only satisfies chosen creature type") {
            test("restricted mana cannot pay for Elf when Goblin is chosen") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cavern of Souls")
                    .withCardInHand(1, "Elvish Warrior") // {G}{G} Elf, not Goblin
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cavernId = game.findPermanent("Cavern of Souls")!!
                game.state = game.state.updateEntity(cavernId) { c -> c.with(ChosenCreatureTypeComponent("Goblin")) }

                val restrictedAbility = cardRegistry.getCard("Cavern of Souls")!!.script.activatedAbilities[1]
                // Tap Cavern for green mana restricted to Goblin
                game.execute(ActivateAbility(game.player1Id, cavernId, restrictedAbility.id, manaColorChoice = Color.GREEN))

                // Only restricted green in pool; Elvish Warrior ({G}{G}) needs 2 green but Elf ≠ Goblin
                val elvishId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Elvish Warrior"
                }!!
                val result = game.execute(CastSpell(playerId = game.player1Id, cardId = elvishId))

                withClue("Casting Elf with Goblin-restricted mana should fail") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
