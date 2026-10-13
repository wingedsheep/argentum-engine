package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sol'Kanar the Tainted
 * {2}{U}{B}{R}
 * Legendary Creature — Elemental Demon
 * 5/5
 * At the beginning of your end step, choose one that hasn't been chosen —
 * • Draw a card.
 * • Each opponent loses 2 life and you gain 2 life.
 * • Sol'Kanar deals 3 damage to up to one other target creature or planeswalker.
 * • Exile Sol'Kanar, then return it to the battlefield under an opponent's control.
 *
 * The Zuko, Conflicted shape: [ModalEffect.chooseOneNotYetChosen] keeps a per-object memory of
 * the modes already chosen. The fourth mode returns Sol'Kanar as a new object, so its new
 * controller starts with a fresh memory and all four modes on offer again (per the ruling, that
 * player makes every choice for it from then on). The life gain is a fixed 2, not "the life lost
 * this way" — so it's a loss-then-gain, not [Effects.DrainLife].
 */
val SolKanarTheTainted = card("Sol'Kanar the Tainted") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Elemental Demon"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your end step, choose one that hasn't been chosen —\n" +
        "• Draw a card.\n" +
        "• Each opponent loses 2 life and you gain 2 life.\n" +
        "• Sol'Kanar deals 3 damage to up to one other target creature or planeswalker.\n" +
        "• Exile Sol'Kanar, then return it to the battlefield under an opponent's control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = ModalEffect.chooseOneNotYetChosen(
            Mode.noTarget(Effects.DrawCards(1), "Draw a card"),
            Mode.noTarget(
                Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2),
                "Each opponent loses 2 life and you gain 2 life"
            ),
            mode("Sol'Kanar deals 3 damage to up to one other target creature or planeswalker") {
                val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).other(), optional = true)
                effect = Effects.DealDamage(3, victim)
            },
            Mode.noTarget(
                Effects.Move(EffectTarget.Self, Zone.EXILE) then
                    Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        controllerOverride = EffectTarget.PlayerRef(Player.AnOpponent)
                    ),
                "Exile Sol'Kanar, then return it to the battlefield under an opponent's control"
            )
        )
        description = "At the beginning of your end step, choose one that hasn't been chosen —"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3027347d-5fd3-47cd-83d4-8c834e00794c.jpg?1783921275"

        ruling(
            "2022-09-09",
            "Once Sol'kanar the Tainted is controlled by your opponent, its ability triggers during that player's end step and that player makes all choices for it. That player draws a card for its first mode and gains life for its second mode, that player's opponents lose life for its second mode, and that player chooses one of their opponents for its last mode."
        )
    }
}
