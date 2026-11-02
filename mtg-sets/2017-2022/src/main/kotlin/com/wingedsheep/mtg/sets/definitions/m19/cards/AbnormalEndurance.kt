package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abnormal Endurance
 * {1}{B}
 * Instant
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies, return it to
 * the battlefield tapped under its owner's control."
 *
 * Same shape as Perigee Beckoner's grant: +2/+0 and an end-of-turn self-bound dies trigger whose
 * graveyard → battlefield move (owner's control by default) enters tapped.
 */
val AbnormalEndurance = card("Abnormal Endurance") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature " +
        "dies, return it to the battlefield tapped under its owner's control.\""

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature, Duration.EndOfTurn) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                        fromZone = Zone.GRAVEYARD,
                    ),
                    descriptionOverride = "When this creature dies, return it to the battlefield " +
                        "tapped under its owner's control."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Tomasz Jedruszek"
        flavorText = "Tenacity prevails when hopes die."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de2de2bd-9ba7-4b6f-94c2-dafb2011a48e.jpg?1783934576"

        ruling(
            "2018-07-13",
            "Abnormal Endurance's effect works only once. If the targeted creature dies and is then " +
                "returned to the battlefield, it's considered to be a new creature. If that new " +
                "creature dies, it won't come back."
        )
    }
}
