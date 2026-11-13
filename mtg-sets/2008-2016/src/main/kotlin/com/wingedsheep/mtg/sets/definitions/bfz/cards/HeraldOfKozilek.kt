package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Herald of Kozilek
 * {1}{U}{R}
 * Creature — Eldrazi Drone
 * 2/4
 * Devoid (This card has no color.)
 * Colorless spells you cast cost {1} less to cast.
 *
 * The discount is Ugin, the Ineffable's `ModifySpellCost` static at {1} instead of {2}:
 * `YouCast(Any.withCardPredicate(IsColorless))` + `ReduceGeneric(1)`. The cost calculator reads
 * the spell's colors off `CardDefinition.colors`, which is empty for a devoid card in every zone,
 * so devoid spells (and artifacts, and Eldrazi with `{C}` costs) all get the discount, while a
 * card given a colour off the battlefield does not. `ReduceGeneric` only shaves generic mana,
 * matching the rulings.
 */
val HeraldOfKozilek = card("Herald of Kozilek") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Colorless spells you cast cost {1} less to cast."

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Johannes Voss"
        flavorText = "\"Some Eldrazi, their purpose is obvious: suck the life out of the land, birth new " +
            "Eldrazi. Others aren't so obvious, and those are the ones that make me really nervous.\"\n" +
            "—Chadir the Navigator"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a4180ae-1fd9-4185-b4f0-9e8f668a5737.jpg?1783938181"
        ruling(
            "2015-08-25",
            "Herald of Kozilek's last ability doesn't change the mana cost or mana value of any " +
                "spell. It changes only the total cost you actually pay."
        )
        ruling(
            "2015-08-25",
            "Herald of Kozilek's last ability can't reduce the amount of colored mana you pay for a " +
                "spell. It reduces only the generic component of that mana cost."
        )
        ruling(
            "2015-08-25",
            "If there are additional costs to cast a spell, or if the cost to cast a spell is " +
                "increased by an effect, apply those increases before applying cost reductions."
        )
        ruling("2015-08-25", "The cost reduction can apply to alternative costs.")
        ruling(
            "2015-08-25",
            "If a colorless spell you cast has {X} in its mana cost, you choose the value of X " +
                "before calculating the spell's total cost. For example, if that spell's mana cost " +
                "is {X}, you could choose 5 as the value of X and pay {4} to cast the spell."
        )
    }
}
