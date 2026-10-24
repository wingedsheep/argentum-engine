package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wandering Archaic // Explore the Vastlands — Strixhaven: School of Mages #6 (canonical printing)
 *
 * Modal double-faced card (CR 712). Cast either face from hand, never both.
 *
 * Front — Wandering Archaic ({5}, Creature — Avatar, 4/4):
 *   "Whenever an opponent casts an instant or sorcery spell, they may pay {2}. If they don't, you
 *    may copy that spell. You may choose new targets for the copy."
 *   Rhystic Study's [Effects.PayOrSuffer] with the casting opponent (`TriggeringPlayer`) as the
 *   payer; the unpaid branch is an optional [Effects.CopyTargetSpell] of the triggering spell,
 *   which the trigger's controller (you) controls and may retarget. The opponent decides first,
 *   so the copy resolves before the original (ruling).
 *
 * Back — Explore the Vastlands ({3}, Sorcery):
 *   "Each player looks at the top five cards of their library and may reveal a land card and/or an
 *    instant or sorcery card from among them. Each player puts the cards they revealed this way
 *    into their hand and the rest on the bottom of their library in a random order. Each player
 *    gains 3 life."
 *   One [Effects.ForEachPlayer] over `ActivePlayerFirst` (the active player reveals first, each
 *   later player knowing the earlier choices — ruling); inside, the controller is the iterated
 *   player, so the look, both optional picks (Relentless Pursuit's two `chooseUpToSplit`s), the
 *   reveal and the moves are all theirs. Then every player gains 3 life, revealed or not.
 */
val WanderingArchaic = card("Wandering Archaic") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Creature — Avatar"
    power = 4
    toughness = 4
    oracleText = "Whenever an opponent casts an instant or sorcery spell, they may pay {2}. If they " +
        "don't, you may copy that spell. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{2}"),
            suffer = Effects.May(Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "let Wandering Archaic's controller copy that spell",
        )
        description = "Whenever an opponent casts an instant or sorcery spell, they may pay {2}. " +
            "If they don't, you may copy that spell. You may choose new targets for the copy."
    }

    modalBack("Explore the Vastlands") {
        manaCost = "{3}"
        typeLine = "Sorcery"
        oracleText = "Each player looks at the top five cards of their library and may reveal a land " +
            "card and/or an instant or sorcery card from among them. Each player puts the cards they " +
            "revealed this way into their hand and the rest on the bottom of their library in a random " +
            "order. Each player gains 3 life."
        imageUri = "https://cards.scryfall.io/normal/back/1/8/18a2bdc8-b705-4eb5-b3a5-ff2e2ab8f312.jpg?1783927400"
        spell {
            effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(5))
                val (land, afterLand) = chooseUpToSplit(
                    1, from = looked,
                    filter = GameObjectFilter.Land,
                    prompt = "You may reveal a land card and put it into your hand",
                    selectedLabel = "Reveal and put in hand",
                    remainderLabel = "Leave",
                    showAllCards = true
                )
                val (spell, rest) = chooseUpToSplit(
                    1, from = afterLand,
                    filter = GameObjectFilter.InstantOrSorcery,
                    prompt = "You may reveal an instant or sorcery card and put it into your hand",
                    selectedLabel = "Reveal and put in hand",
                    remainderLabel = "Put on the bottom",
                    showAllCards = true
                )
                reveal(land)
                reveal(spell)
                toHand(land)
                toHand(spell)
                toLibraryBottom(rest, order = CardOrder.Random)
            }) then Effects.GainLife(3, EffectTarget.PlayerRef(Player.Each))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Wayne Reynolds"
        flavorText = "\"Fly the path of nine birds.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18a2bdc8-b705-4eb5-b3a5-ff2e2ab8f312.jpg?1783927400"
        ruling("2021-04-16", "For Wandering Archaic's triggered ability, the opponent chooses whether to pay {2} before their spell resolves. If they don't pay {2}, your copy will resolve before the original spell.")
        ruling("2021-04-16", "Explore the Vastlands has received an update to its Oracle text. Specifically, revealing either a land card or an instant or sorcery card is optional. Any player who can't reveal a card or chooses not to simply puts all cards they looked at on the bottom of their library in a random order. Each player gains 3 life whether they revealed a card or not.")
        ruling("2021-04-16", "While resolving Explore the Vastlands, first the active player reveals the cards they are going to put into their hand. Then each other player in turn order does the same, knowing choices made before they choose.")
    }
}
