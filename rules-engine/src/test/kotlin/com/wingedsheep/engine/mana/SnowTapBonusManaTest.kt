package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.engine.core.ManaAbilityResolvedEvent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * An aura's "additional mana when enchanted land is tapped" is a triggered mana ability of the
 * aura, so the aura is that mana's source (CR 106.3) and the mana is snow exactly when the aura is
 * a snow permanent (CR 107.4h) — independent of whether the land itself is snow. Pinned on the
 * manual tap path (fixed and chosen color) and on the auto-pay solver, which may spend a snow
 * aura's bonus on a `{S}` pip and floats an unspent one as snow.
 */
class SnowTapBonusManaTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(Glade, SnowGlade, FrostBloom, WarmBloom, FrostDrift, RimeBolt, RimeSpark, RimeSurge, RimeEngine))

        fun board(land: String, aura: String, spell: String? = null) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, land)
            .withCardAttachedTo(1, aura, land)
            .apply { if (spell != null) withCardInHand(1, spell) }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.tap(land: String, card: CardDefinition) {
            execute(ActivateAbility(player1Id, findPermanent(land)!!, card.activatedAbilities.first { it.isManaAbility }.id))
                .error shouldBe null
        }

        fun TestGame.pool() = state.getEntity(player1Id)?.get<ManaPoolComponent>()!!

        fun TestGame.chooseColor(color: Color) {
            val decision = getPendingDecision() as? ChooseColorDecision
            withClue("the any-color bonus asks for its color") { decision shouldNotBe null }
            submitDecision(ColorChosenResponse(decision!!.id, color)).error shouldBe null
        }

        test("a snow aura's fixed-color bonus is snow mana even on a nonsnow land") {
            val game = board("Glade", "Frost Bloom")
            game.tap("Glade", Glade)
            val pool = game.pool()
            withClue("the land's {G} is not snow, the aura's {G} is; pool was $pool") {
                pool.green shouldBe 2
                pool.snowTotal shouldBe 1
            }
        }

        test("a nonsnow aura's any-color bonus on a snow land is not snow mana") {
            val game = board("Snow Glade", "Warm Bloom")
            game.tap("Snow Glade", SnowGlade)
            game.chooseColor(Color.BLUE)
            val pool = game.pool()
            withClue("only the snow land's {G} is snow; pool was $pool") {
                pool.green shouldBe 1
                pool.blue shouldBe 1
                pool.snowTotal shouldBe 1
                pool.snowMana[Color.GREEN] shouldBe 1
            }
        }

        test("a snow aura's any-color bonus is snow mana") {
            val game = board("Glade", "Frost Drift")
            game.tap("Glade", Glade)
            game.chooseColor(Color.RED)
            val pool = game.pool()
            withClue("the land (made snow) and the snow aura both make snow mana; pool was $pool") {
                pool.snowTotal shouldBe 2
                pool.snowMana[Color.RED] shouldBe 1
            }
        }

        test("auto-pay spends a snow aura's bonus on a second {S} pip") {
            val game = board("Glade", "Frost Drift", spell = "Rime Bolt")
            withClue("one land tapped once makes the two snow mana {S}{S} needs") {
                game.castSpell(1, "Rime Bolt").error shouldBe null
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
        }

        test("auto-pay cannot pay {S}{S} from a nonsnow aura's bonus") {
            val game = board("Snow Glade", "Warm Bloom", spell = "Rime Bolt")
            withClue("the nonsnow aura's bonus can't pay the second {S}") {
                game.castSpell(1, "Rime Bolt").error shouldNotBe null
            }
        }

        test("auto-pay pays {S}{S}{1}: one aura-enchanted land covers both pips, another the generic") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Glade")
                .withCardAttachedTo(1, "Frost Drift", "Glade")
                .withCardOnBattlefield(1, "Snow Glade")
                .withCardInHand(1, "Rime Surge")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val result = game.castSpell(1, "Rime Surge")
            result.error shouldBe null
            withClue("both lands tapped, nothing floats") {
                game.state.getEntity(game.findPermanent("Glade")!!)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.findPermanent("Snow Glade")!!)?.has<TappedComponent>() shouldBe true
                game.pool().total shouldBe 0
            }
            withClue("the land reports only its own {G}; the aura's mana isn't booked as the land's") {
                result.events.filterIsInstance<ManaAbilityResolvedEvent>()
                    .filter { it.sourceId == game.findPermanent("Glade") }
                    .map { it.amount } shouldBe listOf(1)
            }
        }

        test("a snow permanent's 'whenever a land is tapped for mana' bonus is snow mana") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Glade")
                .withCardOnBattlefield(1, "Rime Engine")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.tap("Glade", Glade)
            val pool = game.pool()
            withClue("the land's {G} is not snow, the snow engine's {G} is; pool was $pool") {
                pool.green shouldBe 2
                pool.snowTotal shouldBe 1
            }
        }

        test("an unspent snow bonus floats as snow mana after auto-pay") {
            val game = board("Glade", "Frost Drift", spell = "Rime Spark")
            game.castSpell(1, "Rime Spark").error shouldBe null
            val pool = game.pool()
            withClue("the aura's unspent bonus floats and is still snow; pool was $pool") {
                pool.total shouldBe 1
                pool.snowTotal shouldBe 1
            }
        }
    }

    companion object {
        private val Glade = card("Glade") {
            typeLine = "Land"
            activatedAbility {
                manaAbility = true
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
            }
        }

        private val SnowGlade = card("Snow Glade") {
            typeLine = "Snow Land"
            activatedAbility {
                manaAbility = true
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN)
            }
        }

        /** A snow aura with a fixed-color bonus, on a land it doesn't make snow. */
        private val FrostBloom = card("Frost Bloom") {
            typeLine = "Snow Enchantment — Aura"
            staticAbility { ability = AdditionalManaOnTap(color = Color.GREEN, amount = DynamicAmount.Fixed(1)) }
        }

        /** Fertile Ground's shape: a nonsnow aura with an any-color bonus. */
        private val WarmBloom = card("Warm Bloom") {
            typeLine = "Enchantment — Aura"
            staticAbility { ability = AdditionalManaOnTap(amount = DynamicAmount.Fixed(1), anyColor = true) }
        }

        /** Glittering Frost's shape. */
        private val FrostDrift = card("Frost Drift") {
            typeLine = "Snow Enchantment — Aura"
            staticAbility { ability = GrantSupertype("SNOW") }
            staticAbility { ability = AdditionalManaOnTap(amount = DynamicAmount.Fixed(1), anyColor = true) }
        }

        private val RimeBolt = card("Rime Bolt") {
            manaCost = "{S}{S}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(2) }
        }

        private val RimeSurge = card("Rime Surge") {
            manaCost = "{1}{S}{S}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(3) }
        }

        /** A snow permanent with a global "whenever a land is tapped for mana, add {G}". */
        private val RimeEngine = card("Rime Engine") {
            typeLine = "Snow Enchantment"
            staticAbility { ability = AdditionalManaOnSourceTap(sourceFilter = GameObjectFilter.Land, color = Color.GREEN) }
        }

        private val RimeSpark = card("Rime Spark") {
            manaCost = "{S}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(1) }
        }
    }
}
