package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shriek, Treblemaker
 * {2}{B/R}
 * Legendary Creature — Mutant Villain
 * 2/3
 *
 * At the beginning of your first main phase, you may discard a card. When you do, target
 * creature can't block this turn.
 * Sonic Blast — Whenever a creature an opponent controls dies, Shriek deals 1 damage to that player.
 *
 *  - **First-main discard** — a [Triggers.FirstMainPhase] trigger whose optional discard is the
 *    action of a "When you do" [ReflexiveTriggerEffect]. The reflexive ability targets a creature
 *    (chosen as it goes on the stack) and only fires if a card is actually discarded, matching the
 *    reflexive-discard pattern (cf. Inti, Passenger Ferry). [Effects.CantBlock] defaults to an
 *    end-of-turn duration ("this turn").
 *  - **Sonic Blast** — an ability word (flavor only). A [Triggers.leavesBattlefield]-to-graveyard
 *    ("dies") trigger filtered to opponent-controlled creatures; [Player.TriggeringPlayer] resolves
 *    to the dying creature's controller, so Shriek deals 1 to "that player". `damageSource` defaults
 *    to the ability's source (Shriek).
 */
val ShriekTreblemaker = card("Shriek, Treblemaker") {
    manaCost = "{2}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Mutant Villain"
    power = 2
    toughness = 3
    oracleText = "At the beginning of your first main phase, you may discard a card. When you do, " +
        "target creature can't block this turn.\n" +
        "Sonic Blast — Whenever a creature an opponent controls dies, Shriek deals 1 damage to that player."

    // "At the beginning of your first main phase, you may discard a card. When you do, target
    // creature can't block this turn."
    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = ReflexiveTriggerEffect(
            action = Effects.Discard(1),
            optional = true,
            reflexiveEffect = Effects.CantBlock(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.Creature)
        )
    }

    // "Sonic Blast — Whenever a creature an opponent controls dies, Shriek deals 1 damage to that player."
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.opponentControls(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Borja Pindado"
        flavorText = "Exposure to the Darkforce dimension left Frances Barrison craving chaos and carnage."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01f1900e-b10f-47dd-8b3d-6913fa661186.jpg?1783905312"
    }
}
