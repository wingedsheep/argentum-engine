package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wasteland Strangler
 * {2}{B}
 * Creature — Eldrazi Processor
 * 3/2
 * Devoid (This card has no color.)
 * When this creature enters, you may put a card an opponent owns from exile into that player's
 * graveyard. If you do, target creature gets -3/-3 until end of turn.
 *
 * The -3/-3 target is chosen as the trigger goes on the stack; the processed card is chosen on
 * resolution from every opponent's exile (the pool is keyed by owner, so it is exactly "a card an
 * opponent owns"), and the move routes it to its owner's graveyard. The "may" is the up-to-one
 * pick — choosing nothing, or an empty pool, skips the shrink. "If you do" gates on the pick, not
 * on the card landing in the graveyard: under a graveyard-to-exile replacement (Anafenza, the
 * Foremost) the card is still "put" there and stays in exile as a new object (2015-08-25 ruling).
 */
val WastelandStrangler = card("Wasteland Strangler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Processor"
    power = 3
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, you may put a card an opponent owns from exile into that player's " +
        "graveyard. If you do, target creature gets -3/-3 until end of turn."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val opponentsExile = gather(
                    CardSource.FromZone(zone = Zone.EXILE, player = Player.EachOpponent, filter = GameObjectFilter.Any)
                )
                // Named: the "if you do" check below reads it from outside the pipeline.
                val processed = chooseUpTo(
                    1,
                    from = opponentsExile,
                    prompt = "You may put a card an opponent owns from exile into its owner's graveyard",
                    selectedLabel = "Put into graveyard",
                    name = "processed",
                )
                toGraveyard(processed)
            },
            then = Effects.ModifyStats(-3, -3, creature),
            successCriterion = SuccessCriterion.CollectionNonEmpty("processed", min = 1),
        )
        description = "When this creature enters, you may put a card an opponent owns from exile into that " +
            "player's graveyard. If you do, target creature gets -3/-3 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e215ab0-efd6-4ea2-8bd2-0a93865e1e3c.jpg?1783938203"
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
    }
}
