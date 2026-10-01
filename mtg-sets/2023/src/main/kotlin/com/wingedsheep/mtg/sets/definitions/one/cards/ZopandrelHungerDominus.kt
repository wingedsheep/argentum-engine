package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zopandrel, Hunger Dominus
 * {5}{G}{G}
 * Legendary Creature — Phyrexian Horror
 * 4/6
 *
 * Reach
 * At the beginning of each combat, double the power and toughness of each creature you control
 * until end of turn.
 * {G/P}{G/P}, Sacrifice two other creatures: Put an indestructible counter on Zopandrel.
 *
 * The combat trigger is Unnatural Growth's shape ([Patterns.Group.doublePowerAndToughnessForAll]);
 * "two other creatures" is `SacrificeMultiple` over a `notSourceItself()` creature filter
 * (Elesh Norn's spelling), so Zopandrel can never be one of the two.
 */
val ZopandrelHungerDominus = card("Zopandrel, Hunger Dominus") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Phyrexian Horror"
    power = 4
    toughness = 6
    oracleText = "Reach\n" +
        "At the beginning of each combat, double the power and toughness of each creature you control " +
        "until end of turn.\n" +
        "{G/P}{G/P}, Sacrifice two other creatures: Put an indestructible counter on Zopandrel. " +
        "({G/P} can be paid with either {G} or 2 life.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        effect = Patterns.Group.doublePowerAndToughnessForAll(Filters.Group.creaturesYouControl)
        description = "At the beginning of each combat, double the power and toughness of each " +
            "creature you control until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G/P}{G/P}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Creature.notSourceItself())
        )
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "{G/P}{G/P}, Sacrifice two other creatures: Put an indestructible counter on Zopandrel."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "195"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb419d9d-e06f-48c8-a4f8-a57f9be39e50.jpg?1783918004"
        ruling("2023-02-04", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. Similarly, a creature whose toughness is doubled gets +0/+X, where X is its toughness as the effect begins to apply.")
    }
}
