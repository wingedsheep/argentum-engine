package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nahiri's Lithoforming — Zendikar Rising #151
 * {X}{R}{R} · Sorcery
 *
 * Sacrifice X lands. For each land sacrificed this way, draw a card. You may play X additional
 * lands this turn. Lands you control enter tapped this turn.
 *
 * The sacrifice happens on resolution, so with fewer than X lands you sacrifice them all and draw
 * that many. The enters-tapped clause is a floating grant keyed on the caster: it outlives the
 * sorcery and taps every land entering under your control this turn, played or put onto the
 * battlefield.
 */
val NahirisLithoforming = card("Nahiri's Lithoforming") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sacrifice X lands. For each land sacrificed this way, draw a card. " +
        "You may play X additional lands this turn. Lands you control enter tapped this turn."

    spell {
        effect = Effects.Sacrifice(GameObjectFilter.Land, DynamicAmounts.xValue(), EffectTarget.Controller) then
            Effects.DrawCards(DynamicAmounts.permanentsSacrificedThisWay()) then
            Effects.PlayAdditionalLands(DynamicAmounts.xValue()) then
            Effects.GrantReplacementEffect(
                replacement = PermanentsEnterTapped(
                    appliesTo = EventPattern.ZoneChangeEvent(
                        filter = GameObjectFilter.Land.youControl(),
                        to = Zone.BATTLEFIELD
                    )
                ),
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Campbell White"
        flavorText = "\"Zendikar has suffered enough. It is time, at last, for my home to know peace.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06bb4bc1-6ef9-4015-9f5f-36f51dbac87e.jpg?1783929355"
    }
}
