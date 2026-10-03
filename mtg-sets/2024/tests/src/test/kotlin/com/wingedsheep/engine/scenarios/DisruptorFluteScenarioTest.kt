package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fra.cards.SeasonedCryomancer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Disruptor Flute ({2}, Artifact — MH3 #209):
 *   Flash
 *   As this artifact enters, choose a card name.
 *   Spells with the chosen name cost {3} more to cast.
 *   Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 */
class DisruptorFluteScenarioTest : ScenarioTestBase() {

    private fun TestGame.castFluteNaming(name: String) {
        castSpell(1, "Disruptor Flute")
        resolveStack()
        val decision = getPendingDecision()
        withClue("Disruptor Flute must present an as-enters card-name choice") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        decision.options shouldContain name
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun TestGame.castCostOf(cardId: EntityId): String? =
        getLegalActions(1).firstOrNull { (it.action as? CastSpell)?.cardId == cardId }?.manaCostString

    private fun TestGame.nonManaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { !it.isManaAbility }

    private fun TestGame.manaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { it.isManaAbility }

    init {
        test("spells with the chosen name cost {3} more, including its controller's") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(1, "Grizzly Bears").first()
            game.castCostOf(bears) shouldBe "{1}{G}"

            game.castFluteNaming("Grizzly Bears")
            game.state.getEntity(game.findPermanent("Disruptor Flute")!!)?.chosenCardName() shouldBe "Grizzly Bears"

            withClue("Grizzly Bears now costs {3} more") {
                game.castCostOf(bears) shouldBe "{4}{G}"
            }
        }

        test("naming a different card leaves other spells untaxed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(1, "Grizzly Bears").first()
            game.castFluteNaming("Keldon Necropolis")
            game.castCostOf(bears) shouldBe "{1}{G}"
        }

        test("named source's non-mana abilities are locked but its mana abilities still work") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Mountain", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val necropolis = game.findPermanent("Keldon Necropolis")!!
            game.nonManaAbilityCount(necropolis) shouldBe 1

            game.castFluteNaming("Keldon Necropolis")

            withClue("Non-mana ability locked") { game.nonManaAbilityCount(necropolis) shouldBe 0 }
            withClue("Mana ability unaffected") { game.manaAbilityCount(necropolis) shouldNotBe 0 }
        }

        // "Sources" is any object, so the lock reaches cards outside the battlefield too.
        test("a named card in hand can't be cycled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInHand(1, "Disciple of Law")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.getLegalActions(1).any { it.action is CycleCard } shouldBe true
            game.castFluteNaming("Disciple of Law")

            game.getLegalActions(1).any { it.action is CycleCard } shouldBe false
            game.cycleCard(1, "Disciple of Law").error shouldNotBe null
        }

        test("a named card's graveyard ability can't be activated") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInGraveyard(1, "Seasoned Cryomancer")
                .withLandsOnBattlefield(1, "Island", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cryomancer = game.findCardsInGraveyard(1, "Seasoned Cryomancer").single()
            game.nonManaAbilityCount(cryomancer) shouldBe 1
            game.castFluteNaming("Seasoned Cryomancer")

            game.nonManaAbilityCount(cryomancer) shouldBe 0
            val abilityId = SeasonedCryomancer.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, cryomancer, abilityId)).error shouldNotBe null
        }

        test("crew and saddle are activated abilities, so a named Vehicle or Mount is locked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardOnBattlefield(1, "Ballista Charger")
                .withCardOnBattlefield(1, "Caustic Bronco")
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val charger = game.findPermanent("Ballista Charger")!!
            val bronco = game.findPermanent("Caustic Bronco")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.castFluteNaming("Ballista Charger")

            withClue("the named Vehicle can't be crewed") {
                game.getLegalActions(1).any { (it.action as? CrewVehicle)?.vehicleId == charger } shouldBe false
                game.execute(CrewVehicle(game.player1Id, charger, listOf(giant))).error shouldNotBe null
            }
            withClue("an unnamed Mount still saddles") {
                game.getLegalActions(1).any { (it.action as? SaddleMount)?.mountId == bronco } shouldBe true
            }
        }

        test("saddle is locked on a named Mount") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardOnBattlefield(1, "Caustic Bronco")
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bronco = game.findPermanent("Caustic Bronco")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.castFluteNaming("Caustic Bronco")

            game.getLegalActions(1).any { (it.action as? SaddleMount)?.mountId == bronco } shouldBe false
            game.execute(SaddleMount(game.player1Id, bronco, listOf(giant))).error shouldNotBe null
        }
    }
}
