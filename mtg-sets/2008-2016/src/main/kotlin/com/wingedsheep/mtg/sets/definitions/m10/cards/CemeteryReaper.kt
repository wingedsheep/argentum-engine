package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cemetery Reaper
 * {1}{B}{B}
 * Creature — Zombie
 * 2/2
 * Other Zombie creatures you control get +1/+1.
 * {2}{B}, {T}: Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token.
 *
 * The exile is part of the effect, not the cost: if the card has left the graveyard by
 * resolution, the ability's only target is illegal and no Zombie is created (2009 ruling).
 */
val CemeteryReaper = card("Cemetery Reaper") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Other Zombie creatures you control get +1/+1.\n" +
        "{2}{B}, {T}: Exile target creature card from a graveyard. Create a 2/2 black Zombie creature token."
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl(),
                excludeSelf = true,
            ),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(t, Zone.EXILE) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Dave Allsop"
        flavorText = "Every grave cradles a recruit."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/639b48f0-3426-46cf-b857-4611f7de4826.jpg?1783942385"
        ruling(
            "2009-10-01",
            "The creature card in a graveyard is exiled as part of the activated ability's effect, not as a cost. If that card has left the graveyard by the time the ability would resolve, the ability doesn't resolve and you don't get a Zombie token."
        )
    }
}
