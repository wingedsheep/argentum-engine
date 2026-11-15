package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jeskai Infiltrator
 * {2}{U}
 * Creature — Human Monk
 * 2/3
 * This creature can't be blocked as long as you control no other creatures.
 * When this creature deals combat damage to a player, exile it and the top card of your library in a
 * face-down pile, shuffle that pile, then manifest those cards.
 *
 * "It" is gathered only while it is still the same object on the battlefield — the identity-gated
 * `CardSource.Entity(Self)` — so an Infiltrator that has left by then leaves just the top card to
 * manifest (ruling). Both cards go to exile face down; `shufflePile` joins them and gives each a fresh
 * identity, so the opponent can't tell which of the two manifested 2/2s is the Infiltrator.
 */
val JeskaiInfiltrator = card("Jeskai Infiltrator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 3
    oracleText = "This creature can't be blocked as long as you control no other creatures.\n" +
        "When this creature deals combat damage to a player, exile it and the top card of your library in a " +
        "face-down pile, shuffle that pile, then manifest those cards. (To manifest a card, put it onto the " +
        "battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost if it's a creature card.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouControl(GameObjectFilter.Creature, negate = true, excludeSelf = true),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        description = "When this creature deals combat damage to a player, exile it and the top card of your " +
            "library in a face-down pile, shuffle that pile, then manifest those cards."
        effect = Effects.Pipeline {
            val self = gather(CardSource.Entity(EffectTarget.Self))
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            exile(self, faceDown = FaceDownMode.HIDDEN)
            exile(top, faceDown = FaceDownMode.HIDDEN)
            val pile = shufflePile(listOf(self, top))
            move(pile, CardDestination.ToZone(Zone.BATTLEFIELD), faceDown = FaceDownMode.MANIFEST)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea15d083-551b-4c13-92ad-3fc2d5414ab3.jpg?1783938705"
        ruling(
            "2014-11-24",
            "The pile is shuffled to disguise from your opponents which manifested creature is which. After you " +
                "manifest the cards, you can look at them."
        )
        ruling(
            "2014-11-24",
            "If Jeskai Infiltrator isn't on the battlefield as its triggered ability resolves, you'll manifest " +
                "just the top card of your library."
        )
        ruling(
            "2014-11-24",
            "A card's owner is public information at all times. If the two cards you exile are owned by different " +
                "players (perhaps because you gained control of a Jeskai Infiltrator owned by your opponent), which " +
                "card is which is no longer hidden from your opponent. That player will know which face-down creature " +
                "they own."
        )
    }
}
