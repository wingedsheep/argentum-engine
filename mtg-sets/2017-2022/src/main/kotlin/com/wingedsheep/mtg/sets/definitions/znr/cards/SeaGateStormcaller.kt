package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Sea Gate Stormcaller — Zendikar Rising #77
 * {1}{U} · Creature — Human Wizard · 2/1
 *
 * Kicker {4}{U}
 * When this creature enters, copy the next instant or sorcery spell with mana value 2 or less you
 * cast this turn when you cast it. If this creature was kicked, copy that spell twice instead. You
 * may choose new targets for the copies.
 *
 * The ETB arms one pending "copy your next spell" rider ([Effects.CopyNextSpellCast]) filtered to
 * instants and sorceries with mana value 2 or less — the filter is checked as the spell is cast, so
 * a spell that doesn't match leaves the rider waiting for the next one. Kicked, the same single
 * delayed trigger makes two copies instead of one. [WasKicked] reads the cast-cost choices captured
 * on the enters trigger, so the count holds even if the Stormcaller has left the battlefield before
 * the trigger resolves.
 */
val SeaGateStormcaller = card("Sea Gate Stormcaller") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Kicker {4}{U}\n" +
        "When this creature enters, copy the next instant or sorcery spell with mana value 2 or less " +
        "you cast this turn when you cast it. If this creature was kicked, copy that spell twice " +
        "instead. You may choose new targets for the copies."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{4}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val cheapInstantOrSorcery = GameObjectFilter.InstantOrSorcery.manaValueAtMost(2)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.CopyNextSpellCast(copies = 2, spellFilter = cheapInstantOrSorcery),
            otherwise = Effects.CopyNextSpellCast(copies = 1, spellFilter = cheapInstantOrSorcery)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "77"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e14e677-9bfa-4bcc-9772-8e7758d7538a.jpg?1783929387"
        ruling("2020-09-25", "Sea Gate Stormcaller's delayed triggered ability will copy any instant or sorcery spell, not just one with targets.")
        ruling("2020-09-25", "A copy is created even if Sea Gate Stormcaller leaves the battlefield before its delayed triggered ability is created.")
        ruling("2020-09-25", "A copy is created even if the spell that caused Sea Gate Stormcaller's delayed triggered ability to trigger has been countered by the time that ability resolves. The copy resolves before the original spell.")
        ruling("2020-09-25", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too. Notably, if the spell you copy was kicked, the copy or copies will also be kicked.")
        ruling("2020-09-25", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. If it's copied, the copy has the same value of X.")
    }
}
