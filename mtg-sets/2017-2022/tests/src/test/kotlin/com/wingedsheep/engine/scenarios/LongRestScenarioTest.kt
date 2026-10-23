package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Long Rest (AFR #193) — {X}{G}{G}{G} Sorcery.
 *
 *   Return X target cards with different mana values from your graveyard to your hand. If eight or
 *   more cards were returned to your hand this way, your life total becomes equal to your starting
 *   life total. Exile Long Rest.
 *
 * Pins `TargetObject.differentNumericProperty = MANA_VALUE` (two cards sharing a mana value can't
 * both be targeted) and the eight-or-more life reset.
 */
class LongRestScenarioTest : ScenarioTestBase() {

    // Mana values 0 through 7, one card each.
    private val ladder = listOf(
        "Ornithopter", "Llanowar Elves", "Grizzly Bears", "Centaur Courser",
        "Hill Giant", "Serra Angel", "Craw Wurm", "Duskdale Wurm",
    )

    init {
        context("Long Rest") {

            test("X = 8 with eight different mana values returns them all and resets life to 20") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long Rest")
                    .withLandsOnBattlefield(1, "Forest", 11)
                    .withLifeTotal(1, 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                ladder.forEach { builder.withCardInGraveyard(1, it) }
                val game = builder.build()

                val targets = ladder.map { name ->
                    ChosenTarget.Card(game.findCardsInGraveyard(1, name).single(), game.player1Id, Zone.GRAVEYARD)
                }
                val longRest = game.findCardsInHand(1, "Long Rest").single()
                game.execute(CastSpell(game.player1Id, longRest, targets, xValue = 8)).error shouldBe null
                game.resolveStack()

                withClue("every targeted card returned to hand") {
                    ladder.forEach { game.isInHand(1, it) shouldBe true }
                }
                withClue("eight returned: life total becomes the starting 20") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("Long Rest exiles itself") {
                    game.isInExile(1, "Long Rest") shouldBe true
                }
            }

            test("only cards actually returned count: a target gone before resolution leaves seven and no reset") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long Rest")
                    .withLandsOnBattlefield(1, "Forest", 11)
                    .withLifeTotal(1, 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                ladder.forEach { builder.withCardInGraveyard(1, it) }
                val game = builder.build()

                val ids = ladder.map { game.findCardsInGraveyard(1, it).single() }
                val longRest = game.findCardsInHand(1, "Long Rest").single()
                game.execute(
                    CastSpell(game.player1Id, longRest, ids.map { ChosenTarget.Card(it, game.player1Id, Zone.GRAVEYARD) }, xValue = 8)
                ).error shouldBe null
                // Duskdale Wurm leaves the graveyard while Long Rest is on the stack.
                game.state = game.state.moveToZone(
                    ids.last(), ZoneKey(game.player1Id, Zone.GRAVEYARD), ZoneKey(game.player1Id, Zone.EXILE)
                )
                game.resolveStack()

                withClue("the seven remaining targets return") {
                    ladder.dropLast(1).forEach { game.isInHand(1, it) shouldBe true }
                    game.isInHand(1, "Duskdale Wurm") shouldBe false
                }
                withClue("seven returned is fewer than eight: life stays at 5") {
                    game.getLifeTotal(1) shouldBe 5
                }
                game.isInExile(1, "Long Rest") shouldBe true
            }

            test("X is capped at the number of different mana values in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long Rest")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Elvish Mystic")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val longRest = game.findCardsInHand(1, "Long Rest").single()
                val cast = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == longRest }
                // Three cards but only two mana values (1 and 2): X = 3 has no legal choice of targets.
                cast.maxAffordableX shouldBe 2
            }

            test("two cards with the same mana value can't both be targeted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long Rest")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Elvish Mystic")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findCardsInGraveyard(1, "Llanowar Elves").single()
                val mystic = game.findCardsInGraveyard(1, "Elvish Mystic").single()
                val longRest = game.findCardsInHand(1, "Long Rest").single()
                game.execute(
                    CastSpell(
                        game.player1Id, longRest,
                        listOf(
                            ChosenTarget.Card(elves, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(mystic, game.player1Id, Zone.GRAVEYARD),
                        ),
                        xValue = 2,
                    )
                ).error shouldNotBe null
            }

            test("fewer than eight returned leaves the life total alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long Rest")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLifeTotal(1, 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targets = listOf("Llanowar Elves", "Grizzly Bears").map { name ->
                    ChosenTarget.Card(game.findCardsInGraveyard(1, name).single(), game.player1Id, Zone.GRAVEYARD)
                }
                val longRest = game.findCardsInHand(1, "Long Rest").single()
                game.execute(CastSpell(game.player1Id, longRest, targets, xValue = 2)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Llanowar Elves") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 7
            }
        }
    }
}
