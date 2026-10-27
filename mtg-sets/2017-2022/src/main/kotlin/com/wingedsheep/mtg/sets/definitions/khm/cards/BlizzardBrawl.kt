package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Blizzard Brawl
 * {G}
 * Snow Sorcery
 * Choose target creature you control and target creature you don't control. If you control three or
 * more snow permanents, the creature you control gets +1/+0 and gains indestructible until end of
 * turn. Then those creatures fight each other.
 *
 * The snow count is checked on resolution (ruling), so the buff is an [Effects.If] over
 * [Conditions.YouControlAtLeast]; the fight always follows (Duel for Dominance shape).
 */
val BlizzardBrawl = card("Blizzard Brawl") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Snow Sorcery"
    oracleText = "Choose target creature you control and target creature you don't control. " +
        "If you control three or more snow permanents, the creature you control gets +1/+0 and gains " +
        "indestructible until end of turn. Then those creatures fight each other. " +
        "(Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )

        effect = Effects.If(
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Permanent.snow()),
            then = Effects.ModifyStats(1, 0, mine) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, mine),
        ) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6318918f-34e4-4bbf-9816-8e88f21ae324.jpg?1783928218"

        ruling("2021-02-05", "You can't cast Blizzard Brawl unless you choose both a creature you control and one you don't control as targets.")
        ruling("2021-02-05", "Check whether you control three or more snow permanents as Blizzard Brawl is resolving to see if the target creature you control gets +1/+0 and gains indestructible. That creature will still fight even if it doesn't get those bonuses.")
        ruling("2021-02-05", "If either target is an illegal target as Blizzard Brawl tries to resolve, neither creature will fight and no damage will be dealt.")
        ruling("2021-02-05", "If the creature you control is still a legal target as Blizzard Brawl tries to resolve but the target creature you don't control isn't, the creature you control will still get the bonuses until end of turn.")
    }
}
