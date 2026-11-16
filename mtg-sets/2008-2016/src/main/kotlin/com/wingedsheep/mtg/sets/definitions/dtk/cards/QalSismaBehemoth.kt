package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackOrBlockUnlessPay

/**
 * Qal Sisma Behemoth
 * {2}{R}
 * Creature — Ogre Warrior
 * 5/5
 *
 * This creature can't attack or block unless you pay {2}.
 *
 * The {2} is a cost of declaring it as an attacker or blocker, priced per declaration through
 * [CantAttackOrBlockUnlessPay] (the Myr Prototype shape with a fixed amount). Attacking and blocking
 * are charged separately, and a "must attack/block if able" requirement never forces the payment.
 */
val QalSismaBehemoth = card("Qal Sisma Behemoth") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "This creature can't attack or block unless you pay {2}."
    power = 5
    toughness = 5

    staticAbility {
        ability = CantAttackOrBlockUnlessPay(amount = DynamicAmounts.fixed(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Evan Shipard"
        flavorText = "\"It hunts grand game for Atarka, and when the time comes, it will become a mighty feast.\"\n—Surrak, the Hunt Caller"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea70e96a-bebd-4ca4-aa24-e5fd0df38b8a.jpg?1783938587"
        ruling("2015-02-25", "If an effect says that Qal Sisma Behemoth must attack or block if able, you can choose not to pay the associated cost and ignore that requirement. Players are never forced to pay a cost to attack or block.")
    }
}
