package com.wingedsheep.rulesengine.ability

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TriggerTest : FunSpec({

    context("OnEnterBattlefield trigger") {
        test("selfOnly trigger has correct description") {
            val trigger = OnEnterBattlefield(selfOnly = true)
            trigger.description shouldBe "When this enters the battlefield"
        }

        test("non-selfOnly trigger has correct description") {
            val trigger = OnEnterBattlefield(selfOnly = false)
            trigger.description shouldBe "Whenever a permanent enters the battlefield"
        }
    }

    context("OnDeath trigger") {
        test("selfOnly trigger has correct description") {
            val trigger = OnDeath(selfOnly = true)
            trigger.description shouldBe "When this creature dies"
        }

        test("non-selfOnly trigger has correct description") {
            val trigger = OnDeath(selfOnly = false)
            trigger.description shouldBe "Whenever a creature dies"
        }
    }

    context("OnDraw trigger") {
        test("controllerOnly trigger has correct description") {
            val trigger = OnDraw(controllerOnly = true)
            trigger.description shouldBe "Whenever you draw a card"
        }

        test("non-controllerOnly trigger has correct description") {
            val trigger = OnDraw(controllerOnly = false)
            trigger.description shouldBe "Whenever a player draws a card"
        }
    }

    context("OnAttack trigger") {
        test("selfOnly trigger has correct description") {
            val trigger = OnAttack(selfOnly = true)
            trigger.description shouldBe "Whenever this creature attacks"
        }

        test("non-selfOnly trigger has correct description") {
            val trigger = OnAttack(selfOnly = false)
            trigger.description shouldBe "Whenever a creature attacks"
        }
    }

    context("OnUpkeep trigger") {
        test("controllerOnly trigger has correct description") {
            val trigger = OnUpkeep(controllerOnly = true)
            trigger.description shouldBe "At the beginning of your upkeep"
        }

        test("non-controllerOnly trigger has correct description") {
            val trigger = OnUpkeep(controllerOnly = false)
            trigger.description shouldBe "At the beginning of each upkeep"
        }
    }

    context("OnDealsDamage trigger") {
        test("basic damage trigger has correct description") {
            val trigger = OnDealsDamage(selfOnly = true)
            trigger.description shouldBe "Whenever this creature deals damage"
        }

        test("combat damage only trigger has correct description") {
            val trigger = OnDealsDamage(selfOnly = true, combatOnly = true)
            trigger.description shouldBe "Whenever this creature deals combat damage"
        }

        test("damage to player trigger has correct description") {
            val trigger = OnDealsDamage(selfOnly = true, toPlayerOnly = true)
            trigger.description shouldBe "Whenever this creature deals damage to a player"
        }

        test("combat damage to player trigger has correct description") {
            val trigger = OnDealsDamage(selfOnly = true, combatOnly = true, toPlayerOnly = true)
            trigger.description shouldBe "Whenever this creature deals combat damage to a player"
        }
    }

    context("OnSpellCast trigger") {
        test("any spell trigger has correct description") {
            val trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.ANY)
            trigger.description shouldBe "Whenever you cast a spell"
        }

        test("creature spell trigger has correct description") {
            val trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.CREATURE)
            trigger.description shouldBe "Whenever you cast a creature spell"
        }

        test("instant or sorcery trigger has correct description") {
            val trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.INSTANT_OR_SORCERY)
            trigger.description shouldBe "Whenever you cast an instant or sorcery spell"
        }

        test("any player cast trigger has correct description") {
            val trigger = OnSpellCast(controllerOnly = false, spellType = SpellTypeFilter.ANY)
            trigger.description shouldBe "Whenever a player casts a spell"
        }
    }
})
