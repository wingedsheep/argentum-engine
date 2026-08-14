package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Power Conduit — Mirrodin #229
 * {2} · Artifact
 *
 * {T}, Remove a counter from a permanent you control: Choose one —
 * • Put a charge counter on target artifact.
 * • Put a +1/+1 counter on target creature.
 *
 * The counter spent is of *any* kind off *any* permanent you control — `counterType = null` on
 * [Costs.RemoveCounters] means "any combination", which for a single counter is simply "the player
 * picks which one". The Conduit itself is a legal source (it's a permanent you control), so a
 * charge counter it put on itself can be recycled.
 *
 * The two modes are a printed "Choose one —" on an activated ability, so this is a plain
 * [ModalEffect.chooseOne] with per-mode targets — each mode demands its own target only when
 * chosen, and the ability is not a modal *spell* (nothing keys off that here).
 */
val PowerConduit = card("Power Conduit") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Remove a counter from a permanent you control: Choose one —\n" +
        "• Put a charge counter on target artifact.\n" +
        "• Put a +1/+1 counter on target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounters(count = 1, counterType = null, filter = GameObjectFilter.Permanent)
        )
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.AddCounters(Counters.CHARGE, 1, EffectTarget.ContextTarget(0)),
                Targets.Artifact,
                "Put a charge counter on target artifact"
            ),
            Mode.withTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Put a +1/+1 counter on target creature"
            )
        )
        description = "{T}, Remove a counter from a permanent you control: Choose one — " +
            "Put a charge counter on target artifact; or put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Todd Lockwood"
        flavorText = "Never content, vedalken artificers continually tinker with their creations."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0f5c84f-1924-4a4a-84c1-00dcb756e9c9.jpg?1783944506"
    }
}
