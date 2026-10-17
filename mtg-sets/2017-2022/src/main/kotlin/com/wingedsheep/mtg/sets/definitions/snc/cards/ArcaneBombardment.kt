package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Arcane Bombardment
 * {4}{R}{R}
 * Enchantment
 * Whenever you cast your first instant or sorcery spell each turn, exile an instant or sorcery card
 * at random from your graveyard. Then copy each card exiled with this enchantment. You may cast any
 * number of the copies without paying their mana costs.
 *
 * - **The trigger** is `castsNth(1, InstantOrSorcery)`: the ordinal counts only instant/sorcery casts
 *   this turn, so a creature cast earlier doesn't close the window, and one cast before this entered
 *   does (the second ruling).
 * - **The exile** is a gather → random select → move, linked to this enchantment
 *   (`linkToSource = true`). The triggering spell is still on the stack, so it isn't a candidate.
 * - **"Each card exiled with this enchantment"** reads the whole linked-exile pile
 *   ([CardSource.FromLinkedExile]) — the pile grows every turn. A new object (re-entered or another
 *   copy) has an empty pile, matching the last ruling.
 * - **The casts** happen during resolution ([Effects.CastAnyNumberFromCollectionWithoutPayingCost]);
 *   uncast copies cease to exist as a state-based action.
 */
val ArcaneBombardment = card("Arcane Bombardment") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast your first instant or sorcery spell each turn, exile an instant " +
        "or sorcery card at random from your graveyard. Then copy each card exiled with this " +
        "enchantment. You may cast any number of the copies without paying their mana costs."

    triggeredAbility {
        trigger = Triggers.you.castsNth(1, GameObjectFilter.InstantOrSorcery)
        effect = Effects.Pipeline {
            val bombardmentCandidates = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.InstantOrSorcery)
            )
            val bombardmentPick = chooseRandom(1, from = bombardmentCandidates)
            move(bombardmentPick, CardDestination.ToZone(Zone.EXILE), linkToSource = true)
            val bombardmentPile = gather(CardSource.FromLinkedExile())
            val bombardmentCopies = copyCards(bombardmentPile)
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(from = bombardmentCopies))
        }
        description = "Whenever you cast your first instant or sorcery spell each turn, exile an " +
            "instant or sorcery card at random from your graveyard. Then copy each card exiled " +
            "with this enchantment. You may cast any number of the copies without paying their " +
            "mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "101"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31f1b2bd-fe06-45ec-99dc-ff357a7a31d2.jpg?1783923123"

        ruling(
            "2022-04-29",
            "The spell that caused Arcane Bombardment's ability to trigger is normally still on the " +
                "stack while that ability resolves. That spell won't be among the cards in your " +
                "graveyard when you exile an instant or sorcery from it at random."
        )
        ruling(
            "2022-04-29",
            "If you've already cast an instant or sorcery spell the turn that Arcane Bombardment " +
                "enters the battlefield, it won't trigger until you cast an instant or sorcery spell " +
                "on a later turn."
        )
        ruling(
            "2022-04-29",
            "You cast the desired copies as the triggered ability resolves. You don't get to cast " +
                "them later in the turn. You choose the order of copies as you cast them."
        )
        ruling(
            "2022-04-29",
            "If you cast a spell with {X} in its cost without paying its mana cost, you must choose " +
                "0 for X."
        )
        ruling(
            "2022-04-29",
            "If Arcane Bombardment leaves the battlefield, the exiled cards remain in exile. If it " +
                "returns to the battlefield or you play another copy of it, you won't get to cast " +
                "copies of the cards previously exiled."
        )
    }
}
