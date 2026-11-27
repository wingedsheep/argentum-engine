package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Curse of Exhaustion
 * {2}{W}{W}
 * Enchantment — Aura Curse
 * Enchant player
 * Enchanted player can't cast more than one spell each turn.
 *
 * The per-turn cast cap [RestrictSpellsCastPerTurn] scoped to [Player.EnchantedPlayer]: it binds
 * whoever the Curse enchants, not its controller, and spells that player cast earlier in the turn
 * count toward it.
 */
val CurseOfExhaustion = card("Curse of Exhaustion") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nEnchanted player can't cast more than one spell each turn."

    auraTarget = Targets.Player

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, affected = Player.EnchantedPlayer)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b737a959-e974-4b2a-8dca-a257da6084b0.jpg?1783940856"
    }
}
