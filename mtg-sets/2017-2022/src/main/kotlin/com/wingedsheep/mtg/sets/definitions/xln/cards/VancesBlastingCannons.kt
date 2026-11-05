package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vance's Blasting Cannons // Spitfire Bastion (Ixalan — the card's earliest printing)
 * {3}{R}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Vance's Blasting Cannons (Legendary Enchantment, {3}{R})
 *   At the beginning of your upkeep, exile the top card of your library. If it's a nonland card,
 *   you may cast that card this turn.
 *   Whenever you cast your third spell in a turn, you may transform Vance's Blasting Cannons.
 *
 * Back — Spitfire Bastion (Legendary Land)
 *   {T}: Add {R}.
 *   {2}{R}, {T}: Spitfire Bastion deals 3 damage to any target.
 *
 * Implementation:
 *  - The upkeep exile is Hidetsugu, Devouring Chaos's gather → exile → grant pipeline, with a
 *    cast-only (`nonLandOnly`) permission: an exiled land gets no way to be played, and a
 *    nonland card may be cast this turn under its normal timing and costs. Uncast, it stays in
 *    exile (the permission simply expires).
 *  - The flip is Emeritus of Conflict's `Triggers.you.castsNth(3)`, which reads the turn's spell
 *    count — so spells cast before the Cannons entered, and the Cannons itself, count — wrapping
 *    an optional [Effects.Transform] on Self.
 */

private val VancesBlastingCannonsFront = card("Vance's Blasting Cannons") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment"
    oracleText = "At the beginning of your upkeep, exile the top card of your library. If it's a " +
        "nonland card, you may cast that card this turn.\n" +
        "Whenever you cast your third spell in a turn, you may transform Vance's Blasting Cannons."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1), name = "vancesCannonsExiled")
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(exiled, MayPlayExpiry.EndOfTurn, nonLandOnly = true))
        }
        description = "At the beginning of your upkeep, exile the top card of your library. If " +
            "it's a nonland card, you may cast that card this turn."
    }

    triggeredAbility {
        trigger = Triggers.you.castsNth(3)
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
        description = "Whenever you cast your third spell in a turn, you may transform Vance's " +
            "Blasting Cannons."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Jonas De Ro"
        flavorText = "With a broadside assault, Captain Vance carved the cliffs to form her new home."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e8c0009-787f-480b-84b6-bf297f1fb466.jpg?1783935737"
        ruling(
            "2017-09-29",
            "The second ability of Vance's Blasting Cannons counts all the spells you've cast, " +
                "including Vance's Blasting Cannons itself if you cast it this turn. The ability " +
                "won't trigger unless Vance's Blasting Cannons is on the battlefield as you cast " +
                "your third spell of the turn.",
        )
        ruling(
            "2017-09-29",
            "Casting the exiled card follows the normal rules for casting that card. You must pay " +
                "its costs, and you must follow all applicable timing rules. For example, if you " +
                "exile a creature card this way, you must wait until your main phase to cast it.",
        )
        ruling("2017-09-29", "If you don't cast the exiled card, it remains in exile.")
        ruling(
            "2017-09-29",
            "The second ability of Vance's Blasting Cannons resolves before the spell that caused " +
                "it to trigger. The ability will resolve even if that spell is countered.",
        )
    }
}

private val SpitfireBastion = card("Spitfire Bastion") {
    manaCost = ""
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Vance's Blasting Cannons.)\n" +
        "{T}: Add {R}.\n" +
        "{2}{R}, {T}: Spitfire Bastion deals 3 damage to any target."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {R}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(3, any)
        description = "{2}{R}, {T}: Spitfire Bastion deals 3 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Jonas De Ro"
        flavorText = "Now instead of a ship, Ripley Vance commands a fortress."
        imageUri = "https://cards.scryfall.io/normal/back/9/e/9e8c0009-787f-480b-84b6-bf297f1fb466.jpg?1783935737"
    }
}

val VancesBlastingCannons: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = VancesBlastingCannonsFront,
    backFace = SpitfireBastion,
)
