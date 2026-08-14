package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Hamlet Glutton
 * {5}{G}{G}
 * Creature — Giant
 * 6/6
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * This spell costs {2} less to cast if it's bargained.
 * Trample
 * When this creature enters, you gain 3 life.
 *
 * The **cost-gate** shape of bargain (CR 702.166): unlike [HighFaeNegotiator] and
 * [AgathasChampion], whose payoff is an enters trigger, the Glutton's whole reward is a cheaper
 * cast. That's a `SelfCast` [ModifySpellCost] with `ReduceGeneric(2)` gated by
 * `CostGating.OnlyIf(`[Conditions.WasBargained]`)`, so the reduction is priced against the
 * bargained cast branch specifically — the legal-action enumerator offers the plain cast at
 * {5}{G}{G} and the bargained cast at {3}{G}{G} plus the sacrifice.
 *
 * Per the Johann's Stopgap ruling that applies equally here, the reduction changes only the total
 * cost paid: the Glutton's mana cost and mana value stay {5}{G}{G} / 7 no matter how it was cast.
 *
 * The enters trigger is *unconditional* — the life gain happens on every cast, bargained or not, so
 * it carries no [Conditions.WasBargained] intervening-if.
 */
val HamletGlutton = card("Hamlet Glutton") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "This spell costs {2} less to cast if it's bargained.\n" +
        "Trample\n" +
        "When this creature enters, you gain 3 life."

    bargain()

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.WasBargained),
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(3)
        description = "When this creature enters, you gain 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ec5544-c138-44bb-a807-5798313c9a50.jpg?1783915080"

        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "You can bargain a permanent spell even if you won't be able to choose targets for an " +
                "enters-the-battlefield ability of that permanent once the spell resolves."
        )
        ruling(
            "2023-09-01",
            "If a card or token enters the battlefield as a copy of a permanent that's already on " +
                "the battlefield, the new permanent isn't bargained, even if the original was."
        )
    }
}
