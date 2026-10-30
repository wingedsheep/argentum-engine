package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Alirios, Enraptured
 * {2}{U}
 * Legendary Creature — Human
 * 2/3
 * Alirios enters tapped.
 * Alirios doesn't untap during your untap step if you control a Reflection.
 * When Alirios enters, create a 3/2 blue Reflection creature token.
 *
 * The untap clause is Goblin Rock Sled's shape: a [ConditionalStaticAbility] granting
 * `DOESNT_UNTAP` to itself, re-evaluated each untap step, so Alirios untaps normally once no
 * Reflection remains (and spells can still untap it, per the rulings).
 */
val AliriosEnraptured = card("Alirios, Enraptured") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human"
    power = 2
    toughness = 3
    oracleText = "Alirios enters tapped.\n" +
        "Alirios doesn't untap during your untap step if you control a Reflection.\n" +
        "When Alirios enters, create a 3/2 blue Reflection creature token."

    replacementEffect(EntersTapped())

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Reflection")),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Reflection"),
            imageUri = "https://cards.scryfall.io/normal/front/a/b/ab1c23b2-ac39-46b1-aa2b-7d6cbedba767.jpg?1783931454"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Kieran Yanner"
        flavorText = "\"Why would I ever want to look away?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26f9ebbe-3791-4d19-88d2-d5de95faff48.jpg?1783931586"
        ruling("2020-01-24", "Spells and abilities can untap Alirios, even if it doesn't untap during your untap step.")
        ruling(
            "2020-01-24",
            "Once you no longer control a Reflection, Alirios doesn't immediately become untapped. It'll be " +
                "untapped during your next untap step."
        )
    }
}
