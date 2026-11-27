package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of Misfortunes
 * {4}{B}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of your upkeep, you may search your library for a Curse card that doesn't have
 * the same name as a Curse attached to enchanted player, put it onto the battlefield attached to
 * that player, then shuffle.
 *
 * The search filter excludes every name already on the enchanted player through
 * `nameNotSharedWithAttachmentOf` (Curse of Misfortunes itself included, so it never fetches a
 * second copy of itself). The found Curse enters attached to that player via the move's player
 * host (it isn't cast, so it doesn't target — hexproof doesn't stop it); one that couldn't legally
 * enchant them (protection) stays in the library (CR 303.4i).
 */
val CurseOfMisfortunes = card("Curse of Misfortunes") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nAt the beginning of your upkeep, you may search your library for a Curse card " +
        "that doesn't have the same name as a Curse attached to enchanted player, put it onto the battlefield " +
        "attached to that player, then shuffle."

    auraTarget = Targets.Player

    val enchantedPlayer = EffectTarget.PlayerRef(Player.EnchantedPlayer)
    val curse = GameObjectFilter.Enchantment.withSubtype("Curse")

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.Pipeline {
                val curses = gather(
                    CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        curse.nameNotSharedWithAttachmentOf(enchantedPlayer, curse),
                    ),
                    search = true,
                )
                val chosen = chooseUpTo(
                    1,
                    from = curses,
                    prompt = "Choose a Curse card to put onto the battlefield attached to enchanted player"
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = enchantedPlayer)
                run(Effects.ShuffleLibrary())
                // CR 701.23b — the library was searched whether or not a Curse was found.
                run(EmitLibrarySearchedEventEffect)
            },
            prompt = "Search your library for a Curse card?"
        )
        description = "At the beginning of your upkeep, you may search your library for a Curse card that doesn't " +
            "have the same name as a Curse attached to enchanted player, put it onto the battlefield attached to " +
            "that player, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c531d218-ff1c-4333-a19d-446d709b1e28.jpg?1783940833"
    }
}
