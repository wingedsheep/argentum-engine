package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Bond of Passion
 * {4}{R}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end of
 * turn. Bond of Passion deals 2 damage to any other target.
 */
val BondOfPassion = card("Bond of Passion") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn. Bond of Passion deals 2 damage to any other target."

    spell {
        val creature = target(TargetFilter.Creature)
        val other = target(TargetOther(Targets.Any))
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.DealDamage(2, other)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Izzy"
        flavorText = "\"Why don't we fight for what's right and let bureaucrats argue over details?\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae644e9d-9bc4-4ccd-aef4-3d0559948aa6.jpg?1783933433"
        ruling(
            "2019-05-03",
            "If either target becomes illegal after Bond of Passion is cast but before it resolves, the other " +
                "is still affected as appropriate."
        )
        ruling(
            "2019-05-03",
            "In some rare cases, gaining control of the target creature may cause the second target to become " +
                "illegal. As long as the second target is a legal target as Bond of Passion begins to resolve " +
                "(before the target creature changes control) it will attempt to deal damage to the second target."
        )
    }
}
