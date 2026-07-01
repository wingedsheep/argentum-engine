package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Darkness Crystal
 * {2}{B}{B}
 * Legendary Artifact
 *
 * Black spells you cast cost {1} less to cast.
 * If a nontoken creature an opponent controls would die, instead exile it and you gain 2 life.
 * {4}{B}{B}, {T}: Put target creature card exiled with The Darkness Crystal onto the battlefield
 * tapped under your control with two additional +1/+1 counters on it.
 */
val TheDarknessCrystal = card("The Darkness Crystal") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact"
    oracleText = "Black spells you cast cost {1} less to cast.\n" +
        "If a nontoken creature an opponent controls would die, instead exile it and you gain 2 life.\n" +
        "{4}{B}{B}, {T}: Put target creature card exiled with The Darkness Crystal onto the " +
        "battlefield tapped under your control with two additional +1/+1 counters on it."

    // Ability 1: Black spells you cast cost {1} less to cast. The ruling clarifies this reduces
    // only generic mana, so a fixed generic reduction is correct.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLACK)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    // Ability 2: If a nontoken creature an opponent controls would die, instead exile it (linked to
    // this permanent so ability 3 can retrieve it) and you gain 2 life. The rider gains 2 life per
    // creature redirected, matching "you'll gain 2 life for each of them".
    replacementEffect(
        RedirectZoneChangeWithEffect(
            newDestination = Zone.EXILE,
            additionalEffect = GainLifeEffect(2),
            selfOnly = false,
            linkToSource = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    // Ability 3: {4}{B}{B}, {T}: Put target creature card exiled with The Darkness Crystal onto the
    // battlefield tapped under your control with two additional +1/+1 counters on it.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{B}{B}"), Costs.Tap)
        val t = target(
            "target creature card exiled with The Darkness Crystal",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.exiledWithSource(),
                    zone = Zone.EXILE,
                ),
            ),
        )
        effect = Effects.Move(
            target = t,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.Tapped,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.EXILE,
        ).then(AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 2, t))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Pablo Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f93b6ac-54ce-45d0-8549-19307406e6e5.jpg?1782686526"
        ruling("2025-06-06", "The cost reduction applies only to generic mana in the total cost of black spells you cast.")
        ruling("2025-06-06", "If a creature token an opponent controls dies, it goes to that player's graveyard as normal before ceasing to exist.")
        ruling("2025-06-06", "While you control The Darkness Crystal, abilities that trigger whenever a nontoken creature an opponent controls dies won't trigger because that card is exiled instead.")
        ruling("2025-06-06", "If The Darkness Crystal leaves the battlefield at the same time as one or more nontoken creatures an opponent controls would die, those creature cards will be exiled and you'll gain 2 life for each of them.")
    }
}
