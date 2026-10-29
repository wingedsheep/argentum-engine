package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Summon: Yojimbo — Final Fantasy Commander #28
 * {3}{W} · Enchantment Creature — Saga Samurai · 5/5
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Exile target artifact, enchantment, or tapped creature an opponent controls.
 * II, III — Until your next turn, creatures can't attack you unless their controller pays {2} for
 *          each of those creatures.
 * IV — Create X Treasure tokens, where X is the number of opponents who control a creature with
 *      power 4 or greater.
 * Vigilance
 *
 * - Chapters II and III hand Ghostly Prison's printed tax to *you* for the duration: a
 *   player-anchored `GrantStaticAbility(AttackTax({2}), Controller, UntilYourNextTurn)`. The tax
 *   is a rule about the player, so it keeps applying after the Saga is sacrificed, and it shows as
 *   an "Attack Tax" badge on you.
 * - Chapter I's target is a heterogeneous union — the "tapped" restriction binds only to the
 *   creature branch (Banishing Slash's filter, scoped to an opponent).
 * - Chapter IV counts opponents once, as it resolves (ruling 2025-06-06).
 */
val SummonYojimbo = card("Summon: Yojimbo") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Saga Samurai"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Exile target artifact, enchantment, or tapped creature an opponent controls.\n" +
        "II, III — Until your next turn, creatures can't attack you unless their controller pays {2} " +
        "for each of those creatures.\n" +
        "IV — Create X Treasure tokens, where X is the number of opponents who control a creature " +
        "with power 4 or greater.\n" +
        "Vigilance"
    power = 5
    toughness = 5

    keywords(Keyword.VIGILANCE)

    sagaChapter(1) {
        val permanent = target(
            TargetFilter(
                GameObjectFilter.Artifact.opponentControls() or
                    GameObjectFilter.Enchantment.opponentControls() or
                    GameObjectFilter.Creature.tapped().opponentControls()
            )
        )
        effect = Effects.Exile(permanent)
    }

    val attackTax = Effects.GrantStaticAbility(
        ability = AttackTax(DynamicAmounts.fixed(2)),
        target = EffectTarget.Controller,
        duration = Duration.UntilYourNextTurn,
    )
    sagaChapter(2) { effect = attackTax }
    sagaChapter(3) { effect = attackTax }

    sagaChapter(4) {
        effect = Effects.CreateTreasure(
            DynamicAmounts.countPlayersWith(
                Player.EachOpponent,
                Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4)),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Benjamin Ee"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdbc2d28-ea35-47c6-af7e-07924e9d5794.jpg?1783906365"
        ruling(
            "2025-06-06",
            "On turns where the effect of Summon: Yojimbo's second or third chapter ability is applying, " +
                "your opponents can choose not to attack with a creature that must attack if able as long " +
                "as there is no other player, planeswalker, or battle for that creature to attack that " +
                "wouldn't require a cost."
        )
        ruling(
            "2025-06-06",
            "The value of X is determined only once, as Summon: Yojimbo's fourth chapter ability resolves."
        )
    }
}
