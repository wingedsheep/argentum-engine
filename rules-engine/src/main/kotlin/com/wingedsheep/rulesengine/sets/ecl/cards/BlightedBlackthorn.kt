package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Blighted Blackthorn
 *
 * {4}{B} Creature — Treefolk Warlock 3/7
 * Whenever this creature enters or attacks, you may blight 2.
 * If you do, you draw a card and lose 1 life.
 * (To blight 2, put two -1/-1 counters on a creature you control.)
 */
object BlightedBlackthorn {
    val definition = CardDefinition.creature(
        name = "Blighted Blackthorn",
        manaCost = ManaCost.parse("{4}{B}"),
        subtypes = setOf(Subtype.TREEFOLK, Subtype.WARLOCK),
        power = 3,
        toughness = 7,
        oracleText = "Whenever this creature enters or attacks, you may blight 2. If you do, " +
                "you draw a card and lose 1 life.\n(To blight 2, put two -1/-1 counters on a creature you control.)",
        metadata = ScryfallMetadata(
            collectorNumber = "90",
            rarity = Rarity.COMMON,
            artist = "Omar Rayyan",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb4b4b4b-4b4b-4b4b-4b4b-4b4b4b4b4b4b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Blighted Blackthorn") {
        // Shared effect for both triggers
        // TODO: Blight mechanic needs implementation (put -1/-1 counters as cost)
        // For now, model as optional draw + life loss
        val blightEffect = CompositeEffect(
            effects = listOf(
                DrawCardsEffect(count = 1, target = EffectTarget.Controller),
                LoseLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
        )

        // ETB: May blight 2 to draw and lose life
        triggered(
            trigger = OnEnterBattlefield(),
            effect = blightEffect,
            optional = true
        )

        // Attack: May blight 2 to draw and lose life
        triggered(
            trigger = OnAttack(selfOnly = true),
            effect = blightEffect,
            optional = true
        )
    }
}
