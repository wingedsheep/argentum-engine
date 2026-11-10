package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bearer of Silence
 * {1}{B}
 * Creature — Eldrazi
 * 2/1
 * Devoid (This card has no color.)
 * When you cast this spell, you may pay {1}{C}. If you do, target opponent sacrifices a creature.
 * Flying
 * This creature can't block.
 *
 * The cast trigger resolves while the spell is still on the stack, so the {1}{C} is a cost paid as
 * an effect resolves — mana from Cultivator Drone may pay it (2016-01-22 ruling).
 */
val BearerOfSilence = card("Bearer of Silence") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, you may pay {1}{C}. If you do, target opponent sacrifices a creature. " +
        "({C} represents colorless mana.)\n" +
        "Flying\n" +
        "This creature can't block."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val opponent = target(Targets.Opponent)
        effect = Effects.MayPay(
            ManaCost.parse("{1}{C}"),
            Effects.Sacrifice(GameObjectFilter.Creature, target = opponent),
        )
        description = "When you cast this spell, you may pay {1}{C}. If you do, target opponent sacrifices a creature."
    }

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50460cce-ee1c-4384-bb0c-d90616e0d2e9.jpg?1783937916"
    }
}
