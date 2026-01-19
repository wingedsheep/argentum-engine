package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnBecomesTapped
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.RemoveCountersEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Encumbered Reejerey
 *
 * {1}{W} Creature — Merfolk Soldier 5/4
 * This creature enters with three -1/-1 counters on it.
 * Whenever this creature becomes tapped while it has a -1/-1 counter on it,
 * remove a -1/-1 counter from it.
 */
object EncumberedReejerey {
    val definition = CardDefinition.creature(
        name = "Encumbered Reejerey",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.SOLDIER),
        power = 5,
        toughness = 4,
        oracleText = "This creature enters with three -1/-1 counters on it.\n" +
                "Whenever this creature becomes tapped while it has a -1/-1 counter on it, " +
                "remove a -1/-1 counter from it.",
        metadata = ScryfallMetadata(
            collectorNumber = "14",
            rarity = Rarity.UNCOMMON,
            artist = "Jeff Miracola",
            flavorText = "A merrow's value to their school is heavily determined by the wealth they're able to bring back.",
            imageUri = "https://cards.scryfall.io/normal/front/1/5/15ff6797-f59c-4333-98ea-5711150fd5b8.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Encumbered Reejerey") {
        // ETB: Enter with three -1/-1 counters
        triggered(
            trigger = OnEnterBattlefield(),
            effect = AddCountersEffect(
                counterType = "-1/-1",
                count = 3,
                target = EffectTarget.Self
            )
        )

        // Whenever this creature becomes tapped while it has a -1/-1 counter,
        // remove a -1/-1 counter from it
        triggered(
            trigger = OnBecomesTapped(selfOnly = true),
            effect = RemoveCountersEffect(
                counterType = "-1/-1",
                count = 1,
                target = EffectTarget.Self
            )
            // Note: Full implementation needs condition "while it has a -1/-1 counter"
        )
    }
}
