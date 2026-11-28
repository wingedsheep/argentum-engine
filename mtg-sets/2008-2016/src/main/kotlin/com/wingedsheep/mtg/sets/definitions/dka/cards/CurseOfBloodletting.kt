package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Curse of Bloodletting
 * {3}{R}{R}
 * Enchantment — Aura Curse
 * Enchant player
 * If a source would deal damage to enchanted player, it deals double that damage to that player
 * instead.
 *
 * Dictate of the Twin Gods' [DoubleDamage] narrowed to [Recipient.EnchantedPlayer] — any source,
 * any controller, combat or not, but only damage to the player this Curse enchants (never to the
 * permanents they control). Two Curses on the same player multiply by four.
 */
val CurseOfBloodletting = card("Curse of Bloodletting") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nIf a source would deal damage to enchanted player, it deals double that " +
        "damage to that player instead."

    auraTarget = Targets.Player

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any,
                recipient = Recipient.EnchantedPlayer,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Michael C. Hayes"
        flavorText = "It is the demon's mark, an infernal claim on the flesh of the guilty."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dc4ac6f-0005-47f8-bee9-10429cc542e4.jpg?1783940820"
        ruling("2011-01-22", "Curse of Bloodletting works with any damage, not just combat damage. It also doesn't matter who controls the source of the damage that's being dealt.")
        ruling("2011-01-22", "The source of the damage doesn't change. A spell that deals damage will specify the source of the damage, often the spell itself. An ability that deals damage will also specify the source of the damage, although the ability itself will never be that source. Often the source of the ability is also the source of the damage.")
        ruling("2011-01-22", "If more than one Curse of Bloodletting enchants the same player, damage dealt to that player will double for each one (two of them will end up multiplying the damage by four, three of them by eight, and four of them by sixteen).")
        ruling("2011-01-22", "If multiple effects modify how damage will be dealt to the enchanted player, that player chooses the order to apply the effects. For example, Mending Hands says, \"Prevent the next 4 damage that would be dealt to any target this turn.\" Suppose a spell would deal 5 damage to enchanted player and that player has cast Mending Hands targeting themselves. The enchanted player can either (a) prevent 4 damage first and then let Curse of Bloodletting's effect double the remaining 1 damage, taking 2 damage, or (b) double the damage to 10 and then prevent 4 damage, taking 6 damage.")
    }
}
