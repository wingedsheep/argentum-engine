package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fíli the Pathfinder
 * {3}{W}
 * Legendary Creature — Dwarf Scout
 * 2/2
 *
 * Storied.
 * As long as you have an enduring story, creatures you control get +1/+1.
 * Whenever Fíli or another nontoken Dwarf you control enters, create a 2/2 red Dwarf creature token.
 *
 * The anthem says "creatures you control", not "other creatures", so the filter carries no
 * `excludeSelf` — Fíli pumps himself to 3/3 once the enduring story is on.
 *
 * "Fíli or another nontoken Dwarf you control" is the Arahbo shape: one `ANY`-bound enters trigger over
 * `Dwarf.nontoken().youControl()` covers both halves, because Fíli is himself a nontoken Dwarf you
 * control and so matches his own filter. The tokens it makes are Dwarves too, but `nontoken()` keeps
 * them from re-triggering — otherwise the ability would loop.
 */
val FiliThePathfinder = card("Fíli the Pathfinder") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Scout"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, creatures you control get +1/+1.\n" +
        "Whenever Fíli or another nontoken Dwarf you control enters, create a 2/2 red Dwarf " +
        "creature token."
    power = 2
    toughness = 2

    storied()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 1,
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            ),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DWARF).nontoken().youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf"),
            imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcb3a3f-c0d4-43d4-8549-826a38bfa27d.jpg?1785497537",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b02142f3-5e55-40dc-a02c-9113fb7d763c.jpg?1785496367"
    }
}
