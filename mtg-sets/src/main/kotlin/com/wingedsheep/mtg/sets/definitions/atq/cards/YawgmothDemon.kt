package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yawgmoth Demon
 * {4}{B}{B}
 * Creature — Phyrexian Demon
 * 6/6
 * Flying, first strike
 * At the beginning of your upkeep, you may sacrifice an artifact. If you don't, tap this
 * creature and it deals 2 damage to you.
 *
 * The literal "you may [action]. If you don't, [consequence]" reading: an optional triggered
 * ability whose body is "sacrifice an artifact" and whose `elseEffect` is the upkeep tax — tap
 * this creature and deal 2 damage to its controller. With an artifact the controller is asked
 * whether to sacrifice one (decline → tax); with no artifact the sacrifice is impossible, so the
 * engine skips the prompt and applies the tax automatically (the no-target may-action's feasibility
 * is derived from the [SacrificeEffect], so an impossible "may" falls straight to its else branch).
 */
val YawgmothDemon = card("Yawgmoth Demon") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Demon"
    power = 6
    toughness = 6
    oracleText = "Flying, first strike\n" +
        "At the beginning of your upkeep, you may sacrifice an artifact. If you don't, tap " +
        "this creature and it deals 2 damage to you."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = SacrificeEffect(GameObjectFilter.Artifact)
        elseEffect = Effects.Composite(
            listOf(
                Effects.Tap(EffectTarget.Self),
                Effects.DealDamage(2, EffectTarget.Controller, damageSource = EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04bbd231-0d5f-4cbf-92a7-10d2c5c4b82c.jpg?1562895987"
    }
}
