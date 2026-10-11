package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gourmand's Talent {G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * During your turn, artifacts you control are Foods in addition to their other types
 * and have "{2}, {T}, Sacrifice this artifact: You gain 3 life."
 *
 * {2}{G}: Level 2
 * Whenever you gain life for the first time each turn, create a 3/3 green Raccoon
 * creature token.
 *
 * {3}{G}: Level 3
 * Whenever you gain life for the first time each turn, put a +1/+1 counter on each
 * creature you control.
 *
 * Level 1 is Ragost, Deft Gastronaut's Food grant with both halves gated by
 * [Conditions.IsYourTurn]: the type grant is a conditional continuous effect re-evaluated
 * in projection, and the activated-ability grant is unwrapped against the Class by the
 * ability enumerator, so on an opponent's turn your artifacts are neither Foods nor able
 * to activate the life-gain ability.
 */
val GourmandsTalent = card("Gourmand's Talent") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "During your turn, artifacts you control are Foods in addition to their other types and have " +
        "\"{2}, {T}, Sacrifice this artifact: You gain 3 life.\"\n" +
        "{2}{G}: Level 2\n" +
        "Whenever you gain life for the first time each turn, create a 3/3 green Raccoon creature token.\n" +
        "{3}{G}: Level 3\n" +
        "Whenever you gain life for the first time each turn, put a +1/+1 counter on each creature you control."

    // Level 1: during your turn, artifacts you control are Foods in addition to their other types...
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.youControl()),
            addSubtypes = listOf("Food")
        )
    }

    // ...and have "{2}, {T}, Sacrifice this artifact: You gain 3 life."
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.GainLife(3)
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.youControl())
        )
    }

    // Level 2: whenever you gain life for the first time each turn, create a 3/3 green Raccoon.
    classLevel(2, "{2}{G}") {
        triggeredAbility {
            trigger = Triggers.you.gainsLife(firstTimeEachTurn = true)
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Raccoon")
            )
        }
    }

    // Level 3: whenever you gain life for the first time each turn, put a +1/+1 counter on
    // each creature you control.
    classLevel(3, "{3}{G}") {
        triggeredAbility {
            trigger = Triggers.you.gainsLife(firstTimeEachTurn = true)
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89670a1b-a6f1-491d-bbb4-92e3382da653.jpg?1783910728"
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2024-07-26", "The artifacts retain any types, subtypes, supertypes, and abilities they have.")
        ruling("2024-07-26", "If Gourmand's Talent somehow becomes an artifact, it will also be a Food during your turn.")
        ruling("2024-07-26", "The level 2 and level 3 class abilities of Gourmand's Talent won't trigger if you already gained life earlier in the turn on which it gained those levels, even if you gain life again later in the turn.")
    }
}
