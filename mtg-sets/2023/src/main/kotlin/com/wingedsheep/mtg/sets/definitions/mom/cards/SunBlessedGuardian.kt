package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sun-Blessed Guardian // Furnace-Blessed Conqueror (March of the Machine #38)
 * {1}{W} Creature — Human Cleric 2/2 // Creature — Phyrexian Cleric 3/3 (red-white color indicator)
 *
 * Front — "{5}{R/P}: Transform this creature. Activate only as a sorcery."
 * Back  — "Whenever this creature attacks, create a tapped and attacking token that's a copy of it.
 *          Put a +1/+1 counter on that token for each +1/+1 counter on this creature. Sacrifice
 *          that token at the beginning of the next end step."
 *
 * The copy takes only copiable values (no counters, per the ruling); the token then gets as many
 * +1/+1 counters as the Conqueror has, via the CREATED_TOKENS collection the copy publishes.
 */
private val SunBlessedGuardianFront = card("Sun-Blessed Guardian") {
    manaCost = "{1}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{5}{R/P}: Transform this creature. Activate only as a sorcery. " +
        "({R/P} can be paid with either {R} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{5}{R/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Brian Valeza"
        flavorText = "Light glinted off the raised weapons of the Sun Empire, filling her with an " +
            "unbreakable hope . . ."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/630ce82b-0d09-4a12-8cba-0b0ed8415128.jpg?1783917056"
    }
}

private val FurnaceBlessedConqueror = card("Furnace-Blessed Conqueror") {
    manaCost = ""
    colorIndicator = "WR" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "WR"
    typeLine = "Creature — Phyrexian Cleric"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, create a tapped and attacking token that's a copy " +
        "of it. Put a +1/+1 counter on that token for each +1/+1 counter on this creature. " +
        "Sacrifice that token at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.Self,
            tapped = true,
            attacking = true,
            sacrificeAtStep = Step.END,
            sacrificeOnlyOnControllersTurn = false,
        ) then Effects.AddCountersToCollection(
            CREATED_TOKENS,
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Brian Valeza"
        flavorText = ". . . but even the unbreakable can be reshaped."
        imageUri = "https://cards.scryfall.io/normal/back/6/3/630ce82b-0d09-4a12-8cba-0b0ed8415128.jpg?1783917056"
    }
}

val SunBlessedGuardian: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SunBlessedGuardianFront,
    backFace = FurnaceBlessedConqueror,
)
