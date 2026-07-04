package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Secret of Bloodbending
 * {U}{U}{U}{U}
 * Sorcery — Lesson
 * As an additional cost to cast this spell, you may waterbend {10}.
 * You control target opponent during their next combat phase. If this spell's additional cost was
 * paid, you control that player during their next turn instead. (You see all cards that player could
 * see and make all decisions for them.)
 * Exile Secret of Bloodbending.
 *
 * A Mindslaver variant: the base mode hijacks only the opponent's next combat phase
 * ([Effects.HijackNextCombatPhase]); paying the optional waterbend upgrades it to their whole next
 * turn ([Effects.HijackNextTurn]). The branch reads [Conditions.WaterbendWasPaid], stamped by the
 * optional [waterbendCost]. Like every Lesson, the card exiles itself on resolution ([selfExile]).
 */
val SecretOfBloodbending = card("Secret of Bloodbending") {
    manaCost = "{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Lesson"
    oracleText = "As an additional cost to cast this spell, you may waterbend {10}.\n" +
        "You control target opponent during their next combat phase. If this spell's additional " +
        "cost was paid, you control that player during their next turn instead. (You see all cards " +
        "that player could see and make all decisions for them.)\n" +
        "Exile Secret of Bloodbending."

    waterbendCost(amount = 10, optional = true)

    spell {
        val opponent = target("target opponent", TargetOpponent())
        selfExile()
        effect = ConditionalEffect(
            condition = Conditions.WaterbendWasPaid,
            effect = Effects.HijackNextTurn(opponent),
            elseEffect = Effects.HijackNextCombatPhase(opponent)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "69"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bb928ae-f636-4aee-9146-a7885e6a8976.jpg?1764120426"
    }
}
