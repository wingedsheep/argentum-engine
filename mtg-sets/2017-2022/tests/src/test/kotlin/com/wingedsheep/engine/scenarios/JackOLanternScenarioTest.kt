package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.JackOLantern
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Jack-o'-Lantern (MID #254)
 * "{1}, {T}, Sacrifice this artifact: Exile up to one target card from a graveyard. Draw a card.
 *  {1}, Exile this card from your graveyard: Add one mana of any color."
 *
 * The graveyard ability is a mana ability (CR 605.1a): activated at priority or while a payment
 * asks for mana (CR 605.3a), never on the stack, and never spent by auto-pay.
 */
class JackOLanternScenarioTest : ScenarioTestBase() {

    private val sacAbility = JackOLantern.activatedAbilities[0].id
    private val graveyardManaAbility = JackOLantern.activatedAbilities[1].id

    init {
        context("Jack-o'-Lantern") {
            test("exiling it from the graveyard turns {1} into one mana of any color without using the stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jack-o'-Lantern")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val jack = game.findCardsInGraveyard(1, "Jack-o'-Lantern").single()
                val activate = ActivateAbility(game.player1Id, jack, graveyardManaAbility, manaColorChoice = Color.RED)

                withClue("offered from the graveyard, flagged as a mana ability") {
                    game.getLegalActions(1).single { it.action.let { a -> a is ActivateAbility && a.sourceId == jack } }
                        .isManaAbility shouldBe true
                }

                game.execute(activate).error shouldBe null
                game.state.stack.shouldBeEmpty()
                game.isInExile(1, "Jack-o'-Lantern") shouldBe true
                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 1

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("a spell only it can fix opens a payment window, where it is activatable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jack-o'-Lantern")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val jack = game.findCardsInGraveyard(1, "Jack-o'-Lantern").single()

                withClue("its fixing counts toward affordability: one colored pip, paid by any mana") {
                    services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{R}")) shouldBe true
                    services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{1}{R}")) shouldBe false
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                val window = game.state.pendingDecision as SelectManaSourcesDecision
                withClue("auto-pay never spends the card") {
                    game.isInGraveyard(1, "Jack-o'-Lantern") shouldBe true
                }
                services.legalActionEnumerator.enumerateManaAbilities(game.state, game.player1Id)
                    .any { (it.action as? ActivateAbility)?.sourceId == jack } shouldBe true

                game.execute(ActivateAbility(game.player1Id, jack, graveyardManaAbility, manaColorChoice = Color.RED))
                    .error shouldBe null
                game.state.pendingDecision!!.id shouldBe window.id
                game.submitDecision(ManaSourcesSelectedResponse(window.id)).error shouldBe null
                game.state.stack.size shouldBe 1
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Jack-o'-Lantern") shouldBe true
            }

            test("each Jack-o'-Lantern fixes exactly one colored pip") {
                val oneJack = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jack-o'-Lantern")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                services.manaSolver.canPay(oneJack.state, oneJack.player1Id, ManaCost.parse("{1}{R}")) shouldBe true
                services.manaSolver.canPay(oneJack.state, oneJack.player1Id, ManaCost.parse("{R}{R}")) shouldBe false
                withClue("paying a Phyrexian pip with life doesn't refund the fixer") {
                    services.manaSolver.canPay(oneJack.state, oneJack.player1Id, ManaCost.parse("{R}{R}{R/P}")) shouldBe false
                }

                val twoJacks = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jack-o'-Lantern")
                    .withCardInGraveyard(1, "Jack-o'-Lantern")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                services.manaSolver.canPay(twoJacks.state, twoJacks.player1Id, ManaCost.parse("{R}{R}")) shouldBe true
            }

            test("on the battlefield the graveyard ability isn't usable, and sacrificing it exiles a card and draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jack-o'-Lantern")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val jack = game.findPermanent("Jack-o'-Lantern")!!
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

                game.getLegalActions(1).none {
                    it.action == ActivateAbility(game.player1Id, jack, graveyardManaAbility)
                } shouldBe true
                services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{R}")) shouldBe false

                val handBefore = game.handSize(1)
                game.execute(
                    ActivateAbility(game.player1Id, jack, sacAbility, targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Jack-o'-Lantern") shouldBe true
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
