package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Macabre Mockery
 * {2}{B}{R}
 * Instant
 *
 * Put target creature card from an opponent's graveyard onto the battlefield under your control.
 * It gets +2/+0 and gains haste until end of turn. Sacrifice it at the beginning of the next end step.
 */
val MacabreMockery = card("Macabre Mockery") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Put target creature card from an opponent's graveyard onto the battlefield under your " +
        "control. It gets +2/+0 and gains haste until end of turn. Sacrifice it at the beginning of the " +
        "next end step."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Creature.ownedByOpponent(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldUnderYourControl(t) then
            Effects.ModifyStats(2, 0, t) then
            Effects.GrantKeyword(Keyword.HASTE, t, Duration.EndOfTurn) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.SacrificeTarget(t)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Deruchenko Alexander"
        flavorText = "The Rakdos put the \"fun\" in \"funeral.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c1aee37-dcee-4e58-be9b-81ca57cedb53.jpg?1783933642"
    }
}
