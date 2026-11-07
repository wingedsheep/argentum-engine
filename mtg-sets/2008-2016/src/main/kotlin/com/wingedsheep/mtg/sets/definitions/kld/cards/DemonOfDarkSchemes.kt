package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demon of Dark Schemes — Kaladesh #73
 * {3}{B}{B}{B} · Creature — Demon · 5/5
 *
 * Flying
 * When this creature enters, all other creatures get -2/-2 until end of turn.
 * Whenever another creature dies, you get {E} (an energy counter).
 * {2}{B}, Pay {E}{E}{E}{E}: Put target creature card from a graveyard onto the battlefield tapped
 * under your control.
 *
 * The -2/-2 set is locked in as the trigger resolves ([Effects.ForEachInGroup], Shefet Archfiend
 * shape), so creatures arriving later in the turn are untouched. The reanimation reads any graveyard
 * ([TargetFilter.CreatureInGraveyard] has no owner restriction) and enters under the activator's
 * control via the controller override.
 */
val DemonOfDarkSchemes = card("Demon of Dark Schemes") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, all other creatures get -2/-2 until end of turn.\n" +
        "Whenever another creature dies, you get {E} (an energy counter).\n" +
        "{2}{B}, Pay {E}{E}{E}{E}: Put target creature card from a graveyard onto the battlefield tapped " +
        "under your control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature, excludeSelf = true),
            Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.PayPlayerCounters(CounterType.ENERGY, 4))
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(t, underYourControl = true, tapped = true)
        description = "{2}{B}, Pay {E}{E}{E}{E}: Put target creature card from a graveyard onto the battlefield " +
            "tapped under your control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "73"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99c99736-5c9c-4d2f-946d-05606fcdd039.jpg?1783937212"
        ruling(
            "2016-09-20",
            "Demon of Dark Schemes's second triggered ability triggers whenever another creature dies for any " +
                "reason, not just due to the first triggered ability."
        )
        ruling(
            "2016-09-20",
            "If Demon of Dark Schemes dies at the same time as another creature, its second triggered ability triggers."
        )
        ruling(
            "2016-09-20",
            "The set of creatures affected by Demon of Dark Schemes's first triggered ability is determined as the " +
                "ability resolves. Creatures that enter the battlefield later in the turn and noncreature permanents " +
                "that become creatures later in the turn won't get -2/-2."
        )
    }
}
