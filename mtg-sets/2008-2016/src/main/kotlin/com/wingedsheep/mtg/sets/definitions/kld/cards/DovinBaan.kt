package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dovin Baan — Kaladesh #179
 * {2}{W}{U} · Legendary Planeswalker — Dovin · Starting loyalty 3
 *
 * +1: Until your next turn, up to one target creature gets -3/-0 and its activated abilities
 *     can't be activated.
 * −1: You gain 2 life and draw a card.
 * −7: You get an emblem with "Your opponents can't untap more than two permanents during their
 *     untap steps."
 *
 * The +1 is Academic Probation's lock (a granted self-scoped [PreventActivatedAbilities]) beside a
 * -3/-0, both [Duration.UntilYourNextTurn]. The emblem holds Damping Field's [UntapLimitPerStep]
 * narrowed to `players = Player.EachOpponent`; the opponent picks which two permanents untap.
 */
val DovinBaan = card("Dovin Baan") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Dovin"
    startingLoyalty = 3
    oracleText = "+1: Until your next turn, up to one target creature gets -3/-0 and its activated " +
        "abilities can't be activated.\n" +
        "−1: You gain 2 life and draw a card.\n" +
        "−7: You get an emblem with \"Your opponents can't untap more than two permanents during " +
        "their untap steps.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-3, 0, creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn
            )
        description = "Until your next turn, up to one target creature gets -3/-0 and its activated " +
            "abilities can't be activated."
    }

    loyaltyAbility(-1) {
        effect = Effects.GainLife(2) then Effects.DrawCards(1)
        description = "You gain 2 life and draw a card."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                UntapLimitPerStep(GameObjectFilter.Permanent, max = 2, players = Player.EachOpponent)
            ),
            emblemDescription = "Your opponents can't untap more than two permanents during their untap steps."
        )
        description = "You get an emblem with \"Your opponents can't untap more than two permanents " +
            "during their untap steps.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2344e710-4570-41b6-af84-a3e7d784003a.jpg?1783937169"

        ruling("2016-09-20", "Dovin Baan's first ability can target any creature, not just one with an activated ability. It can also be activated targeting no creature at all.")
        ruling("2016-09-20", "Activated abilities contain a colon. They're generally written “[Cost]: [Effect].” Some keywords are activated abilities and will have colons in their reminder text.")
        ruling("2016-09-20", "An opponent affected by Dovin Baan's emblem chooses which two of their permanents to untap, not you.")
    }
}
