package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mariposa Military Base — Fallout #151
 * Land · Rare
 *
 * You may have this land enter tapped. If you do, you get two rad counters.
 * {T}: Add {C}.
 * {5}, {T}: Draw a card. This ability costs {1} less to activate for each rad counter you have.
 *
 * The optional tapped entry is the reveal-land shape ([OnEnterRun] runs inline with the entry,
 * before the land is ever untapped on the battlefield) with a plain yes/no in place of the
 * reveal: answering yes taps the land and then gives you the two rad counters, answering no
 * leaves it untapped and gives nothing. The discount reads your rad counters when the ability
 * is activated (`genericCostReduction`), and only touches the generic {5} — never the {T}.
 */
val MariposaMilitaryBase = card("Mariposa Military Base") {
    typeLine = "Land"
    oracleText = "You may have this land enter tapped. If you do, you get two rad counters.\n" +
        "{T}: Add {C}.\n" +
        "{5}, {T}: Draw a card. This ability costs {1} less to activate for each rad counter you have."

    replacementEffect(
        OnEnterRun(
            Effects.May(
                Effects.Tap(EffectTarget.Self) then
                    Effects.AddCounters(CounterType.RAD, 2, EffectTarget.Controller),
                prompt = "Have Mariposa Military Base enter tapped to get two rad counters?",
                descriptionOverride = "have this land enter tapped. If you do, you get two rad counters",
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        genericCostReduction = DynamicAmounts.playerCounterCount(CounterType.RAD, Player.You)
        effect = Effects.DrawCards(1)
        description = "{5}, {T}: Draw a card. This ability costs {1} less to activate for each rad " +
            "counter you have."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd7be9d4-f9fa-44a3-9902-7e9e226ccf56.jpg?1783912373"
    }
}
