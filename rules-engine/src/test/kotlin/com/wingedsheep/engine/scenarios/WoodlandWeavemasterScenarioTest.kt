package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Woodland Weavemaster (HOB #143) — {1}{G} Creature — Elf Druid 1/2.
 *
 * "Vigilance
 *  Whenever another Elf you control enters, this creature gets +1/+1 until end of turn.
 *  {T}: Add X mana of any one color, where X is this creature's power. Spend this mana only to
 *  cast Elf spells and activate abilities of Elf sources."
 *
 * The trigger is OTHER-bound (its own arrival must not pump it) and "you control"-scoped, and the
 * mana amount is read at resolution so the pumps it has already taken are included.
 */
class WoodlandWeavemasterScenarioTest : ScenarioTestBase() {

    init {
        context("Woodland Weavemaster") {

            test("it is a 1/2 with vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Woodland Weavemaster")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val weavemaster = game.findPermanent("Woodland Weavemaster")!!
                game.state.projectedState.getPower(weavemaster) shouldBe 1
                game.state.projectedState.getToughness(weavemaster) shouldBe 2
                game.state.projectedState.hasKeyword(weavemaster, Keyword.VIGILANCE) shouldBe true
            }

            test("another Elf you control entering pumps it, but its own arrival did not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Woodland Weavemaster")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val weavemaster = game.findPermanent("Woodland Weavemaster")!!
                withClue("no self-pump from being on the battlefield already") {
                    game.state.projectedState.getPower(weavemaster) shouldBe 1
                }

                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()

                withClue("+1/+1 until end of turn from the other Elf") {
                    game.state.projectedState.getPower(weavemaster) shouldBe 2
                    game.state.projectedState.getToughness(weavemaster) shouldBe 3
                }
            }

            test("a non-Elf creature entering does not pump it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Woodland Weavemaster")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val weavemaster = game.findPermanent("Woodland Weavemaster")!!
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(weavemaster) shouldBe 1
            }

            test("the mana ability produces as much mana as its current power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Woodland Weavemaster")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Grow it to 2 power first, so the amount is demonstrably read at resolution.
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()

                val weavemaster = game.findPermanent("Woodland Weavemaster")!!
                game.state.projectedState.getPower(weavemaster) shouldBe 2

                val manaAbility = cardRegistry.requireCard("Woodland Weavemaster")
                    .activatedAbilities.single().id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = weavemaster,
                        abilityId = manaAbility,
                    )
                ).error shouldBe null
                // Mana abilities don't use the stack, but "any one color" pauses to ask first.
                val colorChoice = game.getPendingDecision() as ChooseColorDecision
                game.submitDecision(ColorChosenResponse(colorChoice.id, Color.GREEN)).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("power 2 produced two green mana, all of it Elf-restricted") {
                    pool.restrictedMana.size shouldBe 2
                    pool.restrictedMana.all { it.color == Color.GREEN } shouldBe true
                    pool.restrictedMana.all {
                        it.restriction == ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf")
                    } shouldBe true
                }
            }
        }
    }
}
