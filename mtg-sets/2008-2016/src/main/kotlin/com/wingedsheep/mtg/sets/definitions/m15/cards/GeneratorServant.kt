package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Generator Servant — Magic 2015 #143
 * {1}{R}
 * Creature — Elemental
 * 2/1
 * {T}, Sacrifice this creature: Add {C}{C}. If any of that mana is spent on a creature spell, it
 * gains haste until end of turn.
 *
 * Carnelian Orb of Dragonkind's end-of-turn haste rider, unfiltered to any creature spell, on two
 * colorless mana. Each {C} carries its own copy of the rider in the pool, so mana split across two
 * creature spells gives both haste (2018-12-07 ruling); the rider lives on the mana, not the
 * Servant, so sacrificing it as part of the cost doesn't lose the haste.
 */
val GeneratorServant = card("Generator Servant") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "{T}, Sacrifice this creature: Add {C}{C}. If any of that mana is spent on a creature spell, " +
        "it gains haste until end of turn. (That creature can attack and {T} as soon as it comes under your control.)"
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddColorlessMana(
            2,
            riders = setOf(
                ManaSpellRider.GrantsKeywordWhenSpent(
                    keyword = Keyword.HASTE,
                    spellFilter = GameObjectFilter.Creature,
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74d0c422-4201-4d6f-9df7-659e8b78b541.jpg?1783939173"
        ruling("2018-12-07", "If the mana is spent on two different creature spells, each of those spells (and resulting creatures) will gain haste until end of turn.")
        ruling("2018-12-07", "If a creature spell has generic mana only in its alternative or additional cost, the creature will still gain haste.")
        ruling("2018-12-07", "An instant or sorcery spell is not a creature spell, even if that spell creates creature tokens.")
        ruling("2018-12-07", "If the mana is spent on a noncreature spell that becomes a creature later in the turn, that creature won't have haste.")
    }
}
