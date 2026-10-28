package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopiedAbilities
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Marvin, Murderous Mimic
 * {2}
 * Legendary Artifact Creature — Toy
 * 2/2
 *
 * Marvin has all activated abilities of creatures you control that don't have the same name as
 * this creature.
 *
 * Modeled with [GainActivatedAbilitiesOfPermanents]: `grantedTo = GroupFilter.source()` (Marvin
 * himself gains the abilities), `sourceFilter = creatures you control not named "Marvin,
 * Murderous Mimic"`, and `abilities = CopiedAbilities.ALL` because the oracle text says *all*
 * activated abilities (no "except mana abilities" clause, unlike Sharkey). The engine recomputes
 * the gained set continuously from projected state, so the abilities track your other creatures
 * entering/leaving and copies/tokens named Marvin are correctly excluded by the name predicate.
 */
val MarvinMurderousMimic = card("Marvin, Murderous Mimic") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact Creature — Toy"
    power = 2
    toughness = 2
    oracleText = "Marvin has all activated abilities of creatures you control that don't have the " +
        "same name as this creature."

    staticAbility {
        ability = GainActivatedAbilitiesOfPermanents(
            grantedTo = GroupFilter.source(),
            sourceFilter = GameObjectFilter.Creature.youControl().notNamed("Marvin, Murderous Mimic"),
            abilities = CopiedAbilities.ALL
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Mirko Failoni"
        flavorText = "In a moment of bloodthirsty whimsy, a razorkin once picked up a " +
            "ventriloquist's puppet and used it to terrorize a survivor. Now it hunts on its own, " +
            "with no one pulling its strings."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66898970-b99b-48f2-9240-68c301c95500.jpg?1726286819"
    }
}
