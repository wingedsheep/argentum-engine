package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brazen Cannonade
 * {3}{R}
 * Enchantment
 *
 * Whenever an attacking creature you control dies, this enchantment deals 2 damage to each opponent.
 * Raid — At the beginning of each of your postcombat main phases, if you attacked this turn, exile
 * the top card of your library. Until end of combat on your next turn, you may play that card.
 *
 * Implementation notes:
 * - "Attacking" is gated in the dies-trigger filter, which reads the zone-change event's last-known
 *   `wasAttacking` — a dead token is gone before the trigger resolves, so a resolution-time check
 *   would miss it.
 * - "Until end of combat on your next turn" has no single [MayPlayExpiry]: may-play windows are
 *   removed only at cleanup, so `UntilControllerStep(END_COMBAT)` would leave the card playable in
 *   the next turn's postcombat main phase. The window is composed from two grants on the same card:
 *     1. [MayPlayExpiry.EndOfTurn] — the rest of this turn (the trigger resolves in your postcombat
 *        main phase, so the gate on grant 2 is closed for now).
 *     2. [MayPlayExpiry.UntilEndOfNextTurn] gated by "it's not your turn, or it's your beginning
 *        phase, your precombat main phase, or your turn's first combat phase" — open through every
 *        other player's turn and your next turn up to and including its first end of combat step,
 *        closed afterwards, and removed at that turn's cleanup.
 */
val BrazenCannonade = card("Brazen Cannonade") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever an attacking creature you control dies, this enchantment deals 2 damage " +
        "to each opponent.\n" +
        "Raid — At the beginning of each of your postcombat main phases, if you attacked this turn, " +
        "exile the top card of your library. Until end of combat on your next turn, you may play that card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attacking().youControl()).dies()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever an attacking creature you control dies, this enchantment deals 2 " +
            "damage to each opponent."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.EndOfTurn))
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiled,
                    expiry = MayPlayExpiry.UntilEndOfNextTurn,
                    condition = Conditions.Any(
                        Conditions.IsNotYourTurn,
                        Conditions.IsInPhase(Phase.BEGINNING, Phase.PRECOMBAT_MAIN),
                        Conditions.All(
                            Conditions.IsInPhase(Phase.COMBAT),
                            Conditions.IsFirstCombatPhaseOfTurn,
                        ),
                    ),
                )
            )
        }
        description = "Raid — At the beginning of each of your postcombat main phases, if you " +
            "attacked this turn, exile the top card of your library. Until end of combat on your " +
            "next turn, you may play that card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbcbad10-475e-41cf-b3fb-68c1ca6c2caa.jpg?1783919184"
        ruling(
            "2022-12-02",
            "Playing a card exiled with this ability follows the normal rules for playing the card. " +
                "You must pay its costs, and you must follow all applicable timing rules."
        )
        ruling(
            "2022-12-02",
            "If you exile a land card with the raid ability, you may play that land only if you have " +
                "any available land plays."
        )
    }
}
