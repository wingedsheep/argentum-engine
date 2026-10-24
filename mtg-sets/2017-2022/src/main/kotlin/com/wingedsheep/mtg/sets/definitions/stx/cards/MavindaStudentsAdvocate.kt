package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mavinda, Students' Advocate — Strixhaven: School of Mages #21
 * {2}{W} · Legendary Creature — Bird Advisor · 2/3
 *
 * The {0} ability grants a this-turn permission over the targeted graveyard card (it stays in the
 * graveyard, like Zul Ashur), stamps the "exile it instead" rider, and taxes the cast {8} unless the
 * spell targets a creature you control. The tax is tied to this permission ("cast this way"): it is
 * priced against the targets actually chosen and isn't owed by a flashback cast of the same card.
 */
val MavindaStudentsAdvocate = card("Mavinda, Students' Advocate") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Bird Advisor"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "{0}: You may cast target instant or sorcery card from your graveyard this turn. If that spell " +
        "doesn't target a creature you control, it costs {8} more to cast this way. If that spell would " +
        "be put into your graveyard, exile it instead. Activate only once each turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Free
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val borrowed = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = borrowed,
                expiry = MayPlayExpiry.EndOfTurn,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
            run(Effects.GrantPlayWithCostIncrease(
                from = borrowed,
                amount = 8,
                unlessTargets = GameObjectFilter.Creature.youControl(),
            ))
        }
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b3d521f-31cd-4bd2-b6b6-771c79252789.jpg?1783927388"
    }
}
