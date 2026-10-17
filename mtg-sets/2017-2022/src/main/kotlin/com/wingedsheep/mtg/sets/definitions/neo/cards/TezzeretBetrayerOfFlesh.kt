package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tezzeret, Betrayer of Flesh — Kamigawa: Neon Dynasty #84
 * {2}{U}{U} · Legendary Planeswalker — Tezzeret · Starting loyalty 4
 *
 * The first activated ability of an artifact you activate each turn costs {2} less to activate.
 * +1: Draw two cards. Then discard two cards unless you discard an artifact card.
 * −2: Target artifact becomes an artifact creature. If it isn't a Vehicle, it has base power and
 *     toughness 4/4.
 * −6: You get an emblem with "Whenever an artifact you control becomes tapped, draw a card."
 *
 * Modeling notes:
 *
 *  - **The static** is [ReduceActivatedAbilityCost] with `firstEachTurn`: only the controller's
 *    first activation this turn of an ability of an artifact permanent is reduced, and every such
 *    activation — mana abilities, `{T}` abilities, crew, even ones made before Tezzeret arrived —
 *    uses the turn's "first" up (both per the printed rulings).
 *  - **The +1** is Thirst for Knowledge's text: one discard selection where a single artifact card
 *    satisfies it, otherwise two cards.
 *  - **The −2 has no duration** — the type change and the 4/4 base P/T are permanent. Whether the
 *    target is a Vehicle is checked as the ability resolves: a Vehicle only gains the creature type
 *    and keeps its printed power and toughness (ruling); anything else becomes a 4/4 artifact
 *    creature. Becoming a creature this way isn't crewing (ruling) — it adds the type directly.
 *  - **The −6 emblem** is a permanent sourceless triggered ability; "an artifact you control"
 *    reads on the emblem's controller, and it is per-permanent, so tapping three artifacts at once
 *    draws three cards.
 */
val TezzeretBetrayerOfFlesh = card("Tezzeret, Betrayer of Flesh") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 4
    oracleText = "The first activated ability of an artifact you activate each turn costs {2} less to activate.\n" +
        "+1: Draw two cards. Then discard two cards unless you discard an artifact card.\n" +
        "−2: Target artifact becomes an artifact creature. If it isn't a Vehicle, it has base power and toughness 4/4.\n" +
        "−6: You get an emblem with \"Whenever an artifact you control becomes tapped, draw a card.\""

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Artifact),
            amount = DynamicAmounts.fixed(2),
            firstEachTurn = true
        )
    }

    // +1: Draw two cards. Then discard two cards unless you discard an artifact card.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(2) then Effects.DiscardUnlessMatching(2, GameObjectFilter.Artifact)
        description = "Draw two cards. Then discard two cards unless you discard an artifact card."
    }

    // −2: Target artifact becomes an artifact creature. If it isn't a Vehicle, it has base power
    //     and toughness 4/4.
    loyaltyAbility(-2) {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE),
                artifact
            ),
            then = Effects.AddCardType("CREATURE", artifact, Duration.Permanent),
            otherwise = Effects.BecomeCreature(
                target = artifact,
                power = 4,
                toughness = 4,
                duration = Duration.Permanent
            )
        )
        description = "Target artifact becomes an artifact creature. If it isn't a Vehicle, it has " +
            "base power and toughness 4/4."
    }

    // −6: You get an emblem with "Whenever an artifact you control becomes tapped, draw a card."
    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).becomesTapped()
                effect = Effects.DrawCards(1)
                description = "Whenever an artifact you control becomes tapped, draw a card."
            },
            descriptionOverride = "Whenever an artifact you control becomes tapped, draw a card."
        )
        description = "You get an emblem with \"Whenever an artifact you control becomes tapped, " +
            "draw a card.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "84"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2174d16e-4a78-4d27-b220-1086c40db83e.jpg?1783923892"

        ruling("2022-02-18", "Tezzeret, Betrayer of Flesh's first ability reduces the cost of the first activated ability of an artifact that you begin to activate each turn. The second ability (and any subsequent abilities) of an artifact you activate that turn won't cost {2} less to activate, even if the first ability you activated didn't cost any mana to begin with. Notably, this does not exclude mana abilities.")
        ruling("2022-02-18", "If you begin to activate an ability that costs mana before activating any mana abilities you need to pay for it, that ability will be the one to get Tezzeret's discount. The total cost for that ability becomes locked in, and then you can activate mana abilities to pay for it.")
        ruling("2022-02-18", "If you activate Tezzeret's second loyalty ability targeting a Vehicle, it will have its printed power and toughness.")
        ruling("2022-02-18", "If Tezzeret's second loyalty ability causes a Vehicle to become an artifact creature, it doesn't count as \"crewing\" that Vehicle for any ability that would trigger off of a Vehicle becoming crewed.")
    }
}
