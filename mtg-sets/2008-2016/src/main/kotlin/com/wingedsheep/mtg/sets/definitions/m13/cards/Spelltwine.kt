package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spelltwine — Magic 2013 #68 (canonical printing)
 * {5}{U} · Sorcery
 *
 * Exile target instant or sorcery card from your graveyard and target instant or sorcery card from
 * an opponent's graveyard. Copy those cards. Cast the copies if able without paying their mana
 * costs. Exile Spelltwine.
 *
 * Mizzix's Mastery's exile → copy → free-cast chain over two targets, with the cast mandatory:
 * the bare `CastFromCollectionWithoutPayingCost` (no `May`) casts if able and can't be cancelled,
 * and is a no-op when a copy can't be cast (no legal targets). "Either order" (ruling) is the
 * caster's pick of which copy goes first; the other is cast after it, so it can target the first
 * copy on the stack. Only targets still legal on resolution are gathered, so one illegal target
 * still exiles and copies the other. Copies are cast from exile during resolution; an uncast copy
 * ceases to exist (CR 707.10a).
 */
val Spelltwine = card("Spelltwine") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exile target instant or sorcery card from your graveyard and target instant or sorcery " +
        "card from an opponent's graveyard. Copy those cards. Cast the copies if able without paying " +
        "their mana costs. Exile Spelltwine."

    spell {
        target(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD))
        target(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByOpponent(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            val copies = copyCards(exiled)
            val order = chooseExactlySplit(
                1, copies,
                prompt = "Choose the copy to cast first",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(order.selected))
            run(Effects.CastFromCollectionWithoutPayingCost(order.remainder))
        }

        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4d2f5ab-c6be-4661-843c-51b4977a9bea.jpg?1783940505"
        ruling("2012-07-01", "Spelltwine has two targets: the instant or sorcery card in your graveyard and the one in an opponent's graveyard. You can't cast Spelltwine unless you can choose both legal targets.")
        ruling("2012-07-01", "If one of Spelltwine's targets is illegal when Spelltwine tries to resolve, you'll still exile and copy the remaining legal target.")
        ruling("2012-07-01", "The copies are created in exile and cast from exile. The cards remain exiled.")
        ruling("2012-07-01", "You can cast the copies in either order.")
        ruling("2012-07-01", "The copy you cast first will already be on the stack when you cast the other copy. If that spell targets a spell, you may choose the first copy as a legal target.")
        ruling("2012-07-01", "If the card has X in its mana cost, you must choose 0 as its value.")
    }
}
