package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nylea, Keen-Eyed — Theros Beyond Death #185
 * {3}{G} · Legendary Enchantment Creature — God · 5/6
 *
 * Indestructible
 * As long as your devotion to green is less than five, Nylea isn't a creature.
 * Creature spells you cast cost {1} less to cast.
 * {2}{G}: Reveal the top card of your library. If it's a creature card, put it into your hand.
 * Otherwise, you may put it into your graveyard.
 *
 * The god clause is the Nylea, God of the Hunt shape. The cost reduction is Honest Rutstein's
 * `ModifySpellCost(YouCast(Creature), ReduceGeneric(1))`. The reveal is Kellan, Daring
 * Traveler's gather-top → creature-to-hand, otherwise optional graveyard pipeline; a declined
 * noncreature card stays on top of the library (ruling).
 */
val NyleaKeenEyed = card("Nylea, Keen-Eyed") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 6
    oracleText = "Indestructible\n" +
        "As long as your devotion to green is less than five, Nylea isn't a creature.\n" +
        "Creature spells you cast cost {1} less to cast.\n" +
        "{2}{G}: Reveal the top card of your library. If it's a creature card, put it into your hand. " +
        "Otherwise, you may put it into your graveyard."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.GREEN), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Creature),
                then = Effects.Pipeline { toHand(revealed) },
                otherwise = Effects.May(
                    Effects.Pipeline { toGraveyard(revealed) },
                    descriptionOverride = "Put the revealed card into your graveyard?"
                )
            ))
        }
        description = "{2}{G}: Reveal the top card of your library. If it's a creature card, put it into " +
            "your hand. Otherwise, you may put it into your graveyard."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "185"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdf894d4-1a06-4952-a481-22786ab87a73.jpg?1783931534"
        ruling(
            "2020-01-24",
            "If you don't put the revealed noncreature card into your graveyard, it remains on top of your library."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
