package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Will of the Jeskai — Tarkir: Dragonstorm Commander #40 (canonical printing)
 * {3}{R} · Sorcery
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Each player may discard their hand and draw five cards.
 * • Each instant and sorcery card in your graveyard gains flashback until end of turn. The
 *   flashback cost is equal to its mana cost.
 *
 * The conditional mode count is the Will cycle's shape (Will of the Sultai): `minChooseCount = 1`,
 * capped at 2 when [Conditions.YouControlACommander] holds as the spell is cast.
 *
 * Mode one is Raphael's Technique's per-player optional wheel: each player, in APNAP order, decides
 * on their own whether to discard their hand and draw five. Mode two gathers the instant and sorcery
 * cards in your graveyard as it resolves and grants each one flashback for its own mana cost
 * (Archmage's Newt's single-card grant, run once per gathered card). When both modes are chosen they
 * resolve in printed order, so instants and sorceries you discarded to the wheel gain flashback too.
 */
val WillOfTheJeskai = card("Will of the Jeskai") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Each player may discard their hand and draw five cards.\n" +
        "• Each instant and sorcery card in your graveyard gains flashback until end of turn. " +
        "The flashback cost is equal to its mana cost."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Each player may discard their hand and draw five cards") {
                effect = Effects.ForEachPlayer(
                    players = Player.Each,
                    effect = Effects.May(
                        decisionMaker = EffectTarget.Controller,
                        effect = Patterns.Hand.discardHand(EffectTarget.Controller) then Effects.DrawCards(5)
                    )
                )
            }
            mode("Each instant and sorcery card in your graveyard gains flashback until end of turn. The flashback cost is equal to its mana cost") {
                effect = Effects.Pipeline {
                    val spells = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery))
                    run(Effects.ForEachInCollection(spells, Effects.GrantFlashback(EffectTarget.IterationEntity)))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Jessica Fong"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d05bac1f-d455-4528-93b2-a94c29b5dc2c.jpg?1783907169"
        ruling("2025-04-04", "A player may discard their hand even if their hand contains zero cards.")
        ruling("2025-04-04", "The commander you control doesn't have to be your commander.")
        ruling(
            "2025-04-04",
            "Whether or not you control a commander is determined only once, as you choose the modes for this " +
                "spell. If you somehow lose control of that commander before you finish casting the spell " +
                "(perhaps because you sacrifice it to activate a mana ability), it won't change the number of " +
                "modes chosen."
        )
    }
}
