package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val GIDEON_ALLY_EMBLEM = "Creatures you control get +1/+1."

/**
 * Gideon, Ally of Zendikar - {2}{W}{W}
 * Legendary Planeswalker — Gideon
 * Starting Loyalty: 4
 *
 * +1: Until end of turn, Gideon becomes a 5/5 Human Soldier Ally creature with indestructible
 *     that's still a planeswalker. Prevent all damage that would be dealt to him this turn.
 * 0: Create a 2/2 white Knight Ally creature token.
 * −4: You get an emblem with "Creatures you control get +1/+1."
 *
 * The +1 is the Gideon of the Trials animate/prevent shape at 5/5 with Ally added; the emblem is
 * Elspeth, Sun's Champion's anthem emblem at +1/+1.
 */
val GideonAllyOfZendikar = card("Gideon, Ally of Zendikar") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 4
    oracleText = "+1: Until end of turn, Gideon becomes a 5/5 Human Soldier Ally creature with indestructible " +
        "that's still a planeswalker. Prevent all damage that would be dealt to him this turn.\n" +
        "0: Create a 2/2 white Knight Ally creature token.\n" +
        "−4: You get an emblem with \"$GIDEON_ALLY_EMBLEM\""

    // +1: Until end of turn, Gideon becomes a 5/5 Human Soldier Ally creature with indestructible
    //     that's still a planeswalker. Prevent all damage that would be dealt to him this turn.
    loyaltyAbility(+1) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 5,
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            creatureTypes = setOf("Human", "Soldier", "Ally"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
        description = "Until end of turn, Gideon becomes a 5/5 Human Soldier Ally creature with indestructible " +
            "that's still a planeswalker. Prevent all damage that would be dealt to him this turn."
    }

    // 0: Create a 2/2 white Knight Ally creature token.
    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight", "Ally"),
            imageUri = "https://cards.scryfall.io/normal/front/0/4/0419a202-6e32-4f0a-a032-72f6c00cae5e.jpg?1783938120",
        )
        description = "Create a 2/2 white Knight Ally creature token."
    }

    // −4: You get an emblem with "Creatures you control get +1/+1."
    loyaltyAbility(-4) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            powerBonus = 1,
            toughnessBonus = 1,
            emblemDescription = GIDEON_ALLY_EMBLEM,
        )
        description = "You get an emblem with \"$GIDEON_ALLY_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "29"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/187e887c-c39d-4d25-a506-cdc95fc70316.jpg?1783938219"
        ruling("2015-08-25", "Gideon's first ability doesn't count as a creature entering the battlefield. Gideon was already on the battlefield; he only changed his types. Specifically, your rally abilities won't trigger. (Of course, Gideon's second ability will cause rally abilities to trigger.)")
        ruling("2015-08-25", "If Gideon becomes a creature the same turn he enters the battlefield, you can't attack with him or use any of his {T} abilities (if he gains any).")
        ruling("2015-08-25", "Gideon's first ability causes him to become a creature with the creature types Human, Soldier, and Ally. He remains a planeswalker with the planeswalker type Gideon. (He also retains any other card types or subtypes he may have had.) Each subtype is correlated to the proper card type: planeswalker is only a type (not a creature type), and Human, Soldier, and Ally are just creature types (not planeswalker types).")
        ruling("2015-08-25", "If damage that can't be prevented is dealt to Gideon after his first ability has resolved, that damage will have all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). Even though he has indestructible, if Gideon has no loyalty counters on him, he's put into his owner's graveyard.")
    }
}
