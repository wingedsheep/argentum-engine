package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Skittering Precursor (MH3 #137): "Whenever you sacrifice a nontoken permanent, create a 0/1
 * colorless Eldrazi Spawn creature token…" — per nontoken permanent, never for a token.
 */
class SkitteringPrecursorScenarioTest : ScenarioTestBase() {

    private val scrapTheWorkshop = card("Scrap the Workshop") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all artifacts you control."
        spell {
            effect = Effects.SacrificeAll(GameObjectFilter.Artifact.youControl())
        }
    }

    init {
        cardRegistry.register(scrapTheWorkshop)

        context("Skittering Precursor") {

            test("nontoken sacrifices each make a Spawn; a token sacrifice makes none") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Skittering Precursor")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInHand(1, "Scrap the Workshop")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scrap the Workshop").error shouldBe null
                game.resolveStack()

                withClue("all three artifacts were sacrificed") {
                    game.findPermanents("Ornithopter") shouldHaveSize 0
                    game.findPermanents("Food") shouldHaveSize 0
                }
                withClue("two nontoken Ornithopters -> two Spawn; the Food token adds none") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                }
            }

            test("sacrificing only a token makes no Spawn") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Skittering Precursor")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInHand(1, "Scrap the Workshop")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scrap the Workshop").error shouldBe null
                game.resolveStack()

                game.findPermanents("Food") shouldHaveSize 0
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }
        }
    }
}
