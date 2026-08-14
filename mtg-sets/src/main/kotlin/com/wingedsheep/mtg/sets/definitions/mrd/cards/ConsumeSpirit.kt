package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Consume Spirit — Mirrodin #60
 * {X}{1}{B} · Sorcery
 *
 * Spend only black mana on X.
 * Consume Spirit deals X damage to any target and you gain X life.
 *
 * The Soul Burn shape: `xManaRestriction = {BLACK}` confines the `{X}` portion of the cost to
 * black sources (colorless mana can pay the `{1}` but never X), and the spell resolves as a
 * [Effects.Composite] of [Effects.DealXDamage] and [Effects.GainLife].
 *
 * The life gain is [DynamicAmount.XValue] — the chosen X — not the damage actually dealt. Per the
 * ruling, prevention or damage redirection does not shrink the life gain. (Soul Burn's sibling
 * reads [DynamicAmount.ManaSpentOnX] instead, because *its* gain is scoped to the black portion
 * of a two-color restriction; here the whole restriction is black, so plain X is the faithful
 * reading and stays correct when X is paid with e.g. a Cabal Coffers activation.)
 *
 * One target for the whole spell: if it is illegal on resolution the spell doesn't resolve at all
 * and no life is gained, which falls out of the engine's standard fizzle path rather than needing
 * any wiring here.
 */
val ConsumeSpirit = card("Consume Spirit") {
    manaCost = "{X}{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spend only black mana on X.\n" +
        "Consume Spirit deals X damage to any target and you gain X life."

    spell {
        xManaRestriction = setOf(Color.BLACK)
        target = Targets.Any
        effect = Effects.Composite(
            Effects.DealXDamage(EffectTarget.ContextTarget(0)),
            Effects.GainLife(DynamicAmount.XValue)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Matt Thompson"
        flavorText = "Mephidross changes all who dwell there, taking their lives and adding them to its own."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f375a49c-806a-4d8b-9513-6b4afc19497b.jpg?1783944549"

        ruling(
            "2009-10-01",
            "The amount of life you gain is equal to the number chosen for X, not the amount of " +
                "damage Consume Spirit deals (in case some of it is prevented)."
        )
        ruling(
            "2009-10-01",
            "If the targeted permanent or player is an illegal target by the time Consume Spirit " +
                "would resolve, the entire spell doesn't resolve. You won't gain any life."
        )
    }
}
