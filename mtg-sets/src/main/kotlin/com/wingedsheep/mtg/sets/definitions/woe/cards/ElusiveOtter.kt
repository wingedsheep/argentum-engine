package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Elusive Otter // Grove's Bounty
 * {U}
 * Creature — Otter
 * 1/1
 *
 * Prowess
 * Creatures with power less than this creature's power can't block it.
 *
 * Adventure: Grove's Bounty — {X}{G}, Sorcery — Adventure
 * Distribute X +1/+1 counters among any number of target creatures you control.
 *
 * The evasion clause reuses the attacker-side static [CantBeBlockedByCreaturesWithLessPower]
 * (Formation Breaker), which compares *projected* power on both sides — so the prowess bonus
 * raises the blocking threshold in the same turn it applies.
 *
 * Grove's Bounty is the first X-scaled distribute. `unlimited = true` gives "any number of
 * target creatures"; `dynamicMaxCount = XValue` enforces CR 601.2d — each target must be dealt
 * at least one counter, so you can never declare more targets than X.
 */
val ElusiveOtter = card("Elusive Otter") {
    manaCost = "{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Otter"
    power = 1
    toughness = 1
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Creatures with power less than this creature's power can't block it."

    keywords(Keyword.PROWESS)

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    adventure("Grove's Bounty") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Distribute X +1/+1 counters among any number of target creatures you control. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target(
                "any number of target creatures you control",
                TargetObject(
                    filter = TargetFilter.CreatureYouControl,
                    unlimited = true,
                    dynamicMaxCount = DynamicAmount.XValue,
                ),
            )
            effect = Effects.DistributeCountersAmongTargets(DynamicAmount.XValue)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc9bdf96-3e3b-4dca-aae2-e81d4cbeafe8.jpg?1783915065"
    }
}
