package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Panicked Bystander // Cackling Culprit (Innistrad: Crimson Vow)
 * {1}{W}
 * Creature — Human Peasant // Creature — Human Rogue
 *
 * Front — Panicked Bystander (2/2)
 *   Whenever this creature or another creature you control dies, you gain 1 life.
 *   At the beginning of your end step, if you gained 3 or more life this turn, transform this creature.
 *
 * Back — Cackling Culprit (3/5)
 *   Whenever this creature or another creature you control dies, you gain 1 life.
 *   {1}{B}: This creature gains deathtouch until end of turn.
 *
 * "This creature or another creature you control dies" is exactly [Triggers.YourCreatureDies], which
 * includes the source itself. The transform is an intervening-if end-step trigger gated on
 * [Conditions.YouGainedLifeThisTurnAtLeast] 3. The back is a transformed face with no mana cost, so
 * its color comes from a color indicator (CR 204): `colorIndicator = "B"`.
 */

private val PanickedBystanderFront = card("Panicked Bystander") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another creature you control dies, you gain 1 life.\n" +
        "At the beginning of your end step, if you gained 3 or more life this turn, transform this " +
        "creature."

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = Effects.GainLife(1)
        description = "Whenever this creature or another creature you control dies, you gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = TransformEffect(EffectTarget.Self)
        description = "At the beginning of your end step, if you gained 3 or more life this turn, " +
            "transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/031c5cff-e579-432a-bcee-864b12eb0558.jpg?1783924920"
    }
}

private val CacklingCulprit = card("Cackling Culprit") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 5
    oracleText = "Whenever this creature or another creature you control dies, you gain 1 life.\n" +
        "{1}{B}: This creature gains deathtouch until end of turn."

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = Effects.GainLife(1)
        description = "Whenever this creature or another creature you control dies, you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
        description = "This creature gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/back/0/3/031c5cff-e579-432a-bcee-864b12eb0558.jpg?1783924920"
    }
}

val PanickedBystander: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = PanickedBystanderFront,
    backFace = CacklingCulprit,
)
