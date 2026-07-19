package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Embereth Veteran
 * {R}
 * Creature — Human Knight (2/1)
 * {1}, Sacrifice this creature: Create a Young Hero Role token attached to another target creature.
 * (If you control another Role on it, put that one into the graveyard. Enchanted creature has
 * "Whenever this creature attacks, if its toughness is 3 or less, put a +1/+1 counter on it.")
 *
 * The Young Hero Role token carries the granted attack trigger; this card just pays {1} + sacrifices
 * itself to create it on another target creature (any controller, so it can buff an ally or bait a
 * gift onto an opponent's small creature).
 */
val EmberethVeteran = card("Embereth Veteran") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 1
    oracleText = "{1}, Sacrifice this creature: Create a Young Hero Role token attached to another " +
        "target creature. (If you control another Role on it, put that one into the graveyard. " +
        "Enchanted creature has \"Whenever this creature attacks, if its toughness is 3 or less, " +
        "put a +1/+1 counter on it.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val t = target("target", TargetCreature(filter = TargetFilter.OtherCreature))
        effect = Effects.CreateRoleToken("Young Hero Role", t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Andreia Ugrai"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc7130b8-3168-421f-912a-46ed5b769807.jpg?1783915096"
    }
}
