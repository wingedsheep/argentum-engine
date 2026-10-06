package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bogardan Dragonheart
 * {2}{R}
 * Creature — Human Shaman
 * 2/2
 * Sacrifice another creature: Until end of turn, this creature becomes a Dragon with base power
 * and toughness 4/4, flying, and haste.
 *
 * The Startling Development shape on itself: [Effects.BecomeCreature] *replaces* the creature
 * subtypes with Dragon (it stops being a Human Shaman), sets base P/T 4/4 in layer 7b — so pumps
 * and +1/+1 counters still apply on top — and grants flying and haste, all until end of turn. It
 * keeps its own abilities (third ruling). "Another" is [Costs.SacrificeAnother], so it can't feed
 * itself.
 */
val BogardanDragonheart = card("Bogardan Dragonheart") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Sacrifice another creature: Until end of turn, this creature becomes a Dragon " +
        "with base power and toughness 4/4, flying, and haste."

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            creatureTypes = setOf("Dragon"),
            duration = Duration.EndOfTurn,
        )
        description = "Sacrifice another creature: Until end of turn, this creature becomes a " +
            "Dragon with base power and toughness 4/4, flying, and haste."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Randy Vargas"
        flavorText = "A hunger to soar must be sated."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feb81f44-8f22-4d28-a452-a50bef69a3e3.jpg?1783933116"
        ruling(
            "2019-06-14",
            "Bogardan Dragonheart's ability overwrites all previous effects that set its creature " +
                "types, power, and/or toughness to specific values. Other effects that set these " +
                "characteristics to specific values that start to apply after the ability resolves " +
                "will overwrite that part of this effect.",
        )
        ruling(
            "2019-06-14",
            "Effects that modify Bogardan Dragonheart's power or toughness (such as the effects of " +
                "Force of Virtue or Giant Growth) will apply no matter when they started to take " +
                "effect. The same is true for counters that change its power or toughness (such as " +
                "+1/+1 counters).",
        )
        ruling("2019-06-14", "Bogardan Dragonheart doesn't lose any abilities when it becomes a Dragon.")
    }
}
