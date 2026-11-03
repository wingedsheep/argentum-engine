package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tell Me What You See
 * {1}{B}
 * Enchantment
 *
 * When this enchantment enters, target opponent may have you draw a card. If they don't, they lose
 * 4 life and you surveil 1.
 * Whenever you face a dilemma, you may return this enchantment to its owner's hand. (You face a
 * dilemma as you choose one or more modes for a spell or ability.)
 *
 * The punisher choice is an [Effects.May] whose `decisionMaker` is the targeted opponent: only the
 * yes/no is delegated, so the draw and the surveil stay the controller's, while the 4 life loss
 * lands on the opponent through the `otherwise` branch.
 */
val TellMeWhatYouSee = card("Tell Me What You See") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, target opponent may have you draw a card. If they " +
        "don't, they lose 4 life and you surveil 1.\n" +
        "Whenever you face a dilemma, you may return this enchantment to its owner's hand. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.May(
            effect = Effects.DrawCards(1, EffectTarget.Controller),
            otherwise = Effects.LoseLife(4, opponent) then Effects.Surveil(1),
            decisionMaker = opponent,
            prompt = "Let your opponent draw a card? If you don't, you lose 4 life and they surveil 1.",
            descriptionOverride = "Target opponent may have you draw a card. If they don't, they " +
                "lose 4 life and you surveil 1."
        )
        description = "When this enchantment enters, target opponent may have you draw a card. If they " +
            "don't, they lose 4 life and you surveil 1."
    }

    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.May(Effects.ReturnToHand(EffectTarget.Self))
        description = "Whenever you face a dilemma, you may return this enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Douglas Shuler"
        flavorText = "\"I don't understand how you can be so mistaken.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e339c8fb-26d2-4514-aeb7-014274df0c9c.jpg?1793013584"
    }
}
