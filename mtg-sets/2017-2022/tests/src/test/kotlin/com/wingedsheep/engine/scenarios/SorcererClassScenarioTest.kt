package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.SorcererClass
import com.wingedsheep.mtg.sets.definitions.afr.cards.WizardClass
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sorcerer Class (AFR #233).
 *
 * Level 2's granted mana is `AnyOf(InstantOrSorceryOnly, ClassLevelUpOnly)`: it pays an instant,
 * this Class's own level 3 and another Class's level-up, and refuses a creature spell. Level 3's
 * trigger makes the cast spell deal damage to each opponent equal to the instants and sorceries
 * cast this turn, counted as the trigger resolves (ruling).
 */
class SorcererClassScenarioTest : ScenarioTestBase() {

    private val grantedManaAbilityId =
        (SorcererClass.classLevels.single { it.level == 2 }.staticAbilities.single() as GrantActivatedAbility).ability.id

    private fun TestGame.tapForMana(creature: EntityId, color: Color) =
        execute(ActivateAbility(player1Id, creature, grantedManaAbilityId, manaColorChoice = color))

    private fun TestGame.restrictedManaCount() =
        state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana.size

    init {
        cardRegistry.register(SorcererClass)
        cardRegistry.register(WizardClass)

        test("level 1: entering draws two cards, then discards two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sorcerer Class")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sorcerer Class").error shouldBe null
            game.resolveStack()
            // Bolt + the two drawn Bears: three cards, so the discard of two is a real choice.
            val hand = game.state.getHand(game.player1Id)
            hand.size shouldBe 3
            val bears = hand.filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            bears.size shouldBe 2
            game.selectCards(bears).error shouldBe null
            game.state.getHand(game.player1Id).map { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldBe
                listOf("Lightning Bolt")
            game.state.getGraveyard(game.player1Id).size shouldBe 2
        }

        test("level 1 grants no mana ability; level 2 gives your creatures {T}: Add {U} or {R}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.tapForMana(game.findPermanent("Grizzly Bears")!!, Color.BLUE).error shouldNotBe null

            val leveled = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (mine, theirs) = leveled.findPermanents("Grizzly Bears")
                .partition { leveled.state.projectedState.getController(it) == leveled.player1Id }
            leveled.execute(
                ActivateAbility(leveled.player1Id, theirs.single(), grantedManaAbilityId, manaColorChoice = Color.RED)
            ).error shouldNotBe null
            leveled.tapForMana(mine.single(), Color.BLUE).error shouldBe null
            leveled.restrictedManaCount() shouldBe 1
        }

        test("level 2 mana pays an instant but not a creature spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Goblin Guide")
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.tapForMana(game.findPermanent("Grizzly Bears")!!, Color.RED).error shouldBe null
            // No lands: only the restricted {R} could pay either spell.
            game.castSpell(1, "Goblin Guide").error shouldNotBe null
            val bolt = game.state.getHand(game.player1Id).first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt" }
            game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.restrictedManaCount() shouldBe 0
        }

        test("level 2 mana pays this Class's level 3 and another Class's level-up") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 2)
                .withCardOnBattlefield(1, "Wizard Class", classLevel = 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanents("Grizzly Bears")
            val wizard = game.findPermanent("Wizard Class")!!
            val sorcerer = game.findPermanent("Sorcerer Class")!!

            // Wizard Class level 2 costs {2}{U}; no lands, so the floating mana must pay it.
            bears.take(3).forEach { game.tapForMana(it, Color.BLUE).error shouldBe null }
            val wizardLevel = game.execute(ActivateAbility(game.player1Id, wizard, AbilityId.classLevelUp(2)))
            withClue("Wizard Class level-up from Sorcerer Class mana: ${wizardLevel.error}") {
                wizardLevel.error shouldBe null
            }
            game.restrictedManaCount() shouldBe 0
            game.resolveStack()

            // Sorcerer Class level 3 costs {3}{U}{R}: two floating mana is not enough, and that
            // shortfall is the only reason it fails.
            bears.drop(3).zip(listOf(Color.BLUE, Color.RED)).forEach { (bear, color) ->
                game.tapForMana(bear, color).error shouldBe null
            }
            val affordable = game.getLegalActions(1)
                .firstOrNull { (it.action as? ActivateAbility)?.let { a -> a.sourceId == sorcerer } == true }
            affordable shouldNotBe null
            affordable!!.isAffordable shouldBe false
        }

        test("level 3: each instant deals damage to each opponent equal to instants and sorceries cast this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opponentStart = game.getLifeTotal(2)
            fun boltOpponent() {
                val bolt = game.state.getHand(game.player1Id).first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt" }
                game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
                game.resolveStack()
            }

            // First Bolt: trigger deals 1 (one instant cast), Bolt deals 3.
            boltOpponent()
            game.getLifeTotal(2) shouldBe opponentStart - 4
            // Second Bolt: trigger deals 2, Bolt deals 3.
            boltOpponent()
            game.getLifeTotal(2) shouldBe opponentStart - 9
        }

        test("level 3 counts spells cast in response to the trigger (ruling)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sorcerer Class", classLevel = 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opponentStart = game.getLifeTotal(2)
            val bolts = game.state.getHand(game.player1Id).filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt" }
            game.execute(CastSpell(game.player1Id, bolts[0], listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            // Respond to the first trigger with the second Bolt: both triggers then read 2.
            game.execute(CastSpell(game.player1Id, bolts[1], listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe opponentStart - (3 + 3 + 2 + 2)
        }
    }
}
