package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.shouldBe

/**
 * "If N or more mana from creatures was spent to cast it" — [SpellCastPredicate.PaidWithManaFromCardType].
 * The producing source's card types are snapshotted when the mana is made
 * ([com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker]) and counted by the payment,
 * whether the mana floated in the pool, was tapped by the auto-payer, or was restricted mana.
 */
class ManaFromCardTypeProvenanceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(ManaElf, PickyElf, GreenLand, CreatureManaWatcher, BigBeast, LandAnimator))

        fun board(elves: Int = 3, pickyElves: Int = 0, lands: Int = 1) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Creature Mana Watcher")
            .apply { repeat(elves) { withCardOnBattlefield(1, "Mana Elf") } }
            .apply { repeat(pickyElves) { withCardOnBattlefield(1, "Picky Elf") } }
            .apply { repeat(lands) { withCardOnBattlefield(1, "Green Land") } }
            .withCardInHand(1, "Big Beast")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.tapAll(name: String, def: CardDefinition) {
            val abilityId = def.activatedAbilities.first { it.isManaAbility }.id
            for (id in findAllPermanents(name)) {
                execute(ActivateAbility(player1Id, id, abilityId, manaColorChoice = Color.GREEN)).error shouldBe null
            }
        }

        context("counting mana from creatures spent on a cast") {
            test("three floating mana from creatures meets the threshold") {
                val game = board()
                game.tapAll("Mana Elf", ManaElf)
                game.tapAll("Green Land", GreenLand)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }

            test("two mana from creatures and two from a land falls short") {
                val game = board(elves = 2, lands = 2)
                game.tapAll("Mana Elf", ManaElf)
                game.tapAll("Green Land", GreenLand)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20
            }

            test("creatures tapped by the auto-payer count too") {
                val game = board(elves = 3, lands = 1)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }

            test("restricted mana from creatures carries its provenance") {
                val game = board(elves = 0, pickyElves = 3, lands = 1)
                game.tapAll("Picky Elf", PickyElf)
                game.tapAll("Green Land", GreenLand)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }

            test("an animated land's mana is mana from a creature (projected type at production)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Creature Mana Watcher")
                    .withCardOnBattlefield(1, "Land Animator")
                    .withCardOnBattlefield(1, "Mana Elf")
                    .withCardOnBattlefield(1, "Green Land")
                    .withCardOnBattlefield(1, "Green Land")
                    .withCardOnBattlefield(1, "Green Land")
                    .withCardInHand(1, "Big Beast")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                // Three animated lands are creatures; with the Elf that's four creature mana.
                game.tapAll("Green Land", GreenLand)
                game.tapAll("Mana Elf", ManaElf)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }

            test("mixing restricted and unrestricted creature mana adds up") {
                val game = board(elves = 1, pickyElves = 2, lands = 1)
                game.tapAll("Mana Elf", ManaElf)
                game.tapAll("Picky Elf", PickyElf)
                game.tapAll("Green Land", GreenLand)
                game.castSpell(1, "Big Beast").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }
        }
    }

    companion object {
        private val ManaElf = card("Mana Elf") {
            typeLine = "Creature — Elf"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }

        /** "{T}: Add one mana of any color. Spend this mana only to cast a creature spell." */
        private val PickyElf = card("Picky Elf") {
            typeLine = "Creature — Elf"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddManaOfChoice(
                    restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE)
                )
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }

        private val GreenLand = card("Green Land") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }

        private val CreatureManaWatcher = card("Creature Mana Watcher") {
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.you.casts(
                    GameObjectFilter.Creature,
                    requires = setOf(SpellCastPredicate.PaidWithManaFromCardType(CardType.CREATURE, atLeast = 3))
                )
                effect = Effects.GainLife(5)
            }
        }

        private val LandAnimator = card("Land Animator") {
            typeLine = "Enchantment"
            staticAbility {
                ability = AnimateLandGroup(GroupFilter(GameObjectFilter.Land.youControl()), 1, 1)
            }
        }

        private val BigBeast = card("Big Beast") {
            manaCost = "{3}{G}"
            typeLine = "Creature — Beast"
            power = 4
            toughness = 4
        }
    }
}
