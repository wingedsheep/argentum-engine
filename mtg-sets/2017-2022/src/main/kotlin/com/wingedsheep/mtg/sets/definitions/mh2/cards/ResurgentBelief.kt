package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Resurgent Belief
 * Sorcery (no mana cost — white color indicator)
 *
 * Suspend 2—{1}{W}
 * Return all enchantment cards from your graveyard to the battlefield. (Auras with nothing to
 * enchant remain in your graveyard.)
 *
 * Ancestral Vision's no-mana-cost suspend shape (unpayable cost, CR 118.6, so it can only be
 * suspended or cast free; white is a printed color indicator). The return is Open the Vaults'
 * gather → move over your own graveyard: every enchantment card moves at once, and the move
 * leaves each Aura's host to its controller's choice as it enters (CR 303.4f), keeping an Aura
 * with nothing it could enchant in the graveyard (CR 303.4g).
 */
val ResurgentBelief = card("Resurgent Belief") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Sorcery"
    oracleText = "Suspend 2—{1}{W} (Rather than cast this card from your hand, pay {1}{W} and exile it " +
        "with two time counters on it. At the beginning of your upkeep, remove a time counter. When the " +
        "last is removed, you may cast it without paying its mana cost.)\n" +
        "Return all enchantment cards from your graveyard to the battlefield. (Auras with nothing to " +
        "enchant remain in your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            val enchantments = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Enchantment)
            )
            move(enchantments, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    keywordAbility(KeywordAbility.suspend("{1}{W}", 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01880c6f-be00-48b5-913f-ec6c80ced184.jpg?1783926887"
        ruling("2021-06-18", "All of the enchantment cards in your graveyard that can return to the battlefield will. You can't choose to leave some in the graveyard. If you're returning an Aura that can only be attached to a creature, but your opponent is the only player who controls a creature, you must choose to attach it to that creature.")
        ruling("2021-06-18", "If an effect puts an Aura onto the battlefield under your control, you choose what it enters attached to. It must be something that it could legally enchant, but not necessarily something it could target if it were on the stack. For example, you could attach an Aura with enchant creature to an opponent's creature with hexproof, but not to a creature with protection from enchantments.")
    }
}
