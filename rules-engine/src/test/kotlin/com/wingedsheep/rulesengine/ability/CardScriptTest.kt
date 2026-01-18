package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CardScriptTest : FunSpec({

    context("CardScript creation") {

        test("empty script for vanilla creatures") {
            val script = CardScript.empty("Grizzly Bears")
            script.cardName shouldBe "Grizzly Bears"
            script.isVanilla shouldBe true
            script.isFrenchVanilla shouldBe false
            script.hasAbilities shouldBe false
        }

        test("script with keywords only (French vanilla)") {
            val script = CardScript.withKeywords("Serra Angel", Keyword.FLYING, Keyword.VIGILANCE)
            script.cardName shouldBe "Serra Angel"
            script.isVanilla shouldBe false
            script.isFrenchVanilla shouldBe true
            script.hasAbilities shouldBe false
            script.keywords shouldContain Keyword.FLYING
            script.keywords shouldContain Keyword.VIGILANCE
        }

        test("script with triggered ability") {
            val script = CardScript(
                cardName = "Mulldrifter",
                triggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = OnEnterBattlefield(),
                        effect = DrawCardsEffect(2)
                    )
                )
            )
            script.hasAbilities shouldBe true
            script.isVanilla shouldBe false
            script.triggeredAbilities shouldHaveSize 1
        }

        test("script with spell effect") {
            val script = CardScript(
                cardName = "Lightning Bolt",
                spellEffect = SpellEffect(
                    effect = DealDamageEffect(3, EffectTarget.AnyTarget)
                )
            )
            script.hasAbilities shouldBe true
            script.spellEffect shouldNotBe null
            script.spellEffect?.description shouldBe "Deal 3 damage to any target"
        }

        test("script with conditional spell effect") {
            val script = CardScript(
                cardName = "Searing Blaze",
                spellEffect = SpellEffect(
                    effect = DealDamageEffect(3, EffectTarget.AnyTarget),
                    condition = ControlCreature,
                    elseEffect = DealDamageEffect(1, EffectTarget.AnyTarget)
                )
            )
            script.spellEffect?.description shouldBe
                "If you control a creature, deal 3 damage to any target. Otherwise, deal 1 damage to any target"
        }
    }

    context("CardScriptBuilder") {

        test("build script with keywords using builder") {
            val script = cardScript("Baneslayer Angel") {
                keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)
            }

            script.keywords shouldHaveSize 3
            script.keywords shouldContain Keyword.FLYING
            script.keywords shouldContain Keyword.FIRST_STRIKE
            script.keywords shouldContain Keyword.LIFELINK
        }

        test("build script with triggered ability using builder") {
            val script = cardScript("Elvish Visionary") {
                triggered(
                    trigger = OnEnterBattlefield(),
                    effect = DrawCardsEffect(1)
                )
            }

            script.triggeredAbilities shouldHaveSize 1
            script.triggeredAbilities[0].trigger shouldBe OnEnterBattlefield()
        }

        test("build script with activated ability using builder") {
            val script = cardScript("Llanowar Elves") {
                activated(
                    cost = AbilityCost.Tap,
                    effect = AddManaEffect(Color.GREEN)
                )
            }

            script.activatedAbilities shouldHaveSize 1
            script.activatedAbilities[0].cost shouldBe AbilityCost.Tap
        }

        test("build script with static abilities using builder") {
            val script = cardScript("Bonesplitter") {
                modifyStats(2, 0)
            }

            script.staticAbilities shouldHaveSize 1
            (script.staticAbilities[0] as ModifyStats).powerBonus shouldBe 2
            (script.staticAbilities[0] as ModifyStats).toughnessBonus shouldBe 0
        }

        test("build script with spell effect using builder") {
            val script = cardScript("Giant Growth") {
                spell(ModifyStatsEffect(3, 3, EffectTarget.TargetCreature))
            }

            script.spellEffect shouldNotBe null
            script.spellEffect?.effect shouldBe ModifyStatsEffect(3, 3, EffectTarget.TargetCreature)
        }

        test("build complex card script") {
            val script = cardScript("Glorybringer") {
                keywords(Keyword.FLYING, Keyword.HASTE)
                triggered(
                    trigger = OnAttack(),
                    effect = DealDamageEffect(4, EffectTarget.TargetOpponentCreature),
                    optional = true
                )
            }

            script.keywords shouldHaveSize 2
            script.triggeredAbilities shouldHaveSize 1
            script.triggeredAbilities[0].optional shouldBe true
        }
    }

    context("CardScriptRepository") {

        test("register and retrieve scripts") {
            val repository = CardScriptRepository()

            val bearScript = CardScript.empty("Grizzly Bears")
            val angelScript = CardScript.withKeywords("Serra Angel", Keyword.FLYING, Keyword.VIGILANCE)

            repository.register(bearScript)
            repository.register(angelScript)

            repository.getScript("Grizzly Bears") shouldBe bearScript
            repository.getScript("Serra Angel") shouldBe angelScript
            repository.getScript("Unknown Card") shouldBe null
        }

        test("hasScript returns correct value") {
            val repository = CardScriptRepository()
            repository.register(CardScript.empty("Test Card"))

            repository.hasScript("Test Card") shouldBe true
            repository.hasScript("Unknown") shouldBe false
        }

        test("registeredCards returns all card names") {
            val repository = CardScriptRepository()
            repository.registerAll(
                CardScript.empty("Card A"),
                CardScript.empty("Card B"),
                CardScript.empty("Card C")
            )

            repository.registeredCards() shouldBe setOf("Card A", "Card B", "Card C")
            repository.size shouldBe 3
        }

        test("clear removes all scripts") {
            val repository = CardScriptRepository()
            repository.register(CardScript.empty("Test"))

            repository.size shouldBe 1
            repository.clear()
            repository.size shouldBe 0
        }

        test("export to AbilityRegistry") {
            val repository = CardScriptRepository()
            val script = cardScript("Test Creature") {
                triggered(
                    trigger = OnEnterBattlefield(),
                    effect = GainLifeEffect(2)
                )
                staticAbility(GrantKeyword(Keyword.FLYING))
            }
            repository.register(script)

            val registry = AbilityRegistry()
            repository.exportTo(registry)

            registry.hasAbilities("Test Creature") shouldBe true
        }
    }

    context("AbilityRegistry integration") {

        test("register CardScript adds abilities to registry") {
            val registry = AbilityRegistry()

            val script = cardScript("Siege Rhino") {
                triggered(
                    trigger = OnEnterBattlefield(),
                    effect = CompositeEffect(listOf(
                        LoseLifeEffect(3, EffectTarget.EachOpponent),
                        GainLifeEffect(3)
                    ))
                )
            }

            registry.register(script)

            registry.hasAbilities("Siege Rhino") shouldBe true
            registry.getTriggeredAbilities(
                com.wingedsheep.rulesengine.card.CardDefinition.creature(
                    name = "Siege Rhino",
                    manaCost = com.wingedsheep.rulesengine.core.ManaCost.parse("{1}{W}{B}{G}"),
                    subtypes = setOf(com.wingedsheep.rulesengine.core.Subtype.BEAST),
                    power = 4,
                    toughness = 5
                )
            ) shouldHaveSize 1
        }
    }

    context("SpellEffect") {

        test("basic spell effect description") {
            val effect = SpellEffect(
                effect = DealDamageEffect(3, EffectTarget.AnyTarget)
            )
            effect.description shouldBe "Deal 3 damage to any target"
        }

        test("conditional spell effect description") {
            val effect = SpellEffect(
                effect = DrawCardsEffect(3),
                condition = ControlCreature
            )
            effect.description shouldBe "If you control a creature, draw 3 cards"
        }

        test("conditional spell effect with else clause description") {
            val effect = SpellEffect(
                effect = DealDamageEffect(5, EffectTarget.TargetCreature),
                condition = LifeTotalAtMost(10),
                elseEffect = DealDamageEffect(2, EffectTarget.TargetCreature)
            )
            effect.description shouldBe
                "If your life total is 10 or less, deal 5 damage to target creature. Otherwise, deal 2 damage to target creature"
        }
    }

    context("Example card scripts") {

        test("Lightning Bolt script") {
            val script = cardScript("Lightning Bolt") {
                spell(DealDamageEffect(3, EffectTarget.AnyTarget))
            }

            script.spellEffect?.description shouldBe "Deal 3 damage to any target"
        }

        test("Giant Growth script") {
            val script = cardScript("Giant Growth") {
                spell(ModifyStatsEffect(3, 3, EffectTarget.TargetCreature))
            }

            script.spellEffect?.description shouldBe "target creature gets +3/+3 until end of turn"
        }

        test("Llanowar Elves script") {
            val script = cardScript("Llanowar Elves") {
                activated(
                    cost = AbilityCost.Tap,
                    effect = AddManaEffect(Color.GREEN)
                )
            }

            script.activatedAbilities shouldHaveSize 1
            script.activatedAbilities[0].effect shouldBe AddManaEffect(Color.GREEN)
        }

        test("Blade of the Bloodchief equipment script") {
            val script = cardScript("Blade of the Bloodchief") {
                modifyStats(0, 0)
                triggered(
                    trigger = OnDeath(selfOnly = false),
                    effect = AddCountersEffect("+1/+1", 1, EffectTarget.Self)
                )
            }

            script.staticAbilities shouldHaveSize 1
            script.triggeredAbilities shouldHaveSize 1
        }

        test("Murder sorcery script") {
            val script = cardScript("Murder") {
                spell(DestroyEffect(EffectTarget.TargetCreature))
            }

            script.spellEffect?.description shouldBe "Destroy target creature"
        }
    }
})
