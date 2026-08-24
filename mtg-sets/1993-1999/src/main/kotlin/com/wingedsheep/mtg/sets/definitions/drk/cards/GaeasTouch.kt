package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gaea's Touch
 * {G}{G}
 * Enchantment
 * {0}: You may put a basic Forest card from your hand onto the battlefield. Activate only as a
 * sorcery and only once each turn.
 * Sacrifice this enchantment: Add {G}{G}.
 *
 * The Forest is **put onto the battlefield**, not played — it costs no land drop, which is the
 * whole reason the card is worth {G}{G}. `Patterns.Hand.putFromHand` with a `ChooseUpTo(1)`
 * selection is exactly the "you may": with no basic Forest in hand it is a silent no-op rather than
 * a failed activation.
 *
 * The two printed clauses of that ability map to two separate knobs — `TimingRule.SorcerySpeed`
 * for "only as a sorcery", `ActivationRestriction.OncePerTurn` for "only once each turn" — because
 * they restrict different things and an effect that lifted one would leave the other standing.
 *
 * The second ability is a mana ability (CR 605.1a: no target, not a loyalty ability, and it could
 * add mana), so it resolves without using the stack. Sacrificing the enchantment is its cost, which
 * is what makes the card a one-shot ramp *or* a Dark Ritual, never both.
 */
val GaeasTouch = card("Gaea's Touch") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{0}: You may put a basic Forest card from your hand onto the battlefield. " +
        "Activate only as a sorcery and only once each turn.\n" +
        "Sacrifice this enchantment: Add {G}{G}."

    activatedAbility {
        cost = Costs.Free
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST),
            count = 1,
            prompt = "Put a basic Forest onto the battlefield?",
        )
        description = "{0}: You may put a basic Forest card from your hand onto the battlefield. " +
            "Activate only as a sorcery and only once each turn."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        manaAbility = true
        effect = Effects.AddMana(Color.GREEN, 2)
        description = "Sacrifice this enchantment: Add {G}{G}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e1ae3d6-6d96-4db6-bbc4-cee91bae6cf7.jpg?1783947932"
    }
}
