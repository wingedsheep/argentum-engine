package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Athreos, Shroud-Veiled — Theros Beyond Death #269
 * {4}{W}{B} · Legendary Enchantment Creature — God · 4/7
 *
 * Indestructible
 * As long as your devotion to white and black is less than seven, Athreos isn't a creature.
 * At the beginning of your end step, put a coin counter on another target creature.
 * Whenever a creature with a coin counter on it dies or is put into exile, return that card to the
 * battlefield under your control.
 *
 * The devotion clause is the two-colour God template (Klothys). The return is Kaya's Ghostform's
 * "dies or is put into exile" split into one trigger per destination, each moving the card *from
 * that zone* under your control — so a token (which ceased to exist) or a card that already moved
 * on stays where it is, and it comes back as a new object without the coin counter (rulings). The
 * subject filter `Creature.withCounter(COIN)` is answered from last-known information, so it sees
 * the counter and the creature type the permanent had as it left; `Triggers.a` includes Athreos
 * itself, should it ever get a coin counter (ruling).
 */
val AthreosShroudVeiled = card("Athreos, Shroud-Veiled") {
    manaCost = "{4}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 7
    oracleText = "Indestructible\n" +
        "As long as your devotion to white and black is less than seven, Athreos isn't a creature.\n" +
        "At the beginning of your end step, put a coin counter on another target creature.\n" +
        "Whenever a creature with a coin counter on it dies or is put into exile, return that card to the " +
        "battlefield under your control."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.WHITE, Color.BLACK),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.AddCounters(CounterType.COIN, 1, creature)
        description = "At the beginning of your end step, put a coin counter on another target creature."
    }

    val coinCreature = GameObjectFilter.Creature.withCounter(CounterType.COIN)
    val returnText = "Whenever a creature with a coin counter on it dies or is put into exile, return " +
        "that card to the battlefield under your control."

    triggeredAbility {
        trigger = Triggers.a(coinCreature).dies()
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        description = returnText
    }

    triggeredAbility {
        trigger = Triggers.a(coinCreature).leaves(to = Zone.EXILE)
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.EXILE,
        )
        description = returnText
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "269"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/871c19fc-7d99-4022-9e13-d679a9e3547e.jpg?1783931501"
        ruling("2020-01-24", "If a token with a coin counter on it dies or is exiled, Athreos's last ability triggers, but won't return it to the battlefield.")
        ruling("2020-01-24", "The creature returns to the battlefield without a coin counter on it.")
        ruling(
            "2020-01-24",
            "If an effect exiles a creature with a coin counter on it and immediately returns it to the battlefield, " +
                "Athreos's last ability triggers but will have no effect. If an effect exiles a creature with a coin " +
                "counter on it and would return it to the battlefield at a later time, Athreos's last ability will " +
                "return that card to the battlefield and it won't be returned later."
        )
        ruling(
            "2020-01-24",
            "If a creature with a coin counter on it is put into a graveyard or exile but leaves that zone before " +
                "Athreos's last ability resolves, that card stays in its new zone, even if that zone is also a " +
                "graveyard or exile. You don't return it to the battlefield."
        )
        ruling(
            "2020-01-24",
            "If Athreos and a creature with a coin counter on it are both put into graveyards and/or exiled at the " +
                "same time, the other creature will be returned to the battlefield."
        )
        ruling("2020-01-24", "If Athreos somehow gets a coin counter on itself, its last ability will return it when it dies or is exiled.")
        ruling(
            "2020-01-24",
            "If Athreos leaves the battlefield, creatures with coin counters on them keep them. The counters won't " +
                "have any meaning or effect unless another Athreos is on the battlefield later."
        )
        ruling(
            "2020-01-24",
            "If a creature with a coin counter on it stops being a creature, it keeps its coin counter, but " +
                "Athreos's last ability won't trigger when that permanent dies or is exiled unless it's a creature " +
                "again by that time."
        )
        ruling(
            "2020-01-24",
            "If a creature you don't own has a coin counter on it, that permanent will return to the battlefield " +
                "under your control when it dies or is exiled while you control Athreos."
        )
    }
}
