package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Junji, the Midnight Sky — Kamigawa: Neon Dynasty #102 (canonical printing)
 * {3}{B}{B} · Legendary Creature — Dragon Spirit · 5/5
 *
 * Flying, menace
 * When Junji dies, choose one —
 * • Each opponent discards two cards and loses 2 life.
 * • Put target non-Dragon creature card from a graveyard onto the battlefield under your control.
 *   You lose 2 life.
 *
 * The NEO Dragon Spirit cycle's modal dies trigger (Ao / Atsushi shape). Mode one is Unnerve's
 * [Patterns.Hand.eachOpponentDiscards] followed by an each-opponent life loss. Mode two is
 * Reanimate's graveyard-to-battlefield move with a controller override, targeting any graveyard
 * and excluding Dragons (so Junji can never return itself); it is the only targeted mode.
 */
val JunjiTheMidnightSky = card("Junji, the Midnight Sky") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying, menace\n" +
        "When Junji dies, choose one —\n" +
        "• Each opponent discards two cards and loses 2 life.\n" +
        "• Put target non-Dragon creature card from a graveyard onto the battlefield under your control. " +
        "You lose 2 life."

    keywords(Keyword.FLYING, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(
                    Patterns.Hand.eachOpponentDiscards(2) then
                        Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)),
                    "Each opponent discards two cards and loses 2 life."
                ),
                mode(
                    "Put target non-Dragon creature card from a graveyard onto the battlefield under " +
                        "your control. You lose 2 life."
                ) {
                    val creature = target(TargetFilter.CreatureInGraveyard.notSubtype(Subtype.DRAGON))
                    effect = Effects.Move(
                        creature,
                        Zone.BATTLEFIELD,
                        controllerOverride = EffectTarget.Controller,
                        fromZone = Zone.GRAVEYARD
                    ) then Effects.LoseLife(2, EffectTarget.Controller)
                }
            )
        )
        description = "When Junji dies, choose one — • Each opponent discards two cards and loses 2 life. " +
            "• Put target non-Dragon creature card from a graveyard onto the battlefield under your " +
            "control. You lose 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "102"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9033cf27-d9e6-49b8-8ee1-c1f38c9680b9.jpg?1783923885"
    }
}
