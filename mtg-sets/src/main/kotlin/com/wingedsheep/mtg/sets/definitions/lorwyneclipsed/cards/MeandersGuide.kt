package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val MeandersGuide = card("Meanders Guide") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Merfolk Scout"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, you may tap another untapped Merfolk you control. " +
        "When you do, return target creature card with mana value 3 or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.Attacks
        val merfolk = target(
            "merfolk",
            TargetObject(filter = TargetFilter.OtherCreatureYouControl.withSubtype("Merfolk").untapped())
        )
        effect = ReflexiveTriggerEffect(
            action = Effects.Tap(merfolk),
            optional = true,
            reflexiveEffect = MoveToZoneEffect(
                target = EffectTarget.ContextTarget(0),
                destination = Zone.BATTLEFIELD
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Julie Dillon"
        flavorText = "In the silence of the Dark Meanders, she thought only of those still lost."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c41a0ad-138e-4eef-8f7f-35017e3b086f.jpg?1767871713"
    }
}
