package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bard's Company
 * {2}{W}{U}
 * Creature — Human Citizen
 * 2/3
 *
 * You may cast this spell as though it had flash if you control a Human.
 * Other creatures you control get +1/+1.
 * Whenever this creature enters or attacks, recruit.
 *
 * Three separate clauses, each on its own existing primitive:
 *
 * - The flash clause is card-level [conditionalFlash], not a battlefield static — the permission has
 *   to be readable while the card is still in hand, which is exactly what that field is for
 *   (Illusion Spinners, Colossal Rattlewurm). "A Human" is any permanent with the subtype, so the
 *   filter is [GameObjectFilter.Permanent], not `Creature`; a noncreature permanent that has been
 *   made a Human still turns the clause on.
 * - The anthem is a plain [ModifyStats] over other creatures you control (Benalish Marshal).
 * - "Enters or attacks" is two triggered abilities, the established shape here (Sentinel of the
 *   Nameless City) — one event each, so a creature that enters attacking would fire both, as CR
 *   requires.
 */
val BardsCompany = card("Bard's Company") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Citizen"
    oracleText = "You may cast this spell as though it had flash if you control a Human.\n" +
        "Other creatures you control get +1/+1.\n" +
        "Whenever this creature enters or attacks, recruit. (Draw a card, then discard a card. " +
        "If you discarded a nonland card, create a 1/1 white Human Soldier creature token.)"
    power = 2
    toughness = 3

    conditionalFlash = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.OtherCreaturesYouControl
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Mechanic.recruit()
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d14aa2ff-7bbd-47a6-8e36-481e56302a62.jpg?1785152250"
    }
}
