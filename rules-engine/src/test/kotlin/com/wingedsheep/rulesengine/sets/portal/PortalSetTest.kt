package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PortalSetTest : FunSpec({

    context("PortalSet basic properties") {

        test("set code and name are correct") {
            PortalSet.setCode shouldBe "POR"
            PortalSet.setName shouldBe "Portal"
        }

        test("has 10 cards registered") {
            PortalSet.cardCount shouldBe 10
        }
    }

    context("Alabaster Dragon") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Alabaster Dragon")
            definition.shouldNotBeNull()
            definition.name shouldBe "Alabaster Dragon"
            definition.manaCost.cmc shouldBe 6
            definition.manaCost.colorCount[Color.WHITE] shouldBe 2
            definition.creatureStats?.basePower shouldBe 4
            definition.creatureStats?.baseToughness shouldBe 4
            definition.keywords shouldContain Keyword.FLYING
            definition.typeLine.subtypes shouldContain Subtype.DRAGON
        }

        test("script has flying keyword and death trigger") {
            val script = PortalSet.getCardScript("Alabaster Dragon")
            script.shouldNotBeNull()
            script.keywords shouldContain Keyword.FLYING
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0].trigger
            trigger.shouldBeInstanceOf<OnDeath>()

            val effect = script.triggeredAbilities[0].effect
            effect.shouldBeInstanceOf<ShuffleIntoLibraryEffect>()
            (effect as ShuffleIntoLibraryEffect).target shouldBe EffectTarget.Self
        }
    }

    context("Alluring Scent") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Alluring Scent")
            definition.shouldNotBeNull()
            definition.name shouldBe "Alluring Scent"
            definition.manaCost.cmc shouldBe 3
            definition.manaCost.colorCount[Color.GREEN] shouldBe 2
            definition.isSorcery shouldBe true
        }

        test("script has must-be-blocked spell effect") {
            val script = PortalSet.getCardScript("Alluring Scent")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<MustBeBlockedEffect>()
            (effect as MustBeBlockedEffect).target shouldBe EffectTarget.TargetCreature
        }
    }

    context("Anaconda") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Anaconda")
            definition.shouldNotBeNull()
            definition.name shouldBe "Anaconda"
            definition.manaCost.cmc shouldBe 4
            definition.manaCost.colorCount[Color.GREEN] shouldBe 1
            definition.creatureStats?.basePower shouldBe 3
            definition.creatureStats?.baseToughness shouldBe 3
            definition.keywords shouldContain Keyword.SWAMPWALK
        }

        test("script has swampwalk keyword") {
            val script = PortalSet.getCardScript("Anaconda")
            script.shouldNotBeNull()
            script.keywords shouldContain Keyword.SWAMPWALK
        }
    }

    context("Ancestral Memories") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Ancestral Memories")
            definition.shouldNotBeNull()
            definition.name shouldBe "Ancestral Memories"
            definition.manaCost.cmc shouldBe 5
            definition.manaCost.colorCount[Color.BLUE] shouldBe 3
            definition.isSorcery shouldBe true
        }

        test("script has look-at-top-cards spell effect") {
            val script = PortalSet.getCardScript("Ancestral Memories")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<LookAtTopCardsEffect>()
            (effect as LookAtTopCardsEffect).count shouldBe 7
            effect.keepCount shouldBe 2
            effect.restToGraveyard shouldBe true
        }
    }

    context("Angelic Blessing") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Angelic Blessing")
            definition.shouldNotBeNull()
            definition.name shouldBe "Angelic Blessing"
            definition.manaCost.cmc shouldBe 3
            definition.manaCost.colorCount[Color.WHITE] shouldBe 1
            definition.isSorcery shouldBe true
        }

        test("script has composite effect with +3/+3 and flying") {
            val script = PortalSet.getCardScript("Angelic Blessing")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<CompositeEffect>()

            val compositeEffect = effect as CompositeEffect
            compositeEffect.effects shouldHaveSize 2

            val statsEffect = compositeEffect.effects[0]
            statsEffect.shouldBeInstanceOf<ModifyStatsEffect>()
            (statsEffect as ModifyStatsEffect).powerModifier shouldBe 3
            statsEffect.toughnessModifier shouldBe 3

            val keywordEffect = compositeEffect.effects[1]
            keywordEffect.shouldBeInstanceOf<GrantKeywordUntilEndOfTurnEffect>()
            (keywordEffect as GrantKeywordUntilEndOfTurnEffect).keyword shouldBe Keyword.FLYING
        }
    }

    context("Archangel") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Archangel")
            definition.shouldNotBeNull()
            definition.name shouldBe "Archangel"
            definition.manaCost.cmc shouldBe 7
            definition.manaCost.colorCount[Color.WHITE] shouldBe 2
            definition.creatureStats?.basePower shouldBe 5
            definition.creatureStats?.baseToughness shouldBe 5
            definition.keywords shouldContain Keyword.FLYING
            definition.keywords shouldContain Keyword.VIGILANCE
            definition.typeLine.subtypes shouldContain Subtype.ANGEL
        }

        test("script is French vanilla with flying and vigilance") {
            val script = PortalSet.getCardScript("Archangel")
            script.shouldNotBeNull()
            script.isFrenchVanilla shouldBe true
            script.keywords shouldContain Keyword.FLYING
            script.keywords shouldContain Keyword.VIGILANCE
        }
    }

    context("Ardent Militia") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Ardent Militia")
            definition.shouldNotBeNull()
            definition.name shouldBe "Ardent Militia"
            definition.manaCost.cmc shouldBe 5
            definition.manaCost.colorCount[Color.WHITE] shouldBe 1
            definition.creatureStats?.basePower shouldBe 2
            definition.creatureStats?.baseToughness shouldBe 5
            definition.keywords shouldContain Keyword.VIGILANCE
        }

        test("script is French vanilla with vigilance") {
            val script = PortalSet.getCardScript("Ardent Militia")
            script.shouldNotBeNull()
            script.isFrenchVanilla shouldBe true
            script.keywords shouldContain Keyword.VIGILANCE
        }
    }

    context("Armageddon") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Armageddon")
            definition.shouldNotBeNull()
            definition.name shouldBe "Armageddon"
            definition.manaCost.cmc shouldBe 4
            definition.manaCost.colorCount[Color.WHITE] shouldBe 1
            definition.isSorcery shouldBe true
        }

        test("script has destroy-all-lands spell effect") {
            val script = PortalSet.getCardScript("Armageddon")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()

            val effect = script.spellEffect!!.effect
            effect shouldBe DestroyAllLandsEffect
        }
    }

    context("Armored Pegasus") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Armored Pegasus")
            definition.shouldNotBeNull()
            definition.name shouldBe "Armored Pegasus"
            definition.manaCost.cmc shouldBe 2
            definition.manaCost.colorCount[Color.WHITE] shouldBe 1
            definition.creatureStats?.basePower shouldBe 1
            definition.creatureStats?.baseToughness shouldBe 2
            definition.keywords shouldContain Keyword.FLYING
        }

        test("script is French vanilla with flying") {
            val script = PortalSet.getCardScript("Armored Pegasus")
            script.shouldNotBeNull()
            script.isFrenchVanilla shouldBe true
            script.keywords shouldContain Keyword.FLYING
        }
    }

    context("Arrogant Vampire") {

        test("definition has correct properties") {
            val definition = PortalSet.getCardDefinition("Arrogant Vampire")
            definition.shouldNotBeNull()
            definition.name shouldBe "Arrogant Vampire"
            definition.manaCost.cmc shouldBe 5
            definition.manaCost.colorCount[Color.BLACK] shouldBe 2
            definition.creatureStats?.basePower shouldBe 4
            definition.creatureStats?.baseToughness shouldBe 3
            definition.keywords shouldContain Keyword.FLYING
        }

        test("script is French vanilla with flying") {
            val script = PortalSet.getCardScript("Arrogant Vampire")
            script.shouldNotBeNull()
            script.isFrenchVanilla shouldBe true
            script.keywords shouldContain Keyword.FLYING
        }
    }
})
