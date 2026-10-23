package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
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
 * Infuse with Vitality — Strixhaven: School of Mages #194 (canonical printing)
 * {B}{G} · Instant
 *
 * Until end of turn, target creature gains deathtouch and "When this creature dies, return it to
 * the battlefield tapped under its owner's control."
 * You gain 2 life.
 *
 * Deathtouch is a turn-long [Effects.GrantKeyword]; the quoted ability is the self-bound dies
 * trigger granted for the turn, exactly Undying Malice's shape minus its counter — the creature
 * moves graveyard → battlefield tapped (gated on `fromZone = GRAVEYARD`, so it no-ops if the card
 * already left the graveyard), and `Move` returns it under its owner's control. Then
 * [Effects.GainLife] for the caster.
 */
val InfuseWithVitality = card("Infuse with Vitality") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains deathtouch and \"When this creature " +
        "dies, return it to the battlefield tapped under its owner's control.\"\nYou gain 2 life."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, t) then
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
            ) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Andrey Kuzinskiy"
        flavorText = "Dina raised her cup to Killian's lips, and for the first time in his life, he " +
            "knew compassion."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/840600a9-3e78-48a6-b75b-860446b82c1b.jpg?1783927311"
    }
}
