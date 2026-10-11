package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skyclave Apparition
 * {1}{W}{W}
 * Creature — Kor Spirit
 * 2/2
 *
 * When this creature enters, exile up to one target nonland, nontoken permanent you don't
 * control with mana value 4 or less.
 * When this creature leaves the battlefield, the exiled card's owner creates an X/X blue
 * Illusion creature token, where X is the mana value of the exiled card.
 *
 * The two triggers are a linked pair (CR 607): the ETB exiles *permanently* and links the card
 * to this creature (`Effects.ExileLinkedToSource`) purely as bookkeeping, and the LTB re-gathers
 * that linked card to size the Illusion and pick its controller (the card's owner). No source
 * gate: if the Apparition leaves before its ETB resolves, the card is still exiled and no token
 * is ever made — the printed rulings' behaviour.
 */
val SkyclaveApparition = card("Skyclave Apparition") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Spirit"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, exile up to one target nonland, nontoken permanent " +
        "you don't control with mana value 4 or less.\n" +
        "When this creature leaves the battlefield, the exiled card's owner creates an X/X blue " +
        "Illusion creature token, where X is the mana value of the exiled card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.nontoken().opponentControls().manaValueAtMost(4)
            ),
            optional = true
        )
        effect = Effects.ExileLinkedToSource(exiled)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        // The exiled card's *owner* creates the token: ForEachPlayer rebinds "you" to each owner of
        // the linked pile (Spell Queller's construction), so a stolen permanent's token goes home.
        effect = Effects.ForEachPlayer(
            players = Player.OwnersOfLinkedExile,
            Effects.Pipeline {
                val exiledCard = gather(CardSource.FromLinkedExile())
                run(Effects.CreateToken(
                    count = exiledCard.count,
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Illusion"),
                    dynamicPower = DynamicAmounts.manaValueOf(exiledCard),
                    dynamicToughness = DynamicAmounts.manaValueOf(exiledCard),
                    imageUri = "https://cards.scryfall.io/normal/front/2/3/2300635e-7771-4676-a5a5-29a9d8f49f1a.jpg?1783929499"
                ))
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b83cfbaa-7890-4f6f-878b-4edb45677371.jpg?1783929406"
    }
}
