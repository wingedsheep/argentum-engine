package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Magda, Brazen Outlaw
 * {1}{R}
 * Legendary Creature — Dwarf Berserker
 * 2/1
 * Other Dwarves you control get +1/+0.
 * Whenever a Dwarf you control becomes tapped, create a Treasure token.
 * Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that card
 * onto the battlefield, then shuffle.
 *
 * "Other Dwarves" / "a Dwarf you control" reach any Dwarf permanent, so both filters are over
 * [GameObjectFilter.Permanent] rather than `.Creature`. The tap trigger only fires on an actual
 * untapped -> tapped change (the engine's TapEvent), matching the ruling. The activated ability
 * has no timing restriction.
 */
val MagdaBrazenOutlaw = card("Magda, Brazen Outlaw") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Berserker"
    oracleText = "Other Dwarves you control get +1/+0.\n" +
        "Whenever a Dwarf you control becomes tapped, create a Treasure token.\n" +
        "Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that " +
        "card onto the battlefield, then shuffle."
    power = 2
    toughness = 1

    // Other Dwarves you control get +1/+0.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.DWARF).youControl(),
                excludeSelf = true
            )
        )
    }

    // Whenever a Dwarf you control becomes tapped, create a Treasure token.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.DWARF).youControl()).becomesTapped()
        effect = Effects.CreateTreasure(1)
    }

    // Sacrifice five Treasures: Search your library for an artifact or Dragon card, put that card
    // onto the battlefield, then shuffle.
    activatedAbility {
        cost = Costs.SacrificeMultiple(5, GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
            destination = SearchDestination.BATTLEFIELD
        )
        description = "Sacrifice five Treasures: Search your library for an artifact or Dragon card, " +
            "put it onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/079e6263-e54c-4899-a336-5315909b9322.jpg?1783928229"
        ruling(
            "2021-02-05",
            "Magda's triggered ability doesn't allow you to tap any Dwarves. You have to find some other way to tap them. Attacking is still a great way to go."
        )
        ruling(
            "2021-02-05",
            "For the triggered ability to trigger, a Dwarf you control has to actually change from untapped to tapped. If an effect attempts to tap a Dwarf you control while it is already tapped, the ability won't trigger."
        )
    }
}
