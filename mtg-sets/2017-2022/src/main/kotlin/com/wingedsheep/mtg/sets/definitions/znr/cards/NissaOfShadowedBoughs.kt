package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa of Shadowed Boughs — Zendikar Rising #231 (canonical printing)
 * {2}{B}{G} · Legendary Planeswalker — Nissa · Loyalty 4
 *
 * Landfall — Whenever a land you control enters, put a loyalty counter on Nissa.
 * +1: Untap target land you control. You may have it become a 3/3 Elemental creature with haste
 *     and menace until end of turn. It's still a land.
 * −5: You may put a creature card with mana value less than or equal to the number of lands you
 *     control onto the battlefield from your hand or graveyard with two +1/+1 counters on it.
 *
 * Landfall is the plain `Triggers.a(Land.youControl()).enters()` (Canopy Baloth) feeding a loyalty
 * counter onto Nissa herself (Ral, Crackling Wit's shape).
 *
 * The +1 untaps the target, then offers the animation as an [Effects.May] over the same target
 * (Academic Dispute / Llanowar Loamspeaker). [Effects.BecomeCreature] keeps the land's types and
 * abilities, so "It's still a land" holds.
 *
 * The −5 is Kastral's hand-or-graveyard pool with Vision Quest's dynamic mana-value cap:
 * `manaValueAtMostDynamic(landsYouControl)` is read on resolution, `chooseUpTo(1)` is the "you
 * may", and the two +1/+1 counters follow the move onto exactly the card that arrived
 * (`moveTracked`) — the collection move can stamp only a single counter on entry.
 */
val NissaOfShadowedBoughs = card("Nissa of Shadowed Boughs") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 4
    oracleText = "Landfall — Whenever a land you control enters, put a loyalty counter on Nissa.\n" +
        "+1: Untap target land you control. You may have it become a 3/3 Elemental creature with " +
        "haste and menace until end of turn. It's still a land.\n" +
        "−5: You may put a creature card with mana value less than or equal to the number of " +
        "lands you control onto the battlefield from your hand or graveyard with two +1/+1 " +
        "counters on it."

    // Landfall — Whenever a land you control enters, put a loyalty counter on Nissa.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self)
    }

    // +1: Untap target land you control. You may have it become a 3/3 Elemental creature with
    // haste and menace until end of turn. It's still a land.
    loyaltyAbility(+1) {
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.Untap(land) then
            Effects.May(
                effect = Effects.BecomeCreature(
                    target = land,
                    power = 3,
                    toughness = 3,
                    keywords = setOf(Keyword.HASTE, Keyword.MENACE),
                    creatureTypes = setOf("Elemental"),
                    duration = Duration.EndOfTurn,
                ),
                descriptionOverride = "You may have it become a 3/3 Elemental creature with haste " +
                    "and menace until end of turn. It's still a land."
            )
    }

    // −5: You may put a creature card with mana value ≤ the number of lands you control onto the
    // battlefield from your hand or graveyard with two +1/+1 counters on it.
    loyaltyAbility(-5) {
        effect = Effects.Pipeline {
            val nissaCandidates = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                        .manaValueAtMostDynamic(DynamicAmounts.landsYouControl())
                )
            )
            val nissaChosen = chooseUpTo(
                1,
                from = nissaCandidates,
                prompt = "You may put a creature card with mana value less than or equal to the " +
                    "number of lands you control onto the battlefield"
            )
            val nissaArrived = moveTracked(nissaChosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.AddCountersToCollection(nissaArrived, CounterType.PLUS_ONE_PLUS_ONE, 2))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d69f943-d474-47c0-bc6b-1b247a5dd6f3.jpg?1783929321"

        ruling("2020-09-25", "Nissa's middle ability can target a land that's already untapped or that's already a creature.")
        ruling("2020-09-25", "Nissa's middle ability doesn't remove any abilities the land creature has.")
        ruling("2020-09-25", "Nissa's middle ability overwrites any previous effects that set the land creature's power and/or toughness to specific values. Other effects that set these characteristics to specific values that start to apply after the ability resolves will overwrite that part of the effect.")
        ruling("2020-09-25", "The creature card's mana value is checked only in your hand or graveyard. You may put a creature card onto the battlefield whose mana value becomes greater than the number of lands you control once it's on the battlefield, most likely because it enters as a copy of another permanent.")
        ruling("2020-09-25", "If a card in a player's hand or graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
