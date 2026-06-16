package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Breeches, the Blastmaker
 * {1}{U}{R}
 * Legendary Creature — Goblin Pirate
 * 3/3
 * Menace
 * Whenever you cast your second spell each turn, you may sacrifice an artifact. If you do,
 * flip a coin. When you win the flip, copy that spell. You may choose new targets for the copy.
 * When you lose the flip, Breeches deals damage equal to that spell's mana value to any target.
 *
 * Modeling notes:
 * - "your second spell each turn" → [Triggers.NthSpellCast] with n = 2, player = You.
 * - "you may sacrifice an artifact. If you do, …" → [OptionalCostEffect]: the optional
 *   [SacrificeEffect] cost gates the coin flip (declining or having no artifact skips the flip,
 *   matching the ruling that neither delayed trigger fires unless an artifact is sacrificed).
 * - The flip's branches copy the triggering spell ([EffectTarget.TriggeringEntity]) and deal
 *   damage equal to its mana value.
 *
 * Timing deviation (documented): Scryfall rules the trigger needs no target, and the lose-flip
 * target is chosen only after the coin is lost. The engine declares targets on the ability, so
 * the "any target" for the damage is chosen when the trigger is put on the stack. This is the
 * same idiom used by Risky Move (choose target up front, then flip). The only behavioral
 * difference is the moment of target selection; the copy/damage outcomes are identical.
 */
val BreechesTheBlastmaker = card("Breeches, the Blastmaker") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Goblin Pirate"
    power = 3
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever you cast your second spell each turn, you may sacrifice an artifact. " +
        "If you do, flip a coin. When you win the flip, copy that spell. You may choose new " +
        "targets for the copy. When you lose the flip, Breeches deals damage equal to that " +
        "spell's mana value to any target."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        val damageTarget = target("any target", Targets.Any)
        effect = OptionalCostEffect(
            cost = SacrificeEffect(filter = GameObjectFilter.Artifact),
            ifPaid = FlipCoinEffect(
                wonEffect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
                lostEffect = Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(
                        EntityReference.Triggering,
                        EntityNumericProperty.ManaValue
                    ),
                    target = damageTarget
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf3bda9e-42af-4f99-a504-c96c25c2794b.jpg?1712356062"

        ruling("2024-04-12", "Spells that were cast before Breeches entered the battlefield count. If Breeches was the first spell you cast this turn, the next spell you cast this turn is your second spell.")
        ruling("2024-04-12", "Breeches's triggered ability triggers without requiring a target. It sets up two delayed triggered abilities: one that triggers when you win the flip, and one that triggers when you lose the flip. Only one of these abilities will trigger, based on the result of the flip.")
        ruling("2024-04-12", "If you don't sacrifice an artifact, you won't flip a coin, and neither delayed triggered ability will trigger.")
        ruling("2024-04-12", "If you win the flip, the copy created by Breeches's delayed triggered ability will have the same targets as the spell it's copying unless you choose new ones.")
        ruling("2024-04-12", "The copy made by Breeches's delayed triggered ability is created on the stack, so it's not \"cast.\"")
    }
}
