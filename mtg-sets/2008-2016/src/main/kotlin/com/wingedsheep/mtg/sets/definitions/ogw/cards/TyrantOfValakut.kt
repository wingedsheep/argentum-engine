package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Tyrant of Valakut
 * {5}{R}{R}
 * Creature — Dragon
 * 5/4
 * Surge {3}{R}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Flying
 * When this creature enters, if its surge cost was paid, it deals 3 damage to any target.
 *
 * The enters trigger's intervening "if" reads [Conditions.SurgeCostWasPaid], the flag the engine
 * stamps on the permanent when it was cast for its surge cost (CR 702.117a).
 */
val TyrantOfValakut = card("Tyrant of Valakut") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 4
    oracleText = "Surge {3}{R}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Flying\n" +
        "When this creature enters, if its surge cost was paid, it deals 3 damage to any target."

    keywordAbility(KeywordAbility.surge("{3}{R}{R}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SurgeCostWasPaid
        val damageTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, damageTarget)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f388be9e-332e-4199-9f30-862d631fa26b.jpg?1783937905"
    }
}
