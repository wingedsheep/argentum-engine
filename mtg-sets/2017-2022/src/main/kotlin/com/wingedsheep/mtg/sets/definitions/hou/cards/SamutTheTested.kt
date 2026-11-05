package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Samut, the Tested — Hour of Devastation #144
 * {2}{R}{G} · Legendary Planeswalker — Samut · Loyalty 4
 *
 * +1: Up to one target creature gains double strike until end of turn.
 * −2: Samut deals 2 damage divided as you choose among one or two targets.
 * −7: Search your library for up to two creature and/or planeswalker cards, put them onto the
 *     battlefield, then shuffle.
 *
 * +1 is an optional creature target (Oko's shape) with an end-of-turn double-strike grant. −2 is
 * Electrolyze's any-target division, chosen as the ability is activated (ruling). −7 is the
 * library search to the battlefield with an up-to-two pick over creature-or-planeswalker cards;
 * the found cards move together, so they enter simultaneously (ruling).
 */
val SamutTheTested = card("Samut, the Tested") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Samut"
    startingLoyalty = 4
    oracleText = "+1: Up to one target creature gains double strike until end of turn.\n" +
        "−2: Samut deals 2 damage divided as you choose among one or two targets.\n" +
        "−7: Search your library for up to two creature and/or planeswalker cards, put them onto the " +
        "battlefield, then shuffle."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
        description = "Up to one target creature gains double strike until end of turn."
    }

    loyaltyAbility(-2) {
        target = AnyTarget(count = 2, minCount = 1)
        effect = Effects.DividedDamage(total = 2, minTargets = 1, maxTargets = 2)
        description = "Samut deals 2 damage divided as you choose among one or two targets."
    }

    loyaltyAbility(-7) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
        description = "Search your library for up to two creature and/or planeswalker cards, put them onto " +
            "the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "144"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09f06f55-7918-46c4-80ff-0bf39e091a4a.jpg?1783936008"
        ruling(
            "2017-07-14",
            "You divide the damage as you activate Samut's second ability, not as it resolves. Each target must " +
                "be assigned at least 1 damage. In other words, as you activate the ability, you choose whether " +
                "to have it deal 2 damage to a single target, or deal 1 damage to each of two targets."
        )
        ruling(
            "2017-07-14",
            "If Samut's second ability has two targets and one becomes an illegal target, the remaining target " +
                "is dealt 1 damage, not 2."
        )
        ruling(
            "2017-07-14",
            "While resolving Samut's last ability, all of the creatures and planeswalkers put onto the " +
                "battlefield this way enter at the same time. If any have triggered abilities that trigger on " +
                "something else entering the battlefield, they'll see each other."
        )
    }
}
