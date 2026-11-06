package com.wingedsheep.mtg.sets.definitions.akh.cards

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
 * Supernatural Stamina — Amonkhet #111 (canonical printing)
 * {B} · Instant
 *
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies, return it to
 * the battlefield tapped under its owner's control."
 *
 * Infuse with Vitality's shape with a +2/+0 pump in place of deathtouch: the granted self-bound
 * dies trigger moves the card graveyard → battlefield tapped under its owner's control.
 */
val SupernaturalStamina = card("Supernatural Stamina") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature " +
        "dies, return it to the battlefield tapped under its owner's control.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                        fromZone = Zone.GRAVEYARD
                    ),
                    descriptionOverride = "When this creature dies, return it to the battlefield " +
                        "tapped under its owner's control."
                ),
                target = t,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Christopher Moeller"
        flavorText = "\"All are not equal—neither in life nor after death. Bontu's favorites will " +
            "stand highest in eternity.\"\n—Senifet, vizier of Bontu"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cac5753-54e7-45a8-b486-4860533ce93c.jpg?1783936498"
    }
}
