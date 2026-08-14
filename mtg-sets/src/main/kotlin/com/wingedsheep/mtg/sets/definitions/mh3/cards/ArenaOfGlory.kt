package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Arena of Glory
 * Land
 *
 * This land enters tapped unless you control a Mountain.
 * {T}: Add {R}.
 * {R}, {T}, Exert this land: Add {R}{R}. If that mana is spent on a creature spell, it gains
 * haste until end of turn. (An exerted permanent won't untap during your next untap step.)
 *
 * The exert cost (CR 701.43a — [com.wingedsheep.sdk.scripting.AbilityCost.Exert], a new engine
 * primitive) isn't otherwise implemented in this codebase, so this is its first user: it's
 * always payable regardless of tapped/exerted state (701.43b), and the marker it sets clears
 * unconditionally at the controller's next untap step (2024-06-07 ruling) rather than only when
 * it actually prevents an untap, distinguishing it from a stun counter.
 *
 * The haste payoff needs no new vocabulary — [ManaSpellRider.GrantsKeywordWhenSpent] already
 * exists (Carnelian Orb of Dragonkind's "If that mana is spent on a Dragon creature spell, it
 * gains haste"), so the {R}{R} here just tags itself the same way, unrestricted to Dragons.
 */
val ArenaOfGlory = card("Arena of Glory") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped unless you control a Mountain.\n" +
        "{T}: Add {R}.\n" +
        "{R}, {T}, Exert this land: Add {R}{R}. If that mana is spent on a creature spell, it " +
        "gains haste until end of turn. (An exerted permanent won't untap during your next " +
        "untap step.)"

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Land.withSubtype("Mountain"),
            )
        )
    )

    // {T}: Add {R}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {R}, {T}, Exert this land: Add {R}{R}. If that mana is spent on a creature spell, it gains
    // haste until end of turn.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.Exert)
        effect = Effects.AddMana(
            Color.RED,
            2,
            riders = setOf(
                ManaSpellRider.GrantsKeywordWhenSpent(
                    keyword = Keyword.HASTE,
                    spellFilter = GameObjectFilter.Creature,
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd148edc-9e43-41aa-bb50-f912115d3e72.jpg?1783911241"

        ruling(
            "2024-06-07",
            "You must already control a Mountain as Arena of Glory enters the battlefield for " +
                "it to enter untapped. If it enters the battlefield at the same time as a " +
                "Mountain when you control no other Mountains, it will enter tapped."
        )
        ruling(
            "2024-06-07",
            "The mana generated with Arena of Glory's last ability can be spent on anything, " +
                "not just creature spells."
        )
        ruling(
            "2024-06-07",
            "If the mana generated with Arena of Glory's last ability is spent to pay any part " +
                "of a creature spell's cost, including an alternative or additional cost, that " +
                "creature spell (and the resulting creature) will gain haste until end of turn."
        )
        ruling(
            "2024-06-07",
            "If the mana is spent on two different creature spells, each of those spells (and " +
                "resulting creatures) will gain haste until end of turn."
        )
    }
}
