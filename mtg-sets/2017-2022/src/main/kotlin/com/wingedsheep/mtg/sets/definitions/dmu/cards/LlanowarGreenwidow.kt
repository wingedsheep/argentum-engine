package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Llanowar Greenwidow — Dominaria United #169
 * {2}{G} · Creature — Spider · 4/3 · Rare
 *
 * Reach, trample
 * Domain — {7}{G}: Return this card from your graveyard to the battlefield tapped. It gains "If
 * this permanent would leave the battlefield, exile it instead of putting it anywhere else." This
 * ability costs {1} less to activate for each basic land type among lands you control.
 *
 * The graveyard recursion is Ghoulsteed's shape (graveyard-guarded, tapped); the granted
 * replacement is [Effects.GrantExileOnLeave] on the returned card, as on Molten Gatekeeper's
 * unearth. Both sit behind the same "still in the graveyard" gate so a card that left in response
 * neither returns nor picks up the replacement. The domain discount rides `genericCostReduction`
 * (Battlefield Butcher / Boseiju's shape), so only the generic {7} shrinks — {G} is always paid.
 */
val LlanowarGreenwidow = card("Llanowar Greenwidow") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 4
    toughness = 3
    oracleText = "Reach, trample\n" +
        "Domain — {7}{G}: Return this card from your graveyard to the battlefield tapped. It gains " +
        "\"If this permanent would leave the battlefield, exile it instead of putting it anywhere " +
        "else.\" This ability costs {1} less to activate for each basic land type among lands you control."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{7}{G}")
        activateFromZone = Zone.GRAVEYARD
        genericCostReduction = DynamicAmounts.domain()
        effect = Effects.If(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true) then
                Effects.GrantExileOnLeave(EffectTarget.Self)
        )
        description = "Domain — {7}{G}: Return this card from your graveyard to the battlefield tapped. " +
            "It gains \"If this permanent would leave the battlefield, exile it instead of putting it " +
            "anywhere else.\" This ability costs {1} less to activate for each basic land type among " +
            "lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81f90e4d-f60f-4175-830f-c2ce04c5a945.jpg?1783921300"
        ruling("2022-09-09", "Domain abilities count the number of basic land types among lands you control, not how many lands you control or how many of any type.")
    }
}
