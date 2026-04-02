package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Builder's Talent {1}{W}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * When this Class enters, create a 0/4 white Wall creature token with defender.
 *
 * {W}: Level 2
 * Whenever one or more noncreature, nonland permanents you control enter,
 * put a +1/+1 counter on target creature you control.
 *
 * {4}{W}: Level 3
 * When this Class becomes level 3, return target noncreature, nonland permanent
 * card from your graveyard to the battlefield.
 */
val BuildersTalent = card("Builder's Talent") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Class"
    oracleText = "When this Class enters, create a 0/4 white Wall creature token with defender.\n" +
        "{W}: Level 2 — Whenever one or more noncreature, nonland permanents you control enter, " +
        "put a +1/+1 counter on target creature you control.\n" +
        "{4}{W}: Level 3 — When this Class becomes level 3, return target noncreature, nonland " +
        "permanent card from your graveyard to the battlefield."

    // Level 1: ETB — create a 0/4 white Wall token with defender
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 0,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Wall"),
            keywords = setOf(Keyword.DEFENDER),
            imageUri = "https://cards.scryfall.io/normal/front/2/2/229a41de-91dd-4696-bfc1-10d702787c3e.jpg?1721424801"
        )
    }

    // Level 2: Whenever one or more noncreature, nonland permanents you control enter,
    // put a +1/+1 counter on target creature you control.
    classLevel(2, "{W}") {
        triggeredAbility {
            trigger = Triggers.OneOrMorePermanentsEnter(
                GameObjectFilter.Noncreature and GameObjectFilter.Nonland
            )
            val creature = target("creature you control", Targets.CreatureYouControl)
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        }
    }

    // Level 3: When this Class becomes level 3, return target noncreature, nonland permanent
    // card from your graveyard to the battlefield.
    classLevel(3, "{4}{W}") {
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            val card = target(
                "noncreature, nonland permanent card in your graveyard",
                TargetObject(
                    filter = TargetFilter(
                        baseFilter = (GameObjectFilter.NoncreaturePermanent and GameObjectFilter.Nonland).ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
            effect = Effects.PutOntoBattlefield(card)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Ovidio Cartagena"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15fa581a-724e-4196-a9a3-ff84c54bdb7d.jpg?1739650809"
        ruling("2024-07-26", "Each Class has five abilities. The three in the major sections of its text box are class abilities. Class abilities can be static, activated, or triggered abilities. The other two are level abilities, one activated ability to advance the Class to level 2 and another to advance the Class to level 3.")
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You can't activate the first level ability of a Class unless that Class is level 1. You can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
