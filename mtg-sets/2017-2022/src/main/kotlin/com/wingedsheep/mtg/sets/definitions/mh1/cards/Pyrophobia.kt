package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pyrophobia
 * {1}{R}
 * Sorcery
 * Pyrophobia deals 3 damage to target creature. Cowards can't block this turn.
 *
 * "Cowards can't block" is a rule-modifying group effect ([Effects.CantBlockGroup]), so it binds
 * every Coward when blockers are declared — including ones that enter or become Cowards after the
 * spell resolves. If the creature target is illegal on resolution the whole spell fizzles and
 * Cowards can block as normal.
 */
val Pyrophobia = card("Pyrophobia") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Pyrophobia deals 3 damage to target creature. Cowards can't block this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, creature) then Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Coward"))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Izzy"
        flavorText = "\"I hate this! I hate fire! I hate you for taking me on this route!\"\n—Norin the Wary"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff02b3f1-d70f-4e44-bc1c-7fc90ec611dc.jpg?1783933106"
        ruling(
            "2019-06-14",
            "If the target creature is an illegal target by the time Pyrophobia tries to resolve, the spell doesn't resolve. Cowards can block if they're otherwise allowed to."
        )
        ruling(
            "2019-06-14",
            "Because it doesn't modify the characteristics of creatures, Pyrophobia can affect Cowards that enter the battlefield or creatures that become Cowards after the spell resolves."
        )
    }
}
