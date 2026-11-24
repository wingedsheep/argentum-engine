package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volatile Rig — Return to Ravnica #236
 * {4} · Artifact Creature — Construct · 4/4
 *
 * Trample
 * This creature attacks each combat if able.
 * Whenever this creature is dealt damage, flip a coin. If you lose the flip, sacrifice this creature.
 * When this creature dies, flip a coin. If you lose the flip, it deals 4 damage to each creature and
 * each player.
 *
 * - "Is dealt damage" triggers once per damage event (its ruling) — the bare
 *   `Triggers.self.isDealtDamage()`, as Fungusaur.
 * - The dies payoff is Magma Phoenix's shape (a creature iteration, then a player iteration) wrapped
 *   in the lost branch of the coin flip; the dead Rig is the damage source via last-known information.
 */
val VolatileRig = card("Volatile Rig") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "This creature attacks each combat if able.\n" +
        "Whenever this creature is dealt damage, flip a coin. If you lose the flip, sacrifice this creature.\n" +
        "When this creature dies, flip a coin. If you lose the flip, it deals 4 damage to each creature and each player."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.FlipCoin(lostEffect = SacrificeSelfEffect)
        description = "Whenever this creature is dealt damage, flip a coin. If you lose the flip, sacrifice this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.FlipCoin(
            lostEffect = Effects.ForEachInGroup(
                GroupFilter.AllCreatures,
                Effects.DealDamage(4, EffectTarget.IterationEntity),
            ) then
                Effects.ForEachPlayer(
                    Player.Each,
                    listOf(Effects.DealDamage(4, EffectTarget.Controller)),
                ),
        )
        description = "When this creature dies, flip a coin. If you lose the flip, it deals 4 damage to each creature and each player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5055ed18-b234-4702-92eb-4d483431ff47.jpg?1783940323"
        ruling(
            "2012-10-01",
            "If Volatile Rig is dealt damage by multiple sources at the same time (for example, multiple " +
                "blocking creatures), its first triggered ability will trigger only once.",
        )
    }
}
