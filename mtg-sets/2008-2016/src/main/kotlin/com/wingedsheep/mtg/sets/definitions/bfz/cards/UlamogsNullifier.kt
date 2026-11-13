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
 * Ulamog's Nullifier
 * {2}{U}{B}
 * Creature — Eldrazi Processor
 * 2/3
 * Devoid (This card has no color.)
 * Flash
 * Flying
 * When this creature enters, you may put two cards your opponents own from exile into their owners'
 * graveyards. If you do, counter target spell.
 *
 * The spell is targeted as the trigger goes on the stack (with no spell to target, the trigger is
 * removed and nothing is processed). "Two" is all-or-nothing, in Blight Herder's shape:
 * `Effects.May` over an `IfYouDo` whose action picks *exactly* two from every opponent's exile and
 * whose bar is `min = 2`. With fewer than two cards your opponents own in exile, "yes" can't clear
 * that bar, so the option isn't offered and the spell isn't countered. Each card goes to its owner's
 * graveyard.
 */
val UlamogsNullifier = card("Ulamog's Nullifier") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Eldrazi Processor"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\nFlash\nFlying\n" +
        "When this creature enters, you may put two cards your opponents own from exile into their owners' " +
        "graveyards. If you do, counter target spell."

    keywords(Keyword.DEVOID, Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.SpellOnStack)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val opponentsExile = gather(
                        CardSource.FromZone(zone = Zone.EXILE, player = Player.EachOpponent, filter = GameObjectFilter.Any)
                    )
                    // Named: the "if you do" check below reads it from outside the pipeline.
                    val processed = chooseExactly(
                        2,
                        from = opponentsExile,
                        prompt = "Choose two cards your opponents own from exile to put into their owners' graveyards",
                        selectedLabel = "Put into graveyard",
                        name = "processed",
                    )
                    toGraveyard(processed)
                },
                then = Effects.CounterSpell(),
                successCriterion = SuccessCriterion.CollectionNonEmpty("processed", min = 2),
            ),
            prompt = "Put two cards your opponents own from exile into their owners' graveyards to counter the spell?",
        )
        description = "When this creature enters, you may put two cards your opponents own from exile into " +
            "their owners' graveyards. If you do, counter target spell."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acd482ad-ca4a-470f-8a76-14ec7b58316a.jpg?1783938181"
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
    }
}
