package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ashiok, Sculptor of Fears
 * {4}{U}{B}
 * Legendary Planeswalker — Ashiok
 * Loyalty 4
 * +2: Draw a card. Each player mills two cards.
 * −5: Put target creature card from a graveyard onto the battlefield under your control.
 * −11: Gain control of all creatures target opponent controls.
 *
 * Modeling notes:
 *  - +2: a draw, then Returned Reveler's `mill(2, PlayerRef(Player.Each))`.
 *  - −5: Reanimate's move from a graveyard to the battlefield with `controllerOverride = Controller`.
 *  - −11: Emrakul, the World Anew's `ForEachInGroup(Creature.targetPlayerControls) → GainControl`
 *    with [Duration.Permanent] — the control change outlives Ashiok (ruling). The group is fixed
 *    on resolution, so creatures that opponent gets later stay theirs.
 */
val AshiokSculptorOfFears = card("Ashiok, Sculptor of Fears") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Ashiok"
    startingLoyalty = 4
    oracleText = "+2: Draw a card. Each player mills two cards.\n" +
        "−5: Put target creature card from a graveyard onto the battlefield under your control.\n" +
        "−11: Gain control of all creatures target opponent controls."

    // +2: Draw a card. Each player mills two cards.
    loyaltyAbility(+2) {
        effect = Effects.DrawCards(1) then
            Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.Each))
        description = "Draw a card. Each player mills two cards."
    }

    // −5: Put target creature card from a graveyard onto the battlefield under your control.
    loyaltyAbility(-5) {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(
            creature,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        )
        description = "Put target creature card from a graveyard onto the battlefield under your control."
    }

    // −11: Gain control of all creatures target opponent controls.
    loyaltyAbility(-11) {
        val opponent = target(Targets.Opponent)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent)),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "Gain control of all creatures target opponent controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "274"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36a5fc37-e7bd-41a9-88ac-74a3f14fea8b.jpg?1783931501"
        ruling(
            "2020-01-24",
            "The effect of Ashiok's last ability lasts indefinitely. It doesn't expire as the turn ends or if " +
                "Ashiok leaves the battlefield."
        )
    }
}
