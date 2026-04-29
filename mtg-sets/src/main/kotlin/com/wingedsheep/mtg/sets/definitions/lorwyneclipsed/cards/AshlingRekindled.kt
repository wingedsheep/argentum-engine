package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorForTargetEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ashling, Rekindled // Ashling, Rimebound
 * {1}{R}
 * Legendary Creature — Elemental Sorcerer // Legendary Creature — Elemental Wizard (transform)
 * 1/3 // 1/3
 *
 * Front — Ashling, Rekindled
 *   Whenever this creature enters or transforms into Ashling, Rekindled, you may discard a card.
 *   If you do, draw a card.
 *   At the beginning of your first main phase, you may pay {U}. If you do, transform Ashling.
 *
 * Back — Ashling, Rimebound
 *   Whenever this creature transforms into Ashling, Rimebound and at the beginning of your first
 *   main phase, add two mana of any one color. Spend this mana only to cast spells with mana
 *   value 4 or greater.
 *   At the beginning of your first main phase, you may pay {R}. If you do, transform Ashling.
 */

private val rummageMay = MayEffect(
    effect = EffectPatterns.rummage(1),
    description_override = "You may discard a card. If you do, draw a card."
)

private val addRimeboundMana = CompositeEffect(
    listOf(
        ChooseColorForTargetEffect(
            target = EffectTarget.Self,
            prompt = "Choose a color for Ashling's mana"
        ),
        AddManaOfChosenColorEffect(
            amount = 2,
            restriction = ManaRestriction.SpellsMV4OrGreater
        )
    )
)

private val AshlingRimebound = card("Ashling, Rimebound") {
    manaCost = ""
    typeLine = "Legendary Creature — Elemental Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature transforms into Ashling, Rimebound and at the beginning " +
        "of your first main phase, add two mana of any one color. Spend this mana only to cast " +
        "spells with mana value 4 or greater.\n" +
        "At the beginning of your first main phase, you may pay {R}. If you do, transform Ashling."

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        effect = addRimeboundMana
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = addRimeboundMana
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{R}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/back/7/d/7d7faefe-9c0d-45b6-8ea4-5fa666762a2c.jpg?1759144841"
    }
}

private val AshlingRekindledFront = card("Ashling, Rekindled") {
    manaCost = "{1}{R}"
    typeLine = "Legendary Creature — Elemental Sorcerer"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature enters or transforms into Ashling, Rekindled, you may " +
        "discard a card. If you do, draw a card.\n" +
        "At the beginning of your first main phase, you may pay {U}. If you do, transform Ashling."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = rummageMay
    }

    triggeredAbility {
        trigger = Triggers.TransformsToFront
        effect = rummageMay
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{U}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d7faefe-9c0d-45b6-8ea4-5fa666762a2c.jpg?1759144841"
    }
}

val AshlingRekindled: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AshlingRekindledFront,
    backFace = AshlingRimebound
)
