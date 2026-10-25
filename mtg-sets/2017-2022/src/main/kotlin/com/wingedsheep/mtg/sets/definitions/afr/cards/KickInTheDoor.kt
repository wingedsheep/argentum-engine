package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kick in the Door
 * {R}
 * Sorcery
 *
 * Put a +1/+1 counter on target creature. That creature gains haste until end of turn and can't
 * be blocked by Walls this turn. Venture into the dungeon. (Enter the first room or advance to the
 * next room.)
 *
 * The Walls clause is Tower of Coireall's grant: `CantBeBlockedBy` handed out for the turn through
 * `Effects.GrantStaticAbility`, filtered by the Wall subtype rather than the defender keyword. If
 * the target is gone on resolution the spell doesn't resolve and there is no venture.
 */
val KickInTheDoor = card("Kick in the Door") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature. That creature gains haste until end of turn " +
        "and can't be blocked by Walls this turn. Venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.GrantStaticAbility(
                ability = CantBeBlockedBy(GameObjectFilter.Creature.withSubtype(Subtype.WALL)),
                target = creature,
                duration = Duration.EndOfTurn,
            ) then
            Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Justine Cruz"
        flavorText = "\"We tried your way. Now we do it my way!\"\n—Jym, barbarian of Baldur's Gate"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/222102d0-fed3-41fd-87b0-f6d22766d7fd.jpg?1783926475"
    }
}
