package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restless Bloodseeker // Bloodsoaked Reveler (Innistrad: Crimson Vow)
 * {1}{B}
 * Creature — Vampire // Creature — Vampire
 *
 * Front — Restless Bloodseeker (1/3)
 *   At the beginning of your end step, if you gained life this turn, create a Blood token.
 *   Sacrifice two Blood tokens: Transform this creature. Activate only as a sorcery.
 *
 * Back — Bloodsoaked Reveler (3/3)
 *   At the beginning of your end step, if you gained life this turn, create a Blood token.
 *   {4}{B}: Each opponent loses 2 life and you gain 2 life.
 *
 * The end-step Blood is an intervening-if trigger gated on [Conditions.YouGainedLifeThisTurn]. The
 * front's transform pays "Sacrifice two Blood tokens" via [Costs.SacrificeMultiple] over the
 * Blood-artifact filter, at sorcery speed. The back's activated ability is a single
 * [Effects.DrainLife] of 2 (each opponent loses 2, you gain 2). The back is a transformed face with
 * no mana cost, so its color comes from a color indicator (CR 204): `colorIndicator = "B"`.
 */

/** "At the beginning of your end step, if you gained life this turn, create a Blood token." */
private fun bloodOnEndStep(builder: com.wingedsheep.sdk.dsl.TriggeredAbilityBuilder) {
    builder.trigger = Triggers.YourEndStep
    builder.triggerCondition = Conditions.YouGainedLifeThisTurn
    builder.effect = Effects.CreateBlood()
    builder.description = "At the beginning of your end step, if you gained life this turn, create a " +
        "Blood token."
}

private val RestlessBloodseekerFront = card("Restless Bloodseeker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 1
    toughness = 3
    oracleText = "At the beginning of your end step, if you gained life this turn, create a Blood " +
        "token. (It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Sacrifice two Blood tokens: Transform this creature. Activate only as a sorcery."

    triggeredAbility { bloodOnEndStep(this) }

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Artifact.withSubtype("Blood"))
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71f67ac0-7901-4248-9cb7-2200fb8f893e.jpg?1783924858"
    }
}

private val BloodsoakedReveler = card("Bloodsoaked Reveler") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, if you gained life this turn, create a Blood " +
        "token. (It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "{4}{B}: Each opponent loses 2 life and you gain 2 life."

    triggeredAbility { bloodOnEndStep(this) }

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        effect = Effects.DrainLife(
            amount = 2,
            from = EffectTarget.PlayerRef(Player.EachOpponent),
            to = EffectTarget.Controller,
        )
        description = "Each opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/back/7/1/71f67ac0-7901-4248-9cb7-2200fb8f893e.jpg?1783924858"
    }
}

val RestlessBloodseeker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = RestlessBloodseekerFront,
    backFace = BloodsoakedReveler,
)
