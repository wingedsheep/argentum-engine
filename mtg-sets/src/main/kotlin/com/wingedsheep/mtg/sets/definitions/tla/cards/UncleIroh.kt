package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Uncle Iroh
 * {1}{R/G}{R/G}
 * Legendary Creature — Human Noble Ally
 * 4/2
 * Firebending 1 (Whenever this creature attacks, add {R}. This mana lasts until end of combat.)
 * Lesson spells you cast cost {1} less to cast.
 */
val UncleIroh = card("Uncle Iroh") {
    manaCost = "{1}{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 4
    toughness = 2
    oracleText = "Firebending 1 (Whenever this creature attacks, add {R}. This mana lasts until end of combat.)\n" +
        "Lesson spells you cast cost {1} less to cast."

    firebending(1)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.LESSON)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Kieran Yanner"
        flavorText = "\"Hope is something you give yourself. That is the meaning of inner strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c88672c-0eba-4e93-a0ec-30bbb1bb7661.jpg?1764121831"
    }
}
