package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The graveyard exile-self cost: "{3}{R}{R}, Exile this card from your graveyard: …" — the
 * `AbilityCost.ExileSelf` the battlefield's "Exile ~" builds, with the zone CR 113.6m derives from
 * the cost landing on `activateFromZone`.
 */
class GraveyardExileCostTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    // Cobbled Lancer's golden, field for field.
    "the graveyard cost is ExileSelf on an ability that works from the graveyard" {
        fragment("{3}{U}, Exile ~ from your graveyard: Draw a card.") shouldBe CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility(
                        id = AbilityId("activated"),
                        cost = Costs.Composite(Costs.Mana(ManaCost.parse("{3}{U}")), AbilityCost.ExileSelf),
                        effect = Effects.DrawCards(1),
                        activateFromZone = Zone.GRAVEYARD,
                    )
                )
            )
        )
        roundTrips("{3}{U}, Exile ~ from your graveyard: Draw a card.")
    }

    "the riders of the activated sentence reach the graveyard cost" {
        roundTrips("{2}{G}, Exile ~ from your graveyard: Put two +1/+1 counters on target creature. Activate only as a sorcery.")
            .also {
                fragment("{2}{G}, Exile ~ from your graveyard: Put two +1/+1 counters on target creature. Activate only as a sorcery.")
                    .script.activatedAbilities.single().timing shouldBe TimingRule.SorcerySpeed
            }
        roundTrips("{1}, {T}, Exile ~ from your graveyard: You gain 2 life.")
    }

    // Fail-closed in both directions: the battlefield "Exile ~" keeps its zone, and an ability that
    // works from the graveyard without exiling itself never borrows the graveyard spelling.
    "the battlefield exile keeps its own sentence" {
        fragment("{2}, Exile ~: Draw a card.").script.activatedAbilities.single().activateFromZone shouldBe Zone.BATTLEFIELD
        roundTrips("{2}, Exile ~: Draw a card.")
        Grammar.abilityLine.printLine(
            CardFragment(
                script = CardScript(
                    activatedAbilities = listOf(
                        ActivatedAbility(
                            id = AbilityId("activated"),
                            cost = Costs.Mana(ManaCost.parse("{2}")),
                            effect = Effects.DrawCards(1),
                            activateFromZone = Zone.GRAVEYARD,
                        )
                    )
                )
            )
        ) shouldBe null
    }
})
