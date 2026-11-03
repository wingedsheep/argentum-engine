package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantPhaseIn
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The City on the Edge of Forever — Star Trek #228
 * {3}{W}{B} · Enchantment — Saga
 *
 * I — All artifacts and creatures phase out. Create a 0/1 white Human creature token with
 * "Permanents can't phase in."
 * II — You draw two cards and each player loses 2 life.
 * III — Sacrifice a creature token. If you do, you gain 5 life.
 *
 * - Chapter I phases each artifact and creature out directly (their Auras and Equipment go with
 *   them, CR 702.26g), then makes the Human, whose [CantPhaseIn] keeps everything phased out for
 *   as long as it stays on the battlefield. Sacrificing it (chapter III is one way) lifts the lock;
 *   the phased-out permanents phase in at their controllers' next untap steps.
 * - Chapter III sacrifices any creature token you control — the Human or another — chosen at
 *   resolution; the life gain is gated on the sacrifice actually happening.
 */
val TheCityOnTheEdgeOfForever = card("The City on the Edge of Forever") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — All artifacts and creatures phase out. Create a 0/1 white Human creature token with " +
        "\"Permanents can't phase in.\" (Treat phased-out permanents and anything attached to them as " +
        "though they don't exist.)\n" +
        "II — You draw two cards and each player loses 2 life.\n" +
        "III — Sacrifice a creature token. If you do, you gain 5 life."

    sagaChapter(1) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature),
            Effects.PhaseOut(EffectTarget.IterationEntity)
        ) then Effects.CreateToken(
            power = 0, toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            staticAbilities = listOf(CantPhaseIn),
            imageUri = "https://cards.scryfall.io/normal/front/2/5/25b0644d-4934-4495-9b6e-c31d1d10c298.jpg?1784177188"
        )
    }

    sagaChapter(2) {
        effect = Effects.DrawCards(2) then
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))
    }

    sagaChapter(3) {
        effect = Effects.IfYouDo(
            action = Effects.SacrificeOwn(GameObjectFilter.Creature.token()),
            then = Effects.GainLife(5),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Peter Diamond"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aac6f64d-d39a-4994-bc70-6b477b2640f8.jpg?1787205815"
    }
}
