package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Balrog, Durin's Bane
 * {5}{B}{R}
 * Legendary Creature — Avatar Demon
 * 7/5
 *
 * This spell costs {1} less to cast for each permanent sacrificed this turn.
 * Haste
 * The Balrog can't be blocked except by legendary creatures.
 * When The Balrog dies, destroy target artifact or creature an opponent controls.
 *
 * Engine notes (Gap 30 — cost reduction by per-turn game history):
 * - "Costs {1} less for each permanent sacrificed this turn" is the per-turn-history analogue of
 *   the existing zone-count cost reductions. It reads the new turn-scoped GameState counter
 *   `permanentsSacrificedThisTurn` via `CostReductionSource.PermanentsSacrificedThisTurn`. The
 *   wording is NOT controller-scoped — every permanent sacrificed by any player this turn counts.
 *   The counter is incremented by the central sacrifice hook
 *   `ZoneTransitionService.trackPermanentSacrifice` (called at every sacrifice site) and reset to
 *   0 in `TurnManager.startTurn`. The reduction floors the mana component at the colored
 *   requirement {B}{R} (CR 601.2f), so the cheapest possible cast is {B}{R}.
 * - "Can't be blocked except by legendary creatures" — the evasion static
 *   `CantBeBlockedExceptBy` with a legendary-creature blocker filter.
 * - The dies trigger destroys "target artifact or creature an opponent controls"
 *   (`GameObjectFilter.CreatureOrArtifact.opponentControls()`).
 */
val TheBalrogDurinsBane = card("The Balrog, Durin's Bane") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Avatar Demon"
    oracleText = "This spell costs {1} less to cast for each permanent sacrificed this turn.\n" +
        "Haste\n" +
        "The Balrog can't be blocked except by legendary creatures.\n" +
        "When The Balrog dies, destroy target artifact or creature an opponent controls."
    power = 7
    toughness = 5

    keywords(Keyword.HASTE)

    // This spell costs {1} less to cast for each permanent sacrificed this turn.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsSacrificedThisTurn(),
            ),
        )
    }

    // The Balrog can't be blocked except by legendary creatures.
    staticAbility {
        ability = CantBeBlockedExceptBy(blockerFilter = GameObjectFilter.Creature.legendary())
    }

    // When The Balrog dies, destroy target artifact or creature an opponent controls.
    triggeredAbility {
        trigger = Triggers.Dies
        target(
            "target artifact or creature an opponent controls",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls())),
        )
        effect = Effects.Destroy(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/416880c3-cefb-45ea-bcc3-2ec7a70c8097.jpg?1686969678"
    }
}
