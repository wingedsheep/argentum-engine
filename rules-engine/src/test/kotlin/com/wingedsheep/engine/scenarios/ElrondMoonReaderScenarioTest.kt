package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.AlongTheCrookedWay
import com.wingedsheep.mtg.sets.definitions.hob.cards.ElrondMoonReader
import com.wingedsheep.mtg.sets.definitions.inv.cards.Firescreamer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elrond, Moon-Reader (HOB #36) — {2}{U} Legendary Creature — Elf Noble 3/3.
 *
 * "Whenever you activate an ability of a creature, draw a card. This ability triggers only once
 * each turn." + "{5}{U}{U}: Exile up to two other target nonland permanents you control. Return
 * those cards to the battlefield under their owner's control at the beginning of the next end step."
 *
 * The trigger is the new source-filtered form of the "you activate an ability" event
 * ([com.wingedsheep.sdk.dsl.Triggers.activatesAbilityOf]), so the interesting cases are the two
 * ways it must *not* fire: a creature's mana ability (excluded by the shared "isn't a mana ability"
 * gate) and a noncreature permanent's ability (excluded by the source filter).
 */
class ElrondMoonReaderScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(ElrondMoonReader)
        cardRegistry.register(Firescreamer)
        cardRegistry.register(AlongTheCrookedWay)

        context("Elrond, Moon-Reader") {

            test("activating a creature's non-mana ability draws, but only once each turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elrond, Moon-Reader", summoningSickness = false)
                    .withCardOnBattlefield(1, "Firescreamer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val screamer = game.findPermanent("Firescreamer")!!
                val pump = Firescreamer.activatedAbilities.single().id
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = screamer, abilityId = pump)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the first creature-ability activation this turn draws a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = screamer, abilityId = pump)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("'only once each turn' suppresses the second activation's trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("a creature's mana ability does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elrond, Moon-Reader", summoningSickness = false)
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val tapForMana = TestCards.LlanowarElves.activatedAbilities.single().id
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = elves, abilityId = tapForMana)
                ).error shouldBe null
                game.resolveStack()

                withClue("mana abilities don't use the stack and never fire this trigger") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("a noncreature permanent's ability does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elrond, Moon-Reader", summoningSickness = false)
                    .withCardOnBattlefield(1, "Along the Crooked Way")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantment = game.findPermanent("Along the Crooked Way")!!
                val grantMenace = AlongTheCrookedWay.activatedAbilities.single().id
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = enchantment,
                        abilityId = grantMenace
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the source filter is 'a creature'; an Enchantment's ability is out of scope") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("the blink ability exiles two permanents and returns them at the next end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elrond, Moon-Reader", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val blink = ElrondMoonReader.activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Elrond, Moon-Reader")!!,
                        abilityId = blink,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(lions))
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("both chosen permanents are exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Savannah Lions") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("each exiled card gets its own delayed return at the next end step") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }
        }
    }
}
