package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * The Celestus
 * {3}
 * Legendary Artifact
 * If it's neither day nor night, it becomes day as The Celestus enters.
 * {T}: Add one mana of any color.
 * {3}, {T}: If it's night, it becomes day. Otherwise, it becomes night. Activate only as a sorcery.
 * Whenever day becomes night or night becomes day, you gain 1 life. You may draw a card. If you do,
 * discard a card.
 *
 * The toggle is a plain resolution-time [Effects.If] over [Conditions.IsNight]: night becomes day,
 * anything else (day, or — only if the designation was somehow lost — neither) becomes night. Either
 * branch is a real day/night change, so it fires The Celestus's own trigger.
 */
val TheCelestus = card("The Celestus") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "If it's neither day nor night, it becomes day as The Celestus enters.\n" +
        "{T}: Add one mana of any color.\n" +
        "{3}, {T}: If it's night, it becomes day. Otherwise, it becomes night. Activate only as a sorcery.\n" +
        "Whenever day becomes night or night becomes day, you gain 1 life. You may draw a card. " +
        "If you do, discard a card."

    becomesDayAsEntersIfNeither()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.If(
            condition = Conditions.IsNight,
            then = Effects.BecomeDay,
            otherwise = Effects.BecomeNight,
        )
        timing = TimingRule.SorcerySpeed
        description = "{3}, {T}: If it's night, it becomes day. Otherwise, it becomes night. " +
            "Activate only as a sorcery."
    }

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Effects.GainLife(1) then Effects.May(Patterns.Hand.loot())
        description = "Whenever day becomes night or night becomes day, you gain 1 life. " +
            "You may draw a card. If you do, discard a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39226a07-d44e-41a6-b9f1-685ef505d015.jpg?1783925548"
    }
}
