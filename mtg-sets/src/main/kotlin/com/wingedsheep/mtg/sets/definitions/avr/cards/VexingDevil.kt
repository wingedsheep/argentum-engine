package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vexing Devil
 * {R}
 * Creature — Devil
 * 4/3
 * When this creature enters, any opponent may have it deal 4 damage to them.
 * If a player does, sacrifice this creature.
 *
 * Per the 2018-12-07 ruling, each opponent in turn order chooses independently, so the ability is
 * modelled as `ForEachPlayer(EachOpponent)` with the yes/no delegated to the opponent being asked
 * (`decisionMaker`). Accepting deals the damage and sacrifices the Devil in the same, uninterruptible
 * step — no player gets priority in between, which is exactly what the second ruling requires. A
 * second opponent accepting after the Devil is already gone still takes 4 (last-known information);
 * the redundant sacrifice is a no-op.
 *
 * Known deviation: `Player.EachOpponent` iterates opponents in seat order rather than APNAP order.
 * Identical in a two-player game; in multiplayer the *order* of the prompts can differ from the
 * ruling, though every opponent is still asked and every acceptance still resolves.
 */
val VexingDevil = card("Vexing Devil") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, any opponent may have it deal 4 damage to them. " +
        "If a player does, sacrifice this creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            listOf(
                MayEffect(
                    effect = Effects.Composite(
                        Effects.DealDamage(
                            amount = 4,
                            target = EffectTarget.PlayerRef(Player.You),
                            damageSource = EffectTarget.Self,
                        ),
                        // `ForEachPlayer` rebinds the resolving controller to the opponent being
                        // asked, so the sacrifice has to name the Devil's *own* controller as the
                        // actor — otherwise the control mismatch silently skips it.
                        SacrificeTargetEffect(
                            target = EffectTarget.Self,
                            sacrificedByItsController = true,
                        ),
                    ),
                    decisionMaker = EffectTarget.PlayerRef(Player.You),
                    // Shown to the opponent being asked, so it reads from their perspective.
                    descriptionOverride = "Have Vexing Devil deal 4 damage to you? " +
                        "If you do, its controller sacrifices it.",
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Lucas Graciano"
        flavorText = "It's not any fun until someone loses an eye."
        imageUri =
            "https://cards.scryfall.io/normal/front/d/b/dbbefd98-4b17-4cc2-9ef9-8807f594cb16.jpg?1783940672"

        ruling(
            "2018-12-07",
            "As Vexing Devil's ability resolves, the next opponent in turn order (or, if it's an " +
                "opponent's turn, that opponent) chooses whether to be dealt 4 damage, then each " +
                "other opponent in turn order does the same. If any of them do, you sacrifice " +
                "Vexing Devil.",
        )
        ruling(
            "2018-12-07",
            "No player may take actions between the time an opponent chooses to be dealt damage " +
                "by Vexing Devil and the time you sacrifice Vexing Devil.",
        )
        ruling(
            "2012-05-01",
            "If a player chooses to have Vexing Devil deal 4 damage to them, but some or all of " +
                "that damage is prevented or redirected, Vexing Devil will still be sacrificed.",
        )
    }
}
