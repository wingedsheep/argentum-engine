package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Curse of Oblivion
 * {3}{B}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of enchanted player's upkeep, that player exiles two cards from their graveyard.
 *
 * Curse of Thirst's enchanted-player upkeep trigger over Grave Birthing's Gather → Select → Move
 * exile. The step trigger's triggering player is the active player — the enchanted player — so
 * [Chooser.TriggeringPlayer] makes *them* pick the two cards, not the Curse's controller. With
 * fewer than two cards they exile what they have; an empty graveyard does nothing.
 */
val CurseOfOblivion = card("Curse of Oblivion") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nAt the beginning of enchanted player's upkeep, that player exiles two " +
        "cards from their graveyard."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.EnchantedPlayer))
            val chosen = chooseExactly(
                2,
                from = graveyard,
                chooser = Chooser.TriggeringPlayer,
                prompt = "Exile two cards from your graveyard"
            )
            exile(chosen, owner = Player.EnchantedPlayer)
        }
        description = "At the beginning of enchanted player's upkeep, that player exiles two cards from " +
            "their graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Jana Schirmer & Johannes Voss"
        flavorText = "The first step to peace is to learn how to forget."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c15cbd2f-8bbf-423a-81fe-521fd99bc8bf.jpg"
    }
}
