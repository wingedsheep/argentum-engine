package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Thunderkin Awakener — Core Set 2020 #162 (canonical printing)
 * {1}{R} · Creature — Elemental Shaman · 1/2
 *
 * Haste
 * Whenever this creature attacks, choose target Elemental creature card in your graveyard with
 * toughness less than this creature's toughness. Return that card to the battlefield tapped and
 * attacking. Sacrifice it at the beginning of the next end step.
 *
 * The target filter compares against this creature's toughness ([DynamicAmounts.sourceToughness],
 * projected, last-known if it has left), checked as the trigger targets and again on resolution.
 * The returned card is a bare "tapped and attacking" (CR 508.4), so its controller picks what it
 * attacks. The end-step sacrifice carries the moved card as a collection: its identity is captured
 * after it enters, so a creature that leaves the battlefield before then is a new object and is
 * left alone (CR 603.7c), and "sacrifice it" only touches it while you still control it.
 */
val ThunderkinAwakener = card("Thunderkin Awakener") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Shaman"
    power = 1
    toughness = 2
    oracleText = "Haste\n" +
        "Whenever this creature attacks, choose target Elemental creature card in your graveyard with " +
        "toughness less than this creature's toughness. Return that card to the battlefield tapped and " +
        "attacking. Sacrifice it at the beginning of the next end step."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(
            TargetFilter.CreatureInYourGraveyard
                .withSubtype(Subtype.ELEMENTAL)
                .compareNumericProperty(
                    CardNumericProperty.TOUGHNESS,
                    ComparisonOperator.LT,
                    DynamicAmounts.sourceToughness(),
                )
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.TappedAndAttacking),
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            )
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.SacrificeTarget(chosen.asTarget),
                carryCollections = listOf(chosen.key),
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/289b649b-c93a-44ad-bdd6-866f8b2f1bc2.jpg?1783932968"
        ruling("2019-07-12", "The target card must have lesser toughness both as Thunderkin Awakener attacks and as its triggered ability resolves. Other abilities that trigger whenever Thunderkin Awakener attacks can’t raise its toughness in time to allow the ability to target cards with greater toughness.")
        ruling("2019-07-12", "If Thunderkin Awakener leaves the battlefield after it has attacked but before its triggered ability resolves, use its toughness as it last existed on the battlefield to determine whether the card is still a legal target.")
        ruling("2019-07-12", "You choose which player or planeswalker the returned creature is attacking. It doesn’t have to be attacking the same player or planeswalker that Thunderkin Awakener is attacking.")
        ruling("2019-07-12", "Although the returned creature is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks (such as that of Thunderkin Awakener itself) won’t trigger when it enters the battlefield attacking.")
        ruling("2019-07-12", "If the new creature leaves the battlefield before the end step, that card remains in its current zone. It won’t be sacrificed.")
    }
}
