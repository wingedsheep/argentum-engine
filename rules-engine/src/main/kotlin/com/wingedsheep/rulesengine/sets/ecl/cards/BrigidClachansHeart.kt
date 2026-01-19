package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AddDynamicManaEffect
import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.OnFirstMainPhase
import com.wingedsheep.rulesengine.ability.OnTransform
import com.wingedsheep.rulesengine.ability.TransformEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.core.Supertype

/**
 * Brigid, Clachan's Heart // Brigid, Doun's Mind
 *
 * A double-faced transforming creature from Lorwyn Eclipsed.
 *
 * Front Face (Brigid, Clachan's Heart):
 * - {2}{W} Legendary Creature — Kithkin Warrior 3/2
 * - When this creature enters or transforms into this creature, create a 1/1 green and white Kithkin token
 * - At the beginning of your first main phase, you may pay {G}. If you do, transform this creature.
 *
 * Back Face (Brigid, Doun's Mind):
 * - Legendary Creature — Kithkin Soldier 3/2
 * - {T}: Add X mana in any combination of {G} and/or {W}, where X is the number of other creatures you control.
 * - At the beginning of your first main phase, you may pay {W}. If you do, transform this creature.
 */
object BrigidClachansHeart {

    // Back face definition (Brigid, Doun's Mind)
    private val backFaceDefinition = CardDefinition.creature(
        name = "Brigid, Doun's Mind",
        manaCost = ManaCost.ZERO,  // Back faces have no mana cost
        subtypes = setOf(Subtype.KITHKIN, Subtype.SOLDIER),
        power = 3,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = "{T}: Add X mana in any combination of {G} and/or {W}, " +
                "where X is the number of other creatures you control.\n" +
                "At the beginning of your first main phase, you may pay {W}. " +
                "If you do, transform this creature."
    )

    // Front face definition (Brigid, Clachan's Heart)
    private val frontFaceDefinition = CardDefinition.creature(
        name = "Brigid, Clachan's Heart",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.WARRIOR),
        power = 3,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = "When this creature enters or transforms into Brigid, Clachan's Heart, " +
                "create a 1/1 green and white Kithkin creature token.\n" +
                "At the beginning of your first main phase, you may pay {G}. " +
                "If you do, transform this creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "7",
            rarity = Rarity.RARE,
            artist = "Zoltan Boros",
            imageUri = "https://cards.scryfall.io/normal/front/c/b/cb7d5bbb-4f68-4e38-8bb0-a95af21b24c8.jpg",
            scryfallId = "cb7d5bbb-4f68-4e38-8bb0-a95af21b24c8",
            releaseDate = "2026-01-23"
        )
    )

    // Combined double-faced card definition
    val definition = CardDefinition.doubleFacedCreature(
        frontFace = frontFaceDefinition,
        backFace = backFaceDefinition
    )

    // Script for the front face (Brigid, Clachan's Heart)
    val script = cardScript("Brigid, Clachan's Heart") {
        // ETB: Create a 1/1 green and white Kithkin creature token
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            )
        )

        // Also triggers when transforming into this face (front)
        triggered(
            trigger = OnTransform(selfOnly = true, intoBackFace = false),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            )
        )

        // At the beginning of your first main phase, you may pay {G} to transform
        // This is an optional triggered ability with a mana cost condition
        // Note: Full implementation requires player prompt for payment decision
        triggered(
            trigger = OnFirstMainPhase(controllerOnly = true),
            effect = TransformEffect(EffectTarget.Self),
            optional = true  // "you may pay {G}" - payment handling requires player prompt
        )
    }

    // Script for the back face (Brigid, Doun's Mind)
    val backScript = cardScript("Brigid, Doun's Mind") {
        // Mana ability: {T}: Add X mana in any combination of {G} and/or {W}
        // where X is the number of other creatures you control
        activated(
            cost = AbilityCost.Tap,
            effect = AddDynamicManaEffect(
                amountSource = DynamicAmount.OtherCreaturesYouControl,
                allowedColors = setOf(Color.GREEN, Color.WHITE)
            ),
            isManaAbility = true
        )

        // At the beginning of your first main phase, you may pay {W} to transform
        triggered(
            trigger = OnFirstMainPhase(controllerOnly = true),
            effect = TransformEffect(EffectTarget.Self),
            optional = true  // "you may pay {W}" - payment handling requires player prompt
        )
    }
}
