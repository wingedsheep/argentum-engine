package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mizzix's Mastery — Commander 2015 #29
 * {3}{R} · Sorcery
 *
 * Exile target card that's an instant or sorcery from your graveyard. For each card exiled this
 * way, copy it, and you may cast the copy without paying its mana cost. Exile Mizzix's Mastery.
 * Overload {5}{R}{R}{R}
 *
 * Both halves are the same exile → copy → free-cast chain (Uldaros Theorix); only the gathered set
 * differs. `moveTracked` keeps "for each card exiled this way" literal — a card that doesn't reach
 * exile isn't copied. Overloaded, "target" becomes "each" (CR 702.96): every instant and sorcery
 * card in your graveyard is exiled and copied, and the copies are offered one at a time in the
 * order you choose (the last cast resolves first, per the ruling). Copies are cast during
 * resolution, ignoring their type's timing; an uncast copy ceases to exist (CR 707.10a). Mizzix's
 * Mastery is still on the stack while it resolves, so the overloaded version never copies itself.
 */
val MizzixsMastery = card("Mizzix's Mastery") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile target card that's an instant or sorcery from your graveyard. For each card " +
        "exiled this way, copy it, and you may cast the copy without paying its mana cost. Exile " +
        "Mizzix's Mastery.\n" +
        "Overload {5}{R}{R}{R} (You may cast this spell for its overload cost. If you do, change " +
        "\"target\" in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{5}{R}{R}{R}"))

    spell {
        target(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            val copy = copyCards(exiled)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(copy),
                descriptionOverride = "You may cast the copy without paying its mana cost.",
            ))
        }

        overloadEffect = Effects.Pipeline {
            val each = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery))
            val exiled = moveTracked(each, CardDestination.ToZone(Zone.EXILE))
            val copies = copyCards(exiled)
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(copies))
        }

        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b5e28e8-1e6d-4e7f-bc85-427bb39ae14b.jpg?1783938111"
        ruling("2015-11-04", "The copies are created and cast during the resolution of Mizzix's Mastery. You can't wait to cast them later in the turn. Timing restrictions based on the copy's type are ignored. Other restrictions (such as \"Cast [this spell] only during combat\") are not.")
        ruling("2015-11-04", "If the copy has {X} in its mana cost, you must choose 0 as the value of X.")
        ruling("2015-11-04", "The cards remain exiled no matter what happens to the copies.")
        ruling("2015-11-04", "If Mizzix's Mastery exiled multiple cards, you may cast the copies in any order. The last copy you cast will be the first one to resolve.")
        ruling("2015-11-04", "If you don't cast one of the copies (perhaps because there are no legal targets available or you don't want to), the copy will cease to exist.")
        ruling("2015-11-04", "Mizzix's Mastery is still on the stack as it resolves. If you pay the overload cost, Mizzix's Mastery won't copy itself.")
    }
}
