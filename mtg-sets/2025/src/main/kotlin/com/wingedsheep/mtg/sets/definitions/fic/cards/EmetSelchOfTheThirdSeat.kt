package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emet-Selch of the Third Seat
 * {2}{U}{B}
 * Legendary Creature — Elder Wizard
 * 3/4
 * Spells you cast from your graveyard cost {2} less to cast.
 * Whenever one or more opponents lose life, you may cast target instant or sorcery card from your
 * graveyard. If that spell would be put into your graveyard, exile it instead. Do this only once
 * each turn.
 *
 * - The discount is Patrician Geist's `YouCastFromZones(GRAVEYARD)` shape; it applies to the cast
 *   the second ability makes too, since that cast happens from the graveyard (ruling: it reduces
 *   only generic mana).
 * - "Whenever one or more opponents lose life" is the batched `anOpponent.losesLife(batch = true)`:
 *   one trigger per life-loss occurrence however many opponents lost life in it.
 * - The cast happens while the trigger resolves, paying the card's costs (Ogre Battlecaster's
 *   [Effects.CastFromCollection] with `insteadOfGraveyard = EXILE`); timing restrictions are
 *   ignored (ruling).
 * - "Do this only once each turn" is `effectOncePerTurn` (CR 603.2h), keyed to the cast: declining
 *   the "you may" spends nothing, so a later life loss offers the cast again.
 */
val EmetSelchOfTheThirdSeat = card("Emet-Selch of the Third Seat") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Elder Wizard"
    power = 3
    toughness = 4
    oracleText = "Spells you cast from your graveyard cost {2} less to cast.\n" +
        "Whenever one or more opponents lose life, you may cast target instant or sorcery card from " +
        "your graveyard. If that spell would be put into your graveyard, exile it instead. Do this " +
        "only once each turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(setOf(Zone.GRAVEYARD)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.losesLife(batch = true)
        effectOncePerTurn = true
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollection(card, insteadOfGraveyard = AfterResolveDestination.EXILE),
                descriptionOverride = "You may cast target instant or sorcery card from your graveyard.",
            ))
        }
        description = "Whenever one or more opponents lose life, you may cast target instant or " +
            "sorcery card from your graveyard. If that spell would be put into your graveyard, exile " +
            "it instead. Do this only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd3141de-633e-48e3-a89f-a91339f5e882.jpg?1783906347"
        ruling("2025-06-06", "You choose whether to cast the target card while Emet-Selch of the Third Seat's second ability is still on the stack. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2025-06-06", "The cost reduction applies only to generic mana in the total cost of spells you cast from your graveyard.")
    }
}
