package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of the Bloody Tome
 * {2}{U}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of enchanted player's upkeep, that player mills two cards.
 */
val CurseOfTheBloodyTome = card("Curse of the Bloody Tome") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nAt the beginning of enchanted player's upkeep, that player mills two cards."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EnchantedPlayer))
        description = "At the beginning of enchanted player's upkeep, that player mills two cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Jaime Jones"
        flavorText = "After seeing his life's work drip away, the mage decided it was a good time to go crazy."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7865e11-263b-4d61-af54-907c1acbb54f.jpg"
    }
}
