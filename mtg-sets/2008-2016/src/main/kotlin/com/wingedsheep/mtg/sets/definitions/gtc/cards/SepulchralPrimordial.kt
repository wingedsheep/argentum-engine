package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Sepulchral Primordial — Gatecrash #75
 * {5}{B}{B} · Creature — Avatar · 5/4
 *
 * Intimidate
 * When this creature enters, for each opponent, you may put up to one target creature card from
 * that player's graveyard onto the battlefield under your control.
 *
 * "For each opponent, up to one target … from that player's graveyard" is one target declaration
 * distributed over graveyard owners ([OwnerDistribution.UP_TO_ONE], Afterlife from the Loam's shape)
 * restricted to opponents' graveyards, so no opponent yields two and zero targets is legal. The
 * "you may" is a per-card choice at resolution: you pick any number of the surviving targets, and
 * those enter the battlefield simultaneously under your control (Scryfall ruling).
 */
val SepulchralPrimordial = card("Sepulchral Primordial") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Avatar"
    power = 5
    toughness = 4
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures " +
        "that share a color with it.)\nWhen this creature enters, for each opponent, you may put up to one " +
        "target creature card from that player's graveyard onto the battlefield under your control."

    keywords(Keyword.INTIMIDATE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Creature.ownedByOpponent(), zone = Zone.GRAVEYARD),
            ownerDistribution = OwnerDistribution.UP_TO_ONE,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val put = chooseAnyNumber(
                from = chosen,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Put any number of the targeted creature cards onto the battlefield under your control",
            )
            move(put, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb0865cd-d9b4-43ea-87d2-ad5c65fc0459.jpg?1783940128"
    }
}
