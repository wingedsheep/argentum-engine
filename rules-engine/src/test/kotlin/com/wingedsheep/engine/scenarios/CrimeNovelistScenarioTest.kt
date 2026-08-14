package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CrimeNovelist
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crime Novelist (MKM) — {2}{R} 1/3 Goblin Bard.
 *
 * "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and add {R}."
 *
 * Two things are worth proving. First that both halves happen — the counter *and* the mana, since a
 * triggered ability producing mana is the unusual half. Second that the trigger is per-artifact
 * rather than per-event: sacrificing two artifacts to a single effect must grow it twice and produce
 * {R}{R}, which is what separates `YouSacrificeA` from the batch `YouSacrificeOneOrMore`.
 */
class CrimeNovelistScenarioTest : ScenarioTestBase() {

    // Free sorceries so nothing but the sacrifice is under test.
    private val sacrificeOneArtifact = card("Sacrifice One Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You sacrifice an artifact."
        spell {
            effect = Effects.Sacrifice(
                Filters.Unified.artifact,
                count = 1,
                target = EffectTarget.PlayerRef(Player.You)
            )
        }
    }

    private val sacrificeTwoArtifacts = card("Sacrifice Two Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You sacrifice two artifacts."
        spell {
            effect = Effects.Sacrifice(
                Filters.Unified.artifact,
                count = 2,
                target = EffectTarget.PlayerRef(Player.You)
            )
        }
    }

    private val sacrificeTargetCreature = card("Sacrifice Creature Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice target creature you control."
        spell {
            val victim = target("target creature you control", Targets.CreatureYouControl)
            effect = Effects.SacrificeTarget(victim)
        }
    }

    init {
        cardRegistry.register(CrimeNovelist)
        cardRegistry.register(sacrificeOneArtifact)
        cardRegistry.register(sacrificeTwoArtifacts)
        cardRegistry.register(sacrificeTargetCreature)

        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun redMana(game: TestGame): Int =
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red ?: 0

        context("Crime Novelist") {

            test("sacrificing one artifact grows it and adds {R}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crime Novelist", summoningSickness = false)
                    .withCardOnBattlefield(1, "Artifact Creature", summoningSickness = false)
                    .withCardInHand(1, "Sacrifice One Test")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val novelist = game.findPermanent("Crime Novelist")!!

                game.castSpell(1, "Sacrifice One Test").error shouldBe null
                game.resolveStack()

                withClue("the artifact is gone") {
                    game.isOnBattlefield("Artifact Creature") shouldBe false
                }
                withClue("one +1/+1 counter, reflected in the projected stats") {
                    counters(game, novelist) shouldBe 1
                    val projected = StateProjector().project(game.state)
                    projected.getPower(novelist) shouldBe 2
                    projected.getToughness(novelist) shouldBe 4
                }
                withClue("and exactly one red mana in the pool") {
                    redMana(game) shouldBe 1
                }
            }

            test("sacrificing two artifacts at once triggers twice, not once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crime Novelist", summoningSickness = false)
                    .withCardOnBattlefield(1, "Artifact Creature", summoningSickness = false)
                    .withCardOnBattlefield(1, "Artifact Creature", summoningSickness = false)
                    .withCardInHand(1, "Sacrifice Two Test")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val novelist = game.findPermanent("Crime Novelist")!!

                game.castSpell(1, "Sacrifice Two Test").error shouldBe null
                game.resolveStack()

                withClue("both artifacts are gone") {
                    game.findAllPermanents("Artifact Creature").size shouldBe 0
                }
                withClue("\"whenever you sacrifice an artifact\" is one trigger per artifact") {
                    counters(game, novelist) shouldBe 2
                    redMana(game) shouldBe 2
                }
            }

            test("sacrificing a nonartifact creature does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Crime Novelist", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardInHand(1, "Sacrifice Creature Test")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val novelist = game.findPermanent("Crime Novelist")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Sacrifice Creature Test", courser).error shouldBe null
                game.resolveStack()

                withClue("the Courser is not an artifact, so the Novelist is untouched") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    counters(game, novelist) shouldBe 0
                    redMana(game) shouldBe 0
                }
            }
        }
    }
}
