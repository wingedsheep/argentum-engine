package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Perigee Beckoner
 * {4}{B}
 * Creature — Horror
 * 4/5
 * When this creature enters, until end of turn, another target creature you control gets +2/+0
 * and gains "When this creature dies, return it to the battlefield tapped under its owner's control."
 * Warp {1}{B}
 */
val PerigeeBeckoner = card("Perigee Beckoner") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, until end of turn, another target creature you control " +
        "gets +2/+0 and gains \"When this creature dies, return it to the battlefield tapped under " +
        "its owner's control.\"\n" +
        "Warp {1}{B} (You may cast this card from your hand for its warp cost. Exile this creature " +
        "at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "another target creature you control",
            TargetCreature(filter = TargetFilter.OtherCreatureYouControl)
        )

        val diesReturnTapped = TriggeredAbility.create(
            trigger = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
            binding = TriggerBinding.SELF,
            effect = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                placement = ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD,
            ),
            descriptionOverride = "When this creature dies, return it to the battlefield tapped under its owner's control."
        )

        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(2, 0, creature),
                GrantTriggeredAbilityEffect(
                    ability = diesReturnTapped,
                    target = creature,
                    duration = Duration.EndOfTurn,
                ),
            )
        )
        description = "When this creature enters, until end of turn, another target creature you control " +
            "gets +2/+0 and gains \"When this creature dies, return it to the battlefield tapped under " +
            "its owner's control.\""
    }

    warp = "{1}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3666a08-d449-496f-969a-bf21d4afbd77.jpg?1752947009"
    }
}
