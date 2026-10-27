package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Warchanter Skald
 * {2}{W}
 * Creature — Dwarf Cleric
 * 2/3
 * Whenever this creature becomes tapped, if it's enchanted or equipped, create a 2/1 red Dwarf
 * Berserker creature token.
 *
 * The "if" is an intervening-if (checked when the trigger would fire and again on resolution);
 * "enchanted or equipped" is the Aura/Equipment-agnostic IsEnchanted / IsEquipped pair, so the
 * attachment needn't be controlled by Warchanter Skald's controller.
 */
val WarchanterSkald = card("Warchanter Skald") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Cleric"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature becomes tapped, if it's enchanted or equipped, create a 2/1 red Dwarf Berserker creature token."

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        interveningIf = Conditions.SourceMatches(
            GameObjectFilter.Any.withStatePredicate(
                StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped))
            )
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf", "Berserker")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "381"
        artist = "Daniel Zrom"
        flavorText = "\"Who will be the hero of my tale?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f79c5e95-f6c6-4e2f-9c6c-d99addb757ac.jpg?1783928122"
    }
}
