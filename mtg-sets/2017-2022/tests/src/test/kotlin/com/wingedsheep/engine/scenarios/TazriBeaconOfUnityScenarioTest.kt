package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.znr.cards.TazriBeaconOfUnity
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tazri, Beacon of Unity (ZNR #44).
 *
 * - The self cost reduction reads the caster's party while Tazri is still in hand: a Cleric Rogue
 *   plus a Wizard is a party of two, so {4}{W} becomes {2}{W}.
 * - The activation cost is four monocolored hybrid symbols ({2/U}{2/B}{2/R}{2/G}) — each paid with
 *   one mana of its colour or {2} — and the dig keeps up to two party-type or Ally cards in total.
 */
class TazriBeaconOfUnityScenarioTest : ScenarioTestBase() {

    private val clericRogue = card("Tazri Test Cleric Rogue") {
        manaCost = "{1}"
        typeLine = "Creature — Vampire Cleric Rogue"
        power = 1
        toughness = 1
    }

    private val wizard = card("Tazri Test Wizard") {
        manaCost = "{1}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
    }

    private val ally = card("Tazri Test Ally") {
        manaCost = "{1}"
        typeLine = "Creature — Kor Ally"
        power = 1
        toughness = 1
    }

    private val digAbility = TazriBeaconOfUnity.activatedAbilities.single().id

    init {
        cardRegistry.register(clericRogue)
        cardRegistry.register(wizard)
        cardRegistry.register(ally)

        test("costs {1} less for each creature in your party") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tazri, Beacon of Unity")
                .withCardOnBattlefield(1, "Tazri Test Cleric Rogue")
                .withCardOnBattlefield(1, "Tazri Test Wizard")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tazri, Beacon of Unity").error shouldBe null
            game.resolveStack()

            game.findPermanent("Tazri, Beacon of Unity") shouldNotBe null
        }

        test("without a party Tazri costs the full {4}{W}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tazri, Beacon of Unity")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tazri, Beacon of Unity").error shouldNotBe null
            game.findPermanent("Tazri, Beacon of Unity") shouldBe null
        }

        test("hybrid activation pays colours or {2}, and keeps up to two matching cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tazri, Beacon of Unity")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Tazri Test Ally")
                .withCardInLibrary(1, "Tazri Test Wizard")
                .withCardInLibrary(1, "Tazri Test Cleric Rogue")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tazri = game.findPermanent("Tazri, Beacon of Unity")!!
            val allyCard = game.findCardsInLibrary(1, "Tazri Test Ally").single()
            val wizardCard = game.findCardsInLibrary(1, "Tazri Test Wizard").single()

            // {U}{B}{R} from the coloured lands, {2} from the two Plains for {2/G}.
            game.execute(ActivateAbility(game.player1Id, tazri, digAbility)).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(allyCard, wizardCard)).error shouldBe null

            game.isInHand(1, "Tazri Test Ally") shouldBe true
            game.isInHand(1, "Tazri Test Wizard") shouldBe true
            game.isInHand(1, "Tazri Test Cleric Rogue") shouldBe false
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.librarySize(1) shouldBe 2
        }
    }
}
