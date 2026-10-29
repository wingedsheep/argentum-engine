package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Commandeer — Coldsnap #29 (canonical printing)
 * {5}{U}{U} · Instant
 *
 * You may exile two blue cards from your hand rather than pay this spell's mana cost.
 * Gain control of target noncreature spell. You may choose new targets for it.
 *
 * The pitch cost is Misdirection's [SelfAlternativeCost] shape with two blue cards. The steal is
 * Invert Polarity's won branch: `GainControl` aimed at a spell rewrites the stack object's
 * controller, so the retarget that follows is judged from your perspective (second ruling) and a
 * stolen permanent spell enters under your control, while a stolen instant or sorcery still goes
 * to its owner's graveyard.
 */
val Commandeer = card("Commandeer") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may exile two blue cards from your hand rather than pay this spell's mana cost.\n" +
        "Gain control of target noncreature spell. You may choose new targets for it. (If that " +
        "spell is an artifact, enchantment, or planeswalker, the permanent enters under your control.)"

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ExileCards(
                count = 2,
                filter = GameObjectFilter.Any.withColor(Color.BLUE),
                fromZone = CostZone.HAND
            )
        )
    )

    spell {
        val spell = target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.GainControl(spell) then Effects.ChangeTriggeringObjectTargets(spell = spell)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e6e6204-c622-4efe-ac4f-8195528cec9c.jpg?1783943362"
        ruling("2007-02-01", "If you Commandeer a spell for which buyback has been paid, the card returns to its owner's hand.")
        ruling("2006-07-15", "After Commandeer resolves, you control the targeted spell. Any instance of \"you\" in that spell's text now refers to you, \"an opponent\" refers to one of your opponents, and so on. The change of control happens before new targets are chosen, so any targeting restrictions such as \"target opponent\" or \"target creature you control\" are now made in reference to you, not the spell's original controller. You may either change those targets to be legal in reference to you, or, if those are the spell's only targets, the spell doesn't resolve for having illegal targets. When the spell resolves, any illegal targets are unaffected by it and you make all decisions the spell's effect calls for.")
        ruling("2006-07-15", "You may change any of the targeted spell's targets. If you change a target, you must choose a legal target for the spell. If you can't, you must leave the target the same (even if that target is now illegal).")
        ruling("2006-07-15", "If the targeted spell has a triggered ability that copies it (for example, replicate or storm), the copies will be controlled by the player who cast that spell.")
        ruling("2006-07-15", "If you gain control of an instant or sorcery spell with Commandeer, it will still be put into its owner's graveyard when it resolves.")
        ruling("2006-07-15", "If you don't have two cards of the right color in your hand, you can't choose to cast the spell using the alternative cost.")
        ruling("2006-07-15", "You can't exile a card from your hand to pay for itself. At the time you would pay costs, that card is on the stack, not in your hand.")
        ruling("2006-07-15", "You may pay the alternative cost rather than the card's mana cost. Any additional costs are paid as normal.")
    }
}
