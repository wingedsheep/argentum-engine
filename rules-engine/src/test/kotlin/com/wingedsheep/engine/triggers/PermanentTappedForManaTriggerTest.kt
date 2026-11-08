package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for "whenever you tap a land for mana" — `EventPattern.PermanentTappedForMana`, spelled
 * `Triggers.self.tappedForMana()` (this land) and `Triggers.<player>.tapsLandForMana(land)` — and its
 * general form `Triggers.<player>.tapsPermanentForMana(filter)` for any mana source (an artifact,
 * a creature), where the tapped permanent is the triggering entity.
 *
 * The event must fire the same way however the player paid: a manual activation of the mana
 * ability, the auto-payer tapping the land for a spell, or an explicit list of sources. A non-mana
 * rider like this uses the stack; only a trigger that adds mana (and doesn't target) is a mana
 * ability itself (CR 605.1b). Only a mana ability with {T} in its cost taps the land *for mana*, and
 * only a land fires the land-specific trigger.
 */
class PermanentTappedForManaTriggerTest : FunSpec({

    val grove = card("Rider Grove") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        triggeredAbility {
            trigger = Triggers.self.tappedForMana()
            effect = Effects.GainLife(1)
        }
    }

    // A land mana ability without {T}: using it doesn't tap the land for mana.
    val bloodSpring = card("Rider Blood Spring") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.PayLife(2)
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        triggeredAbility {
            trigger = Triggers.self.tappedForMana()
            effect = Effects.GainLife(5)
        }
    }

    val rock = card("Rider Rock") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val landWatcher = card("Rider Land Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.tapsLandForMana()
            effect = Effects.GainLife(1)
        }
    }

    // "Whenever an opponent taps an artifact for mana, gain control of that artifact …" — the
    // permanent form, with the tapped permanent as the triggering entity.
    val artifactThief = card("Rider Artifact Thief") {
        manaCost = "{0}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.anOpponent.tapsPermanentForMana(GameObjectFilter.Artifact)
            effect = Effects.GainControl(EffectTarget.TriggeringEntity, Duration.EndOfYourNextTurn)
        }
    }

    val creatureWatcher = card("Rider Creature Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.anyPlayer.tapsPermanentForMana(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }

    val dork = card("Rider Dork") {
        manaCost = "{0}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val golem = card("Rider Golem") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Golem"
        power = 1
        toughness = 1
    }

    val bear = card("Rider Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(grove, bloodSpring, rock, landWatcher, artifactThief, creatureWatcher, dork, golem, bear))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a manual activation fires the trigger, which uses the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putPermanentOnBattlefield(you, "Rider Grove")

        d.submit(ActivateAbility(you, land, grove.activatedAbilities.single().id)).error shouldBe null

        d.stackSize shouldBe 1
        d.getLifeTotal(you) shouldBe 20
        d.bothPass()
        d.getLifeTotal(you) shouldBe 21
    }

    test("the auto-payer tapping the land for a spell fires the trigger") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Rider Grove")

        val card = d.putCardInHand(you, "Rider Bear")
        d.submit(CastSpell(you, card, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        d.stackSize shouldBe 2
        d.bothPass()
        d.getLifeTotal(you) shouldBe 21
    }

    test("an explicit source list fires the trigger") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putPermanentOnBattlefield(you, "Rider Grove")

        val card = d.putCardInHand(you, "Rider Bear")
        d.submit(
            CastSpell(you, card, paymentStrategy = PaymentStrategy.Explicit(listOf(land)))
        ).error shouldBe null

        d.isTapped(land) shouldBe true
        d.stackSize shouldBe 2
        d.bothPass()
        d.getLifeTotal(you) shouldBe 21
    }

    test("a land mana ability without {T} doesn't tap the land for mana") {
        val d = driver()
        val you = d.activePlayer!!
        val land = d.putPermanentOnBattlefield(you, "Rider Blood Spring")

        d.submit(ActivateAbility(you, land, bloodSpring.activatedAbilities.single().id)).error shouldBe null

        d.stackSize shouldBe 0
        d.getLifeTotal(you) shouldBe 18
    }

    test("the player-wide form counts lands only, and only its controller's taps") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Rider Land Watcher")
        val artifact = d.putPermanentOnBattlefield(you, "Rider Rock")
        val yourLand = d.putPermanentOnBattlefield(you, "Rider Grove")
        val theirLand = d.putPermanentOnBattlefield(opponent, "Rider Grove")

        // Tapping an artifact for mana isn't tapping a land for mana.
        d.submit(ActivateAbility(you, artifact, rock.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 0

        // Your land: the watcher and the Grove's own rider both trigger.
        d.submit(ActivateAbility(you, yourLand, grove.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(you) shouldBe 22

        // The opponent's land: only their Grove's rider, under their control.
        d.passPriority(you)
        d.submit(ActivateAbility(opponent, theirLand, grove.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(you) shouldBe 22
        d.getLifeTotal(opponent) shouldBe 21
    }

    test("the permanent form fires for an opponent's artifact, not for their land or your own artifact") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Rider Artifact Thief")
        val yourRock = d.putPermanentOnBattlefield(you, "Rider Rock")
        val theirLand = d.putPermanentOnBattlefield(opponent, "Rider Grove")
        val theirRock = d.putPermanentOnBattlefield(opponent, "Rider Rock")

        // Your own artifact: "an opponent" doesn't include you.
        d.submit(ActivateAbility(you, yourRock, rock.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 0

        // Their land is not an artifact (only their Grove's own rider fires).
        d.passPriority(you)
        d.submit(ActivateAbility(opponent, theirLand, grove.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        d.state.projectedState.getController(theirRock) shouldBe opponent

        // Their artifact: the Thief's trigger goes on the stack and takes the very artifact tapped.
        d.passPriority(you)
        d.submit(ActivateAbility(opponent, theirRock, rock.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        d.state.projectedState.getController(theirRock) shouldBe you
        d.isTapped(theirRock) shouldBe true
    }

    test("the permanent form is any mana source: a creature tapped for mana, by any player") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Rider Creature Watcher")
        val yourDork = d.putCreatureOnBattlefield(you, "Rider Dork")
        val theirDork = d.putCreatureOnBattlefield(opponent, "Rider Dork")
        d.removeSummoningSickness(yourDork)
        d.removeSummoningSickness(theirDork)
        val rockForYou = d.putPermanentOnBattlefield(you, "Rider Rock")

        d.submit(ActivateAbility(you, rockForYou, rock.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 0 // an artifact that isn't a creature

        d.submit(ActivateAbility(you, yourDork, dork.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()

        d.passPriority(you)
        d.submit(ActivateAbility(opponent, theirDork, dork.activatedAbilities.single().id)).error shouldBe null
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(you) shouldBe 22
    }

    test("paying a spell by tapping an artifact for mana fires the permanent form too") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Rider Artifact Thief")
        val yourRock = d.putPermanentOnBattlefield(you, "Rider Rock")

        val card = d.putCardInHand(you, "Rider Golem")
        d.submit(CastSpell(you, card, paymentStrategy = PaymentStrategy.Explicit(listOf(yourRock)))).error shouldBe null

        // The Thief belongs to the opponent and "an opponent" is you: it triggers for your tap.
        d.stackSize shouldBe 2
        d.bothPass()
        d.state.projectedState.getController(yourRock) shouldBe opponent
    }
})
