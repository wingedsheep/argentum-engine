package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shadow of the Second Sun
 * {4}{U}{U}
 * Enchantment — Aura
 * Enchant player
 * At the beginning of each of enchanted player's postcombat main phases, there is an additional
 * beginning phase after this phase. (The end step happens after the added untap, upkeep, and draw
 * steps.)
 *
 * A player-enchanting Aura whose step trigger is keyed to the *enchanted* player, so it fires on
 * that player's postcombat main phases whoever controls the Aura. The added phase is
 * [Effects.AddBeginningPhase]: untap, upkeep and draw within the same turn, then the end step.
 */
val ShadowOfTheSecondSun = card("Shadow of the Second Sun") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant player\n" +
        "At the beginning of each of enchanted player's postcombat main phases, there is an additional " +
        "beginning phase after this phase. (The end step happens after the added untap, upkeep, and draw steps.)"

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.POSTCOMBAT_MAIN)
        effect = Effects.AddBeginningPhase
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Danny Schwartz"
        flavorText = "Live fully, then return to the beginning."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0da72736-574d-4d99-98ba-3a91c374cd10.jpg?1783911288"

        ruling(
            "2024-06-07",
            "The additional beginning phase all happens during the current turn. Any effects that last " +
                "\"until your next turn\" or similar won't expire just because they'll go through an " +
                "additional beginning phase."
        )
        ruling(
            "2024-06-07",
            "After the additional beginning phase, the game proceeds to the ending phase (unless " +
                "something has added even more phases)."
        )
        ruling(
            "2024-06-07",
            "If multiple phases are added to the same point in your turn, the most recently created " +
                "phase happens first."
        )
    }
}
