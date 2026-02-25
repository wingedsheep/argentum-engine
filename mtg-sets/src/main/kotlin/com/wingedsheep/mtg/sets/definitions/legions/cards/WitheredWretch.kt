package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Withered Wretch
 * {B}{B}
 * Creature — Zombie Cleric
 * 2/2
 * {1}: Exile target card from a graveyard.
 */
val WitheredWretch = card("Withered Wretch") {
    manaCost = "{B}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 2
    toughness = 2
    oracleText = "{1}: Exile target card from a graveyard."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{1}"))
        val t = target("target card in a graveyard", Targets.CardInGraveyard)
        effect = MoveToZoneEffect(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Tim Hildebrandt"
        flavorText = "Once it consecrated the dead. Now it desecrates them."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8a82948-503f-4ad4-9e3c-c080c16afd63.jpg?1562932159"
    }
}
