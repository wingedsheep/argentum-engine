package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Efreet Flamepainter — Strixhaven: School of Mages #98 (canonical printing)
 * {3}{R} · Creature — Efreet Shaman · 1/4
 *
 * Double strike
 * Whenever this creature deals combat damage to a player, you may cast target instant or sorcery
 * card from your graveyard without paying its mana cost. If that spell would be put into your
 * graveyard, exile it instead.
 *
 * Torrential Gearhulk's resolution-time free cast ([Effects.CastFromCollectionWithoutPayingCost]
 * with the exile-instead rider) on a combat-damage trigger, targeting with
 * [TargetFilter.InstantOrSorceryInYourGraveyard]. Double strike makes it trigger once per combat
 * damage step. The cast happens during resolution — it can't be saved for later (ruling).
 */
val EfreetFlamepainter = card("Efreet Flamepainter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Efreet Shaman"
    power = 1
    toughness = 4
    oracleText = "Double strike\n" +
        "Whenever this creature deals combat damage to a player, you may cast target instant or " +
        "sorcery card from your graveyard without paying its mana cost. If that spell would be " +
        "put into your graveyard, exile it instead."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val spellCard = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(
                    from = spellCard,
                    insteadOfGraveyard = AfterResolveDestination.EXILE
                )
            ))
        }
        description = "Whenever this creature deals combat damage to a player, you may cast target " +
            "instant or sorcery card from your graveyard without paying its mana cost. If that " +
            "spell would be put into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9f0731c-2a3d-4613-bffa-8b967e4f142e.jpg?1783927358"
        ruling("2021-04-16", "Efreet Flamepainter's triggered ability lets you cast the spell immediately, ignoring timing restrictions. It doesn't let you choose to cast it at a later time.")
        ruling("2021-04-16", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2021-04-16", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
