package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Veteran Adventurer — Zendikar Rising #218
 * {5}{G} · Creature — Human · 5/5
 *
 * Veteran Adventurer is also a Cleric, Rogue, Warrior, and Wizard.
 * This spell costs {1} less to cast for each creature in your party.
 * Vigilance
 *
 * The four self-[GrantSubtype]s are the same all-zones characteristic-defining ability as Tajuru
 * Paragon. The discount is the self-cast [ModifySpellCost] Deadly Alliance uses, reading
 * [DynamicAmounts.partySize] for the caster — the party is the creatures you control, so the
 * Adventurer on the stack never counts itself; a full party casts it for {1}{G}.
 */
val VeteranAdventurer = card("Veteran Adventurer") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    oracleText = "Veteran Adventurer is also a Cleric, Rogue, Warrior, and Wizard.\n" +
        "This spell costs {1} less to cast for each creature in your party.\n" +
        "Vigilance"
    power = 5
    toughness = 5

    for (subtype in listOf("Cleric", "Rogue", "Warrior", "Wizard")) {
        staticAbility { ability = GrantSubtype(subtype) }
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Jakub Kasper"
        flavorText = "\"How can I be of assistance?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e524ddf0-9a2f-4c1c-ae72-4b5b48f55645.jpg?1783929326"
        ruling(
            "2020-09-25",
            "The ability that adds creature types to Veteran Adventurer applies in all zones, not just the battlefield."
        )
        ruling(
            "2020-09-25",
            "To determine \"the number of creatures in your party,\" check whether you control a Cleric, whether " +
                "you control a Rogue, whether you control a Warrior, and whether you control a Wizard. The number " +
                "is the total number of those checks to which you answered yes. Each creature you control can be " +
                "counted for only one of those checks."
        )
        ruling(
            "2020-09-25",
            "If a creature has more than one party creature type, and there are multiple ways to count that " +
                "creature that could result in a different number of creatures in your party, the highest such " +
                "number is used. For example, if you control a Cleric and a Cleric Wizard, the number of creatures " +
                "in your party is two. You can't choose to have it be just one by counting the Cleric Wizard first " +
                "as a Cleric."
        )
        ruling(
            "2020-09-25",
            "Several cards have a cost reduction based on the number of creatures in your party. To determine the " +
                "total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost " +
                "increases, then apply any cost reductions. The mana value of the spell is determined only by its " +
                "mana cost, no matter what the total cost to cast the spell was."
        )
    }
}
