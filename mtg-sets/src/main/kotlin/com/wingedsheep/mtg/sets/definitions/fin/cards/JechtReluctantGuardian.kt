package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jecht, Reluctant Guardian // Braska's Final Aeon
 * {3}{B} — Legendary Creature — Human Warrior 4/3
 * //  — Legendary Enchantment Creature — Saga Nightmare 7/7
 *
 * Front — Jecht, Reluctant Guardian:
 *   Menace
 *   Whenever Jecht deals combat damage to a player, you may exile it, then return it to the
 *   battlefield transformed under its owner's control.
 *
 * Back — Braska's Final Aeon (Summon Saga that stays as a creature, then is sacrificed):
 *   (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 *   I, II — Jecht Beam — Each opponent discards a card and you draw a card.
 *   III — Ultimate Jecht Shot — Each opponent sacrifices two creatures of their choice.
 *   Menace
 */
private val BraskasFinalAeon = card("Braska's Final Aeon") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — Saga Nightmare"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Jecht Beam — Each opponent discards a card and you draw a card.\n" +
        "III — Ultimate Jecht Shot — Each opponent sacrifices two creatures of their choice.\n" +
        "Menace"
    power = 7
    toughness = 7

    keywords(Keyword.MENACE)

    // I, II — Jecht Beam — Each opponent discards a card and you draw a card.
    val jechtBeam = Effects.Composite(
        Effects.Discard(1, EffectTarget.PlayerRef(Player.EachOpponent)),
        Effects.DrawCards(1),
    )
    sagaChapter(1) { effect = jechtBeam }
    sagaChapter(2) { effect = jechtBeam }

    // III — Ultimate Jecht Shot — Each opponent sacrifices two creatures of their choice.
    sagaChapter(3) {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            2,
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Michael MacRae"
        imageUri = "https://cards.scryfall.io/normal/back/4/e/4ec91fe8-b3da-47fa-b45e-94b62a260aba.jpg?1748707810"
    }
}

private val JechtReluctantGuardianFront = card("Jecht, Reluctant Guardian") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "Menace\n" +
        "Whenever Jecht deals combat damage to a player, you may exile it, then return it to the " +
        "battlefield transformed under its owner's control."
    power = 4
    toughness = 3

    keywords(Keyword.MENACE)

    // Whenever Jecht deals combat damage to a player, you may exile-and-return-transformed.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(Effects.ExileAndReturnTransformed())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Michael MacRae"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ec91fe8-b3da-47fa-b45e-94b62a260aba.jpg?1748707810"
    }
}

val JechtReluctantGuardian: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = JechtReluctantGuardianFront,
    backFace = BraskasFinalAeon,
)
