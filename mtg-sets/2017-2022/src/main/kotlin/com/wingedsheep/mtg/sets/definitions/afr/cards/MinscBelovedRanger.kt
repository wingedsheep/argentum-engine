package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Minsc, Beloved Ranger
 * {R}{G}{W}
 * Legendary Creature — Human Ranger
 * 3/3
 *
 * When Minsc enters, create Boo, a legendary 1/1 red Hamster creature token with trample and haste.
 * {X}: Until end of turn, target creature you control has base power and toughness X/X and becomes
 * a Giant in addition to its other types. Activate only as a sorcery.
 *
 * Boo is named "Boo" with creature type Hamster, so the token gets both `name` and
 * `creatureTypes`, plus `legendary = true` for the legend rule.
 */
val MinscBelovedRanger = card("Minsc, Beloved Ranger") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Ranger"
    oracleText = "When Minsc enters, create Boo, a legendary 1/1 red Hamster creature token with trample and haste.\n" +
        "{X}: Until end of turn, target creature you control has base power and toughness X/X and " +
        "becomes a Giant in addition to its other types. Activate only as a sorcery."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Hamster"),
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            name = "Boo",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c49e8e79-8673-41c2-a1ad-273c37e27aca.jpg?1783926344"
        )
        description = "When Minsc enters, create Boo, a legendary 1/1 red Hamster creature token with trample and haste."
    }

    activatedAbility {
        cost = Costs.Mana("{X}")
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.SetBasePowerAndToughness(
            power = DynamicAmounts.xValue(),
            toughness = DynamicAmounts.xValue(),
            target = creature,
            duration = Duration.EndOfTurn
        ) then Effects.AddCreatureType("Giant", creature, Duration.EndOfTurn)
        description = "Until end of turn, target creature you control has base power and toughness X/X " +
            "and becomes a Giant in addition to its other types. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72405607-a60e-42af-b523-64008d3f89b9.jpg?1783926446"
    }
}
