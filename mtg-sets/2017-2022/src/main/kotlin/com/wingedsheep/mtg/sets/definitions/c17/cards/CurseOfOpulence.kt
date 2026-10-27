package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Curse of Opulence
 * {R}
 * Enchantment — Aura Curse
 * Enchant player
 * Whenever enchanted player is attacked, create a Gold token. Each opponent attacking that player
 * does the same.
 *
 * "Whenever enchanted player is attacked" is `Triggers.player(Player.EnchantedPlayer).isAttacked()`:
 * the Curse's controller's trigger, once per combat, counting only creatures attacking the cursed
 * player themself (not their planeswalkers — the card's ruling).
 *
 * "Each opponent attacking that player" is judged as the ability resolves — a player is attacking
 * another if they control a creature that's attacking that player (ruling) — so it is a
 * `ForEachPlayer(EachOpponent)` whose body, with `Player.You` rebound to the visited opponent, asks
 * whether they control a creature attacking the enchanted player. If the attackers have all left,
 * only the Curse's controller gets Gold.
 */
val CurseOfOpulence = card("Curse of Opulence") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "Whenever enchanted player is attacked, create a Gold token. Each opponent attacking that " +
        "player does the same. (A Gold token is an artifact with \"Sacrifice this token: Add one " +
        "mana of any color.\")"

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).isAttacked()
        effect = Effects.CreateGold() then Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.If(
                Conditions.YouControl(GameObjectFilter.Creature.attackingEnchantedPlayer()),
                Effects.CreateGold()
            )
        )
        description = "Whenever enchanted player is attacked, create a Gold token. Each opponent " +
            "attacking that player does the same."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e23db9d3-d11f-4b2c-8349-687bc0e9d4c2.jpg?1783935943"

        ruling("2017-08-25", "Curse of Opulence's triggered ability resolves after attackers have been declared. Any costs to attack must have already been paid before the attacking player creates the Gold.")
        ruling("2017-08-25", "Each Curse's ability triggers only once, no matter how many creatures are attacking the enchanted player.")
        ruling("2017-08-25", "A player is attacking another player if they control a creature that's attacking that player. If no creatures are attacking the enchanted player as a Curse's ability resolves (most likely because they've left the battlefield), only the Curse's controller performs its actions.")
        ruling("2017-08-25", "The triggered ability of these Curses won't trigger if only a planeswalker controlled by the enchanted player is attacked.")
        ruling("2017-08-25", "If you enchant yourself with one of these Curses, you'll get its effects whenever another player attacks you.")
    }
}
