package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "This ability triggers only N times each turn" — `TriggeredAbility.triggersPerTurn`, the counted
 * form of the `oncePerTurn` trigger cap (Nadu, Winged Wisdom: "…only twice each turn").
 *
 *  - *Twice Warden* caps a printed trigger: a sweeper that damages three creatures at once puts only
 *    **two** instances on the stack (the same-pass collapse), and later damage that turn adds none.
 *  - *Twice Granter* grants the capped trigger to creatures. Each granter's grant is its own ability
 *    with its own count (the Nadu rulings: "These abilities are not redundant"), so two granters
 *    give one creature four triggers a turn, not two. A granter that leaves and returns is a new
 *    object granting a new ability, so its fresh grant comes with a fresh count (CR 400.7).
 */
private val TwiceWarden = card("Twice Warden") {
    manaCost = "{2}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control is dealt damage, you gain 1 life. " +
        "This ability triggers only twice each turn."

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(Recipient.CreatureYouControl)
        effect = Effects.GainLife(1)
        triggersPerTurn = 2
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "T1"
    }
}

private val TwiceGranter = card("Twice Granter") {
    manaCost = "{2}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have \"Whenever this creature becomes the target of a spell or " +
        "ability, you gain 1 life. This ability triggers only twice each turn.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.becomesTarget()
                effect = Effects.GainLife(1)
                triggersPerTurn = 2
            },
            filter = GroupFilter.AllCreaturesYouControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "T2"
    }
}

private val GranterRecall = card("Granter Recall") {
    manaCost = "{0}"
    typeLine = "Instant"
    oracleText = "Return target permanent to its owner's hand."

    spell {
        val t = target(TargetFilter.Permanent)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "T3"
    }
}

class TriggersPerTurnCapTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(TwiceWarden, TwiceGranter, GranterRecall))

        fun growthGame(granters: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .apply { repeat(granters) { withCardOnBattlefield(1, "Twice Granter") } }
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardsInHand(1, "Giant Growth", 3)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withLifeTotal(1, 20)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.growBearsThreeTimes() {
            val bears = findPermanent("Grizzly Bears")!!
            repeat(3) {
                castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
                resolveStack()
            }
        }

        test("simultaneous events past the cap collapse to the allowance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Twice Warden")
                .withCardOnBattlefield(1, "Force of Nature")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardInHand(1, "Pyroclasm")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pyroclasm").error shouldBe null
            game.resolveStack()
            withClue("three creatures dealt damage at once trigger it only twice") {
                game.getLifeTotal(1) shouldBe 22
            }

            game.castSpell(1, "Lightning Bolt", targetId = game.findPermanent("Force of Nature")!!).error shouldBe null
            game.resolveStack()
            withClue("the cap is spent for the turn") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("a granted capped trigger fires twice per creature, then stops") {
            val game = growthGame(granters = 1)
            game.growBearsThreeTimes()
            game.getLifeTotal(1) shouldBe 22
        }

        test("two granters grant two separately counted abilities") {
            val game = growthGame(granters = 2)
            game.growBearsThreeTimes()
            withClue("each grant triggers twice — four triggers, not two shared") {
                game.getLifeTotal(1) shouldBe 24
            }
        }

        test("a granter that leaves and returns grants a new ability with a fresh count") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Twice Granter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardsInHand(1, "Giant Growth", 3)
                .withCardInHand(1, "Granter Recall")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            repeat(2) {
                game.castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
                game.resolveStack()
            }
            withClue("the first granter's ability has spent its two triggers") { game.getLifeTotal(1) shouldBe 22 }

            game.castSpell(1, "Granter Recall", targetId = game.findPermanent("Twice Granter")!!).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Twice Granter").error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
            game.resolveStack()
            withClue("the returned granter's ability is a new one and triggers") { game.getLifeTotal(1) shouldBe 23 }
        }

        test("the cap is two or more, and does not combine with oncePerTurn") {
            fun capped(once: Boolean, perTurn: Int) = card("Bad Cap") {
                typeLine = "Enchantment"
                triggeredAbility {
                    trigger = Triggers.self.becomesTarget()
                    effect = Effects.GainLife(1)
                    oncePerTurn = once
                    triggersPerTurn = perTurn
                }
            }
            shouldThrow<IllegalArgumentException> { capped(once = false, perTurn = 1) }
            shouldThrow<IllegalArgumentException> { capped(once = true, perTurn = 2) }
        }
    }
}
