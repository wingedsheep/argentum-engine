package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Renegade Rallier
 * {1}{G}{W}
 * Creature — Human Warrior
 * 3/2
 *
 * Revolt — When this creature enters, if a permanent left the battlefield under your control
 * this turn, return target permanent card with mana value 2 or less from your graveyard to the
 * battlefield.
 *
 * Revolt is an intervening-if, so it rides on `interveningIf` with the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker. The target is a graveyard-scoped
 * permanent card (any one in your graveyard, not only one that left this turn), and
 * `fromZone = Zone.GRAVEYARD` makes the move look it up there.
 *
 * Aether Revolt is the earliest real printing; the only later one is Kaladesh Remastered (KLR),
 * which isn't scaffolded.
 */
val RenegadeRallier = card("Renegade Rallier") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Warrior"
    oracleText = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, return target permanent card with mana value 2 or less from your graveyard to the battlefield."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        val card = target(
            TargetFilter(GameObjectFilter.Permanent.manaValueAtMost(2).ownedByYou(), zone = Zone.GRAVEYARD),
        )
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, return target permanent card with mana value 2 or less from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Kieran Yanner"
        flavorText = "\"This fight belongs to us all.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90bad312-80e3-45b0-9556-60ce06808a47.jpg?1783936735"
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
        ruling("2017-02-09", "You can target any permanent card in your graveyard with mana value 2 or less, not just one that was put there from the battlefield this turn.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. Tokens that leave the battlefield will satisfy a revolt ability; paying {E} won't.")
    }
}
