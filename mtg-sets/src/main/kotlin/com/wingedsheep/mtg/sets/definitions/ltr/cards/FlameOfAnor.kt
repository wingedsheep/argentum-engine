package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flame of Anor
 * {1}{U}{R}
 * Instant
 *
 * Choose one. If you control a Wizard as you cast this spell, you may choose two instead.
 * • Target player draws two cards.
 * • Destroy target artifact.
 * • Flame of Anor deals 5 damage to target creature.
 *
 * The conditional modal count is a cast-time `dynamicChooseCount`: the floor stays
 * `minChooseCount = 1` ("choose one" is mandatory), and the cap is 2 when you control a
 * Wizard as you cast the spell, otherwise 1 — evaluated against the battlefield at cast
 * time by [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler].
 */
val FlameOfAnor = card("Flame of Anor") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Choose one. If you control a Wizard as you cast this spell, you may choose two instead.\n" +
        "• Target player draws two cards.\n" +
        "• Destroy target artifact.\n" +
        "• Flame of Anor deals 5 damage to target creature."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmount.Conditional(
                condition = Conditions.YouControlAtLeast(1, GameObjectFilter.Creature.withSubtype("Wizard")),
                ifTrue = DynamicAmount.Fixed(2),
                ifFalse = DynamicAmount.Fixed(1)
            )
        ) {
            mode("Target player draws two cards") {
                val player = target("target player", Targets.Player)
                effect = Effects.DrawCards(2, player)
            }
            mode("Destroy target artifact") {
                val artifact = target("target artifact", Targets.Artifact)
                effect = Effects.Destroy(artifact)
            }
            mode("Flame of Anor deals 5 damage to target creature") {
                val creature = target("target creature", Targets.Creature)
                effect = Effects.DealDamage(5, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Yigit Koroglu"
        flavorText = "There was a ringing clash and a stab of white fire, and the Balrog fell back."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04779a7e-b453-48b9-b392-6d6fd0b8d283.jpg?1686969766"
    }
}
