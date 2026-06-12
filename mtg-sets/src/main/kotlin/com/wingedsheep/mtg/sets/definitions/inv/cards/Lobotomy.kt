package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Lobotomy
 * {2}{U}{B}
 * Sorcery
 *
 * Target player reveals their hand, then you choose a card other than a basic land card
 * from it. Search that player's graveyard, hand, and library for all cards with the same
 * name as the chosen card and exile them. Then that player shuffles.
 *
 * Invasion engine gap #10 — the "capture a card's name" half of the "name a card" family.
 * Instead of typing a name, the controller picks a card and [PipelineBuilder.storeCardName]
 * records its name; the search then matches every card of that name via
 * [GameObjectFilter.namedFromVariable] ([CardPredicate.NameEqualsChosen]).
 *
 * The explicit slot names preserve the original hand-threaded keys so the serialized
 * tree (and the snapshot golden) is byte-identical to the pre-builder version.
 */
val Lobotomy = card("Lobotomy") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand, then you choose a card other than a basic land card from it. " +
        "Search that player's graveyard, hand, and library for all cards with the same name as the chosen card " +
        "and exile them. Then that player shuffles."

    spell {
        val player = target("player", TargetPlayer())
        effect = Effects.Pipeline {
            // 1. Target player reveals their hand.
            run(RevealHandEffect(player))
            // 2. Gather their hand so the controller can choose a card.
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "hand")
            // 3. You choose a card other than a basic land card.
            val chosen = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.IsBasicLand))),
                prompt = "Choose a card other than a basic land card",
                alwaysPrompt = true,
                showAllCards = true,
                name = "chosen"
            )
            // 4. Record the chosen card's name.
            val chosenName = storeCardName(chosen, name = "chosenName")
            // 5. Find every card of that name across their graveyard, hand, and library.
            val toExile = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = Player.ContextPlayer(0),
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                name = "toExile"
            )
            // 6. Exile them.
            exile(toExile, owner = Player.ContextPlayer(0))
            // 7. That player shuffles.
            run(ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff307dbb-4ab6-457b-be56-47106864bf61.jpg?1562946537"
    }
}
