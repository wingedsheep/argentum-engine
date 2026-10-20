package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stangg
 * {4}{R}{G}
 * Legendary Creature — Human Warrior
 * 3/4
 * When Stangg enters, create Stangg Twin, a legendary 3/4 red and green Human Warrior creature
 * token. Exile that token when Stangg leaves the battlefield. Sacrifice Stangg when that token
 * leaves the battlefield.
 *
 * The two linked clauses are one-shot leaves-watchers created with the token, one on Stangg and
 * one on the Twin. Each watches the object it was created for: a Stangg that is flickered sets off
 * its watcher once (exiling the old Twin), and the Twin-leaves watcher then finds no Stangg to
 * sacrifice — the Stangg that came back is a new object with its own Twin (CR 400.7).
 */
val Stangg = card("Stangg") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "When Stangg enters, create Stangg Twin, a legendary 3/4 red and green Human " +
        "Warrior creature token. Exile that token when Stangg leaves the battlefield. Sacrifice " +
        "Stangg when that token leaves the battlefield."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    power = 3,
                    toughness = 4,
                    colors = setOf(Color.RED, Color.GREEN),
                    creatureTypes = setOf("Human", "Warrior"),
                    name = "Stangg Twin",
                    legendary = true,
                    imageUri = "https://cards.scryfall.io/normal/front/e/b/eba90d37-d7ac-4097-a04d-1f27e4c9e5de.jpg?1783935203",
                )
            )
            val twin = EffectTarget.PipelineTarget(CREATED_TOKENS)
            run(
                Effects.CreateDelayedTrigger(
                    trigger = Triggers.self.leaves(),
                    watchedTarget = EffectTarget.Self,
                    effect = Effects.Exile(twin),
                    expiry = DelayedTriggerExpiry.Never,
                    fireOnce = true,
                )
            )
            run(
                Effects.CreateDelayedTrigger(
                    trigger = Triggers.self.leaves(),
                    watchedTarget = twin,
                    effect = Effects.SacrificeTarget(EffectTarget.Self),
                    expiry = DelayedTriggerExpiry.Never,
                    fireOnce = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a277775a-6b48-4238-a618-3ae94c4cc85c.jpg?1783948032"
    }
}
