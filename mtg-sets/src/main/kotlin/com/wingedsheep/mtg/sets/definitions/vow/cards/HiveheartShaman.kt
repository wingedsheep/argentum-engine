package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hiveheart Shaman
 * {3}{G}
 * Creature — Human Shaman
 * 3/5
 *
 * Whenever this creature attacks, you may search your library for a basic land card that
 * doesn't share a land type with a land you control, put that card onto the battlefield,
 * then shuffle.
 * {5}{G}: Create a 1/1 green Insect creature token. Put X +1/+1 counters on it, where X is
 * the number of basic land types among lands you control. Activate only as a sorcery.
 *
 * The attack trigger's search filter is `GameObjectFilter.BasicLand` composed with the new
 * `notSharingLandTypeWithPermanentYouControl(GameObjectFilter.Land)` predicate (sibling of
 * Radagast the Brown's creature-type predicate, but comparing land subtypes) — a bare
 * `GameObjectFilter.BasicLand` would wrongly let the search find a land type already
 * controlled. The activated ability's counter count uses `DynamicAmounts.domain(Player.You)`
 * (= number of *distinct* basic land types among lands you control, capped at 5), not a raw
 * land count — `AggregateBattlefield(You, Land)` would count lands, not distinct types.
 */
val HiveheartShaman = card("Hiveheart Shaman") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever this creature attacks, you may search your library for a basic land " +
        "card that doesn't share a land type with a land you control, put that card onto the " +
        "battlefield, then shuffle.\n" +
        "{5}{G}: Create a 1/1 green Insect creature token. Put X +1/+1 counters on it, where X " +
        "is the number of basic land types among lands you control. Activate only as a sorcery."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.notSharingLandTypeWithPermanentYouControl(GameObjectFilter.Land),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        effect = Effects.Composite(
            Effects.CreateToken(power = 1, toughness = 1, colors = setOf(Color.GREEN), creatureTypes = setOf("Insect")),
            Effects.AddDynamicCounters(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.domain(),
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33efdb5a-7667-4475-9905-95f8fc9be2d3.jpg?1783924812"
    }
}
