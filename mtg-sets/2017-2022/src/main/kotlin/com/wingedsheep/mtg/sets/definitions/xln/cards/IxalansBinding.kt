package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ixalan's Binding — Ixalan #17
 * {3}{W} · Enchantment · Uncommon
 *
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 * Your opponents can't cast spells with the same name as the exiled card.
 *
 * "Until this enchantment leaves the battlefield" is [Effects.MoveUntilSourceLeaves]: the return is
 * a one-shot effect riding the departure itself, not a leaves trigger (ruling: nothing happens in
 * between), and nothing is exiled if the Binding is already gone when the trigger resolves. The
 * move links the card to this permanent's exile pile, which the name lock — Ashiok's Erasure's
 * [PlayersCantCastSpells] over `sharingNameWithLinkedExile()` — reads; an exiled token ceases to
 * exist and leaves the pile empty, so nothing is locked (ruling).
 */
val IxalansBinding = card("Ixalan's Binding") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target nonland permanent an opponent controls " +
        "until this enchantment leaves the battlefield.\n" +
        "Your opponents can't cast spells with the same name as the exiled card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
        description = "When this enchantment enters, exile target nonland permanent an opponent " +
            "controls until this enchantment leaves the battlefield."
    }

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.sharingNameWithLinkedExile()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f3c8c31-ceb8-4a2c-beb6-0ff5f7b6ae07.jpg?1783935799"

        ruling(
            "2017-09-29",
            "If a token is exiled this way, it will cease to exist and won't return to the battlefield."
        )
        ruling(
            "2017-09-29",
            "The exiled card returns to the battlefield immediately after Ixalan's Binding leaves the " +
                "battlefield. Nothing happens between the two events, including state-based actions."
        )
        ruling(
            "2017-09-29",
            "If Ixalan's Binding leaves the battlefield before its triggered ability resolves, the target " +
                "permanent won't be exiled."
        )
        ruling(
            "2017-09-29",
            "If the exiled card is a land card that's become a nonland permanent, Ixalan's Binding won't " +
                "stop players from playing lands with that name."
        )
        ruling(
            "2017-09-29",
            "If there is no exiled card (perhaps because the exiled permanent was a token or was a commander " +
                "that was moved to the command zone in a Commander game), Ixalan's Binding won't stop players " +
                "from casting spells."
        )
    }
}
