package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenous Demon // Archdemon of Greed
 *
 * Front: {3}{B}{B} Creature — Demon 4/4.
 * "Sacrifice a Human: Transform this creature. Activate only as a sorcery."
 *
 * Back: Archdemon of Greed — Creature — Demon 9/9, black color indicator. Flying, trample.
 * "At the beginning of your upkeep, sacrifice a Human. If you can't, tap this creature and it
 * deals 9 damage to you."
 *
 * The upkeep fork is the Lord of the Pit / Rust Elemental shape: checked as the trigger resolves,
 * the sacrifice is mandatory whenever you control a Human, and only with none does the Archdemon
 * tap and deal 9 damage to you (the Archdemon is the damage source). Unlike those cards the
 * wording is "a Human", not "another", so the Archdemon is not excluded — if it has somehow become
 * a Human it can be sacrificed to its own trigger.
 */
private val HumanPermanent = GameObjectFilter.Permanent.withSubtype("Human")

private val RavenousDemonFront = card("Ravenous Demon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Sacrifice a Human: Transform this creature. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Sacrifice(HumanPermanent)
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Sacrifice a Human: Transform Ravenous Demon."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Igor Kieryluk"
        flavorText = "His twisted words entice the weak."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6aef77b3-4b38-4902-9f7a-dc18b5bb9da9.jpg?1783940832"
    }
}

private val ArchdemonOfGreed = card("Archdemon of Greed") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Demon"
    power = 9
    toughness = 9
    oracleText = "Flying, trample\n" +
        "At the beginning of your upkeep, sacrifice a Human. If you can't, tap this creature and it " +
        "deals 9 damage to you."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.YouControl(HumanPermanent),
            then = Effects.SacrificeOwn(HumanPermanent),
            otherwise = Effects.Tap(EffectTarget.Self) then
                Effects.DealDamage(9, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Igor Kieryluk"
        flavorText = "\"The price is paid. The power is mine!\""
        imageUri = "https://cards.scryfall.io/normal/back/6/a/6aef77b3-4b38-4902-9f7a-dc18b5bb9da9.jpg?1783940832"
    }
}

val RavenousDemon: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = RavenousDemonFront,
    backFace = ArchdemonOfGreed,
)
