package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gastal Thrillroller
 * {2}{R}
 * Artifact — Vehicle
 * 4/2
 * Trample, haste
 * When this Vehicle enters, it becomes an artifact creature until end of turn.
 * Crew 2
 * {2}{R}, Discard a card: Return this card from your graveyard to the battlefield with a finality
 * counter on it. Activate only as a sorcery.
 *
 * The enters trigger self-animates to the Vehicle's own printed 4/2 (the convention Rocketeer
 * Boostbuggy uses) — the permanent is already an artifact, so only CREATURE is added, and the
 * trample/haste it prints carry over for the turn it's alive.
 *
 * The recursion ability is activated from the graveyard ([Zone.GRAVEYARD]) and pairs
 * [Effects.PutOntoBattlefield] with a finality counter, the same shape as Relentless X-ATM092.
 */
val GastalThrillroller = card("Gastal Thrillroller") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    oracleText = "Trample, haste\n" +
        "When this Vehicle enters, it becomes an artifact creature until end of turn.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)\n" +
        "{2}{R}, Discard a card: Return this card from your graveyard to the battlefield with a " +
        "finality counter on it. Activate only as a sorcery."
    power = 4
    toughness = 2

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 2,
            duration = Duration.EndOfTurn
        )
        description = "When this Vehicle enters, it becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(2))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardCard)
        effect = Effects.Composite(
            Effects.PutOntoBattlefield(EffectTarget.Self),
            Effects.AddCounters(Counters.FINALITY, 1, EffectTarget.Self)
        )
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        description = "{2}{R}, Discard a card: Return this card from your graveyard to the " +
            "battlefield with a finality counter on it"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8b1762b-ff03-4312-afcc-cb6b5f280ade.jpg?1783907881"
    }
}
